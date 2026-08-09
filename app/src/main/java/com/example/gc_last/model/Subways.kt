package com.example.gc_last.model

/**
 * 조회 가능한 지하철역 목록. 현재는 2호선 일부 구간만 지원한다.
 *
 * @param holder 화면에 표시할 역 이름
 * @param scode 서울 열린데이터광장 역 코드
 *
 * 주의: 상수 이름(`Subway1` 등)이 Room(`Subway.selectSubway`)과 Bundle에 그대로 저장된다.
 * 이름을 바꾸거나 순서를 바꾸면 이미 저장된 데이터를 읽을 수 없다.
 */
enum class Subways(val holder: String, val scode: String) {
    Subway1("강변", "214"),
    Subway2("잠실나루", "215"),
    Subway3("잠실", "216"),
    Subway4("잠실새내", "217"),
    Subway5("종합운동장", "218"),
    Subway6("삼성", "219"),
    Subway7("선릉", "220"),
    Subway8("역삼", "221"),
    Subway9("강남", "222"),
    Subway10("교대", "223"),
    Subway11("방배", "225"),
    Subway12("사당", "226"),
    Subway13("낙성대", "227"),
    Subway14("서울대입구", "228"),
    Subway15("봉천", "229"),
    Subway16("신림", "230"),
    Subway17("신대방", "231"),
    Subway18("구로디지털단지", "232"),
    Subway19("신도림", "234"),
    Subway20("문래", "235")

}