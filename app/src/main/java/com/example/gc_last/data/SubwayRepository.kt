package com.example.gc_last.data

import com.example.gc_last.model.DayOfWeek
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.Subways
import com.example.gc_last.network.SubwayApi
import com.example.gc_last.util.TimeRemaining
import com.example.gc_last.util.toKoreanRemaining
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 지하철 시간표 조회 단일 진입점.
 *
 * 리팩토링 이전에는 동일한 XML 파싱 루프가 세 개의 ViewModel에 6벌 복사되어 있었다.
 * 네트워크/파싱은 이 클래스에만 두고, ViewModel은 결과를 어떻게 추릴지만 결정한다.
 *
 * 호출부는 반드시 IO 디스패처에서 실행해야 한다(블로킹).
 */
object SubwayRepository {

    private const val TAG_ROW = "row"
    private const val TAG_LINE_NUM = "LINE_NUM"
    private const val TAG_STATION_NAME = "STATION_NM"
    private const val TAG_ARRIVE_TIME = "ARRIVETIME"
    private const val TAG_END_STATION = "SUBWAYENAME"

    /**
     * 선택한 역/요일/방향의 전체 시간표를 반환한다.
     *
     * @param subwayName [Subways] 상수 이름 (예: "Subway9")
     * @param dayName [DayOfWeek] 상수 이름 (예: "평일")
     * @param direction 상행 "1", 하행 그 외
     */
    fun loadTimeTable(subwayName: String, dayName: String, direction: String): List<FreshData> {
        val url = SubwayApi.timeTableUrl(
            stationCode = Subways.valueOf(subwayName).scode,
            weekTag = DayOfWeek.valueOf(dayName).weekcode,
            inOutTag = direction
        )

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(url)
        document.documentElement.normalize()

        val rows = document.getElementsByTagName(TAG_ROW)
        return (0 until rows.length)
            .mapNotNull { rows.item(it) as? Element }
            .map { it.toFreshData(subwayName, dayName, direction) }
    }

    private fun Element.textOf(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent.orEmpty()

    private fun Element.toFreshData(subway: String, day: String, direction: String) = FreshData(
        id = null,
        saveId = null,
        line_num = textOf(TAG_LINE_NUM),
        station_name = textOf(TAG_STATION_NAME),
        arrivetime = textOf(TAG_ARRIVE_TIME),
        subway_end_name = textOf(TAG_END_STATION),
        timeDistance = "",
        selectSubway = subway,
        selectDay = day,
        resultDirection = direction
    )
}

/**
 * 아직 도착하지 않은 편성만 남기고 남은 시간을 [FreshData.timeDistance]에 채운다.
 *
 * 원본에는 `hours == 0` 안에 `hours > 0` 분기가 중첩되어 "N시간" 표기가 실행될 수 없었다.
 * 여기서는 의도한 동작대로 시간 단위까지 표기한다.
 */
fun List<FreshData>.upcoming(limit: Int): List<FreshData> =
    asSequence()
        .mapNotNull { fresh ->
            val remaining = TimeRemaining.until(fresh.arrivetime) ?: return@mapNotNull null
            if (remaining.isNegative) null
            else fresh.copy(timeDistance = remaining.toKoreanRemaining())
        }
        .take(limit)
        .toList()
