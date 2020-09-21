package com.example.gc_last.model

//평일, 토요일, 일요일 시간
enum class DayOfWeek(val holder: String, val weekcode: String) {
    평일("평일", "1"),
    토요일("토요일", "2"),
    일요일("일요일", "3"),
}