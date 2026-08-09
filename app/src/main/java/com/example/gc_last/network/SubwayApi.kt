package com.example.gc_last.network

import com.example.gc_last.BuildConfig
import okhttp3.HttpUrl

/**
 * 서울시 열린데이터광장 지하철 시간표 API(SearchSTNTimeTableByIDService) 주소 생성.
 *
 * 인증키는 소스에 두지 않고 `local.properties`의 `SEOUL_OPENAPI_KEY` 또는
 * 동일한 이름의 환경변수에서 읽어 BuildConfig로 주입한다. (app/build.gradle 참고)
 */
object SubwayApi {

    private const val SCHEME = "http"
    private const val HOST = "openapi.seoul.go.kr"
    private const val PORT = 8088
    private const val SERVICE = "SearchSTNTimeTableByIDService"
    private const val RESPONSE_TYPE = "xml"

    /** 조회 시작 인덱스 (API가 1-base) */
    private const val START_INDEX = "1"

    /** 조회 종료 인덱스. 하루 운행 편성을 모두 담기 위한 상한. */
    private const val END_INDEX = "250"

    /** 역 코드 앞에 붙는 호선 접두사. 현재 2호선만 지원한다. */
    private const val LINE_PREFIX = "0"

    /** 상행 방향 태그. 그 외 값은 하행으로 취급한다. */
    const val DIRECTION_UP = "1"

    fun timeTableUrl(stationCode: String, weekTag: String, inOutTag: String): String {
        // 키가 비어 있으면 addPathSegment("")가 무시되어 경로가 한 칸씩 밀린 채 요청이 나간다.
        // 그 경우 API가 오류 응답을 주고 결과가 조용히 0건이 되므로, 여기서 먼저 실패시킨다.
        check(BuildConfig.SEOUL_OPENAPI_KEY.isNotBlank()) {
            "SEOUL_OPENAPI_KEY가 설정되지 않았습니다. local.properties 또는 환경변수를 확인하세요."
        }

        return HttpUrl.Builder()
            .scheme(SCHEME)
            .host(HOST)
            .port(PORT)
            .addPathSegment(BuildConfig.SEOUL_OPENAPI_KEY)
            .addPathSegment(RESPONSE_TYPE)
            .addPathSegment(SERVICE)
            .addPathSegment(START_INDEX)
            .addPathSegment(END_INDEX)
            .addPathSegment(LINE_PREFIX + stationCode)
            .addPathSegment(weekTag)
            .addPathSegment(inOutTag)
            .build()
            .toString()
    }
}
