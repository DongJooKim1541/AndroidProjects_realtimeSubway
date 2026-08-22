package com.example.gc_last.data

import com.example.gc_last.model.DayOfWeek
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.StationCatalog
import com.example.gc_last.network.SubwayApi
import com.example.gc_last.util.TimeRemaining
import com.example.gc_last.util.toKoreanRemaining
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/** 서울 열린데이터광장이 오류 코드를 돌려줬을 때 던진다. */
class SubwayApiException(message: String) : IOException(message)

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

    private const val TAG_CODE = "CODE"
    private const val TAG_MESSAGE = "MESSAGE"

    /** 연결·응답 대기 상한. 없으면 서버가 응답을 멈출 때 조회가 끝나지 않는다. */
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    /** 서울 열린데이터광장이 정상 응답에 쓰는 코드. 그 외는 오류다. */
    private const val CODE_SUCCESS = "INFO-000"

    /**
     * 선택한 역/요일/방향의 전체 시간표를 반환한다.
     *
     * @param subwayName 역 코드(예: "0222"). 이전 버전이 저장한 [Subways] 상수 이름도 받는다.
     * @param dayName [DayOfWeek] 상수 이름 (예: "평일")
     * @param direction 상행 "1", 하행 그 외
     */
    fun loadTimeTable(subwayName: String, dayName: String, direction: String): List<FreshData> {
        val station = StationCatalog.resolve(subwayName)
            ?: throw SubwayApiException("모르는 역입니다: $subwayName")

        val url = SubwayApi.timeTableUrl(
            stationCode = station.code,
            weekTag = DayOfWeek.valueOf(dayName).weekcode,
            inOutTag = direction
        )

        val document = fetchXml(url)
        document.documentElement.normalize()

        val rows = document.getElementsByTagName(TAG_ROW)
        if (rows.length == 0) {
            // 인증키 오류 등은 <RESULT><CODE>INFO-100</CODE><MESSAGE>..</MESSAGE></RESULT> 로 온다.
            // 이걸 걸러내지 않으면 결과가 조용히 0건이 되어 화면만 비어 보인다.
            document.apiErrorMessage()?.let { throw SubwayApiException(it) }
        }

        return (0 until rows.length)
            .mapNotNull { rows.item(it) as? Element }
            .map { it.toFreshData(subwayName, dayName, direction) }
    }

    /**
     * XML 을 받아 파싱한다.
     *
     * 연결을 직접 열어 타임아웃을 지정한다. 이전에는 `DocumentBuilderFactory.parse(url)` 에
     * 주소만 넘겼는데, 그 경로는 내부적으로 `URL.openStream()` 을 쓰고 기본 타임아웃이 없다.
     * 서버가 연결만 잡고 응답하지 않으면 조회가 끝나지 않고 로딩 표시가 계속 남는다.
     */
    private fun fetchXml(url: String): Document {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
        }
        try {
            connection.inputStream.use { return secureDocumentBuilder().parse(it) }
        } finally {
            connection.disconnect()
        }
    }

    /** 막아야 할 파서 기능. 기기에 따라 지원하지 않는 항목이 있어 개별로 시도한다. */
    private val UNSAFE_FEATURES = mapOf(
        "http://apache.org/xml/features/disallow-doctype-decl" to true,
        "http://xml.org/sax/features/external-general-entities" to false,
        "http://xml.org/sax/features/external-parameter-entities" to false,
        "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false
    )

    /**
     * 외부 엔티티·DTD 를 막은 파서.
     *
     * 이 API 는 HTTPS 를 제공하지 않아 평문 HTTP 로 받는다(2026-08 확인). 응답을 중간에서
     * 바꿔치기할 수 있고, 아무 설정도 하지 않은 파서는 응답에 실린 DOCTYPE 과 외부 엔티티를
     * 그대로 해석한다. 그 경로로 기기의 로컬 파일을 읽거나 내부 주소로 요청을 보내게 만들 수
     * 있다(XXE). 정상 응답에는 DOCTYPE 이 없으므로 막아도 동작에 영향이 없다.
     *
     * Android 기본 파서는 `XMLConstants.FEATURE_SECURE_PROCESSING` 을 지원하지 않아
     * `ParserConfigurationException` 을 던진다(실측). 그래서 기능 설정은 개별로 시도해
     * 지원하지 않는 것은 넘기고, 어느 기기에서나 통하는 [EntityResolver] 차단을 함께 둔다.
     */
    private fun secureDocumentBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        UNSAFE_FEATURES.forEach { (feature, value) ->
            runCatching { factory.setFeature(feature, value) }
        }
        runCatching { factory.isXIncludeAware = false }
        runCatching { factory.isExpandEntityReferences = false }

        return factory.newDocumentBuilder().apply {
            // 외부 엔티티를 참조해도 빈 내용으로 돌려준다. 기능 설정이 통하지 않는 기기에서도
            // 파일·네트워크를 읽지 않도록 하는 마지막 방어선이다.
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }
    }

    private fun Document.apiErrorMessage(): String? {
        val code = getElementsByTagName(TAG_CODE).item(0)?.textContent?.trim()
        if (code.isNullOrEmpty() || code == CODE_SUCCESS) return null
        val message = getElementsByTagName(TAG_MESSAGE).item(0)?.textContent?.trim()
        return listOfNotNull(message?.takeIf { it.isNotEmpty() }, "($code)").joinToString(" ")
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
 *
 * **남은 시간이 짧은 순으로 고른다.** 응답은 05:40 → 24:54 처럼 시각 순으로 오는데,
 * 자정 이후 편성이 24시 이상으로 표기되어 목록 끝에 붙는다. 목록 순서대로 집으면
 * 00:20 에 조회했을 때 1분 뒤 도착하는 24:21 편성 대신 5시간 뒤의 05:40 편성이 잡힌다.
 */
fun List<FreshData>.upcoming(limit: Int): List<FreshData> =
    mapNotNull { fresh ->
        val remaining = TimeRemaining.until(fresh.arrivetime) ?: return@mapNotNull null
        if (remaining.isNegative) null else fresh to remaining
    }
        .sortedBy { (_, remaining) -> remaining }
        .take(limit)
        .map { (fresh, remaining) -> fresh.copy(timeDistance = remaining.toKoreanRemaining()) }
