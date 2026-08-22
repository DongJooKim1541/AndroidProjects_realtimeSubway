package com.example.gc_last.util

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * API가 내려주는 도착 시각 기준으로 남은 시간을 계산한다.
 *
 * 이 API는 자정을 넘긴 편성을 **`24:54:00` 처럼 24시 이상으로 표기**한다(실측: 잠실 평일
 * 상행 239편 중 4편). `HH`는 0~23만 받으므로 `LocalTime.parse`가 그 값에서 실패하고,
 * 예전 구현은 실패를 null로 삼켜 해당 편성을 조용히 버렸다. 그래서 자정 무렵 "곧 도착"이
 * 비어 보였다. 여기서는 24시 이상을 다음 날 같은 시각으로 환산한다.
 */
object TimeRemaining {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    /** 하루를 넘긴 표기를 정규화한 결과. */
    private data class ServiceTime(val time: LocalTime, val nextDay: Boolean)

    /** 파싱에 실패하면 null. 형식이 어긋난 응답 때문에 화면 전체가 죽지 않도록 한다. */
    fun until(arriveTime: String, now: LocalTime = LocalTime.now()): Duration? {
        val service = parse(arriveTime) ?: return null
        val duration = Duration.between(now, service.time)

        // 24시 이후 표기는 "자정을 넘긴 시각"이라는 뜻이므로, 아직 오지 않은 쪽으로 맞춘다.
        //   지금 23:00, 24:21 → 00:21 은 이미 지난 것으로 계산되므로 하루를 더해 1시간 21분.
        //   지금 00:20, 24:21 → 00:21 이 곧 오므로 그대로 1분. (하루를 더하면 24시간 1분이 된다)
        // 반대로 일반 표기(05:40 등)가 지났으면 음수로 남겨 호출부에서 걸러지게 한다.
        return if (service.nextDay && duration.isNegative) duration.plusDays(1) else duration
    }

    /**
     * `HH:mm:ss`를 읽는다. 시가 24 이상이면 24를 빼고 자정 이후로 표시한다.
     *
     * 예) `24:54:00` → `00:54:00`. 응답에 날짜가 없으므로 어느 날의 00:54인지는
     * [until]이 현재 시각을 보고 정한다.
     */
    private fun parse(arriveTime: String): ServiceTime? {
        val text = arriveTime.trim()
        val hour = text.substringBefore(':').toIntOrNull() ?: return null
        if (hour < 24) {
            return runCatching { LocalTime.parse(text, FORMATTER) }
                .getOrNull()
                ?.let { ServiceTime(it, nextDay = false) }
        }
        val normalized = "%02d%s".format(hour - 24, text.substring(text.indexOf(':')))
        return runCatching { LocalTime.parse(normalized, FORMATTER) }
            .getOrNull()
            ?.let { ServiceTime(it, nextDay = true) }
    }
}

/** "3분 20초" / "1시간 3분 20초" 형태로 표기한다. */
fun Duration.toKoreanRemaining(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    val seconds = seconds % 60
    return if (hours > 0) "${hours}시간 ${minutes}분 ${seconds}초" else "${minutes}분 ${seconds}초"
}
