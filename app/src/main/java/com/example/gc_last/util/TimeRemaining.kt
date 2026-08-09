package com.example.gc_last.util

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * API가 내려주는 도착 시각(`HH:mm:ss`) 기준으로 남은 시간을 계산한다.
 *
 * 자정을 넘겨 운행하는 편성(예: 현재 23:50, 도착 00:15)은 음수가 되어 호출부에서 제외된다.
 * 원본 구현과 동일한 동작이며, 막차 이후 시간대를 다루려면 날짜 정보가 함께 필요하다.
 */
object TimeRemaining {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    /** 파싱에 실패하면 null. 형식이 어긋난 응답 때문에 화면 전체가 죽지 않도록 한다. */
    fun until(arriveTime: String, now: LocalTime = LocalTime.now()): Duration? =
        runCatching { LocalTime.parse(arriveTime.trim(), FORMATTER) }
            .getOrNull()
            ?.let { Duration.between(now, it) }
}

/** "3분 20초" / "1시간 3분 20초" 형태로 표기한다. */
fun Duration.toKoreanRemaining(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    val seconds = seconds % 60
    return if (hours > 0) "${hours}시간 ${minutes}분 ${seconds}초" else "${minutes}분 ${seconds}초"
}
