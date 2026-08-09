package com.example.gc_last.model

/**
 * 시간표 구분(평일/토요일/일요일).
 *
 * @param weekcode 서울 열린데이터광장 요일 코드
 *
 * 주의: [Subways]와 마찬가지로 상수 이름이 DB와 Bundle에 저장되므로 변경하면 기존 데이터가 깨진다.
 */
enum class DayOfWeek(val holder: String, val weekcode: String) {
    평일("평일", "1"),
    토요일("토요일", "2"),
    일요일("일요일", "3"),
}