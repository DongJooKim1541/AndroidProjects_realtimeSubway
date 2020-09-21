package com.example.gc_last.result

import java.text.SimpleDateFormat
import java.util.*

//시간이 얼마나 남았는지 알기 위한 class
class TimeCalculate(val arrivetime:String) {

    val currentDateTime = Calendar.getInstance().time
    val hFormat= SimpleDateFormat("HH")
    val mFormat= SimpleDateFormat("mm")
    val sFormat= SimpleDateFormat("ss")
    val realHTime=hFormat.format(currentDateTime)
    val realMTime=mFormat.format(currentDateTime)
    val realSTime=sFormat.format(currentDateTime)

    val realTime=Time(realHTime.toInt(),realMTime.toInt(),realSTime.toInt())

    val arriveHour=arrivetime.slice(IntRange(0,1))
    val arriveMinute=arrivetime.slice(IntRange(3,4))
    val arriveSecond=arrivetime.slice(IntRange(6,7))

    val arrTime=Time(arriveHour.toInt(),arriveMinute.toInt(),arriveSecond.toInt())
    val diff:Time=difference(arrTime,realTime)
}

fun difference(start: Time, stop: Time): Time {
    val diff = Time(0, 0, 0)

    if (stop.seconds > start.seconds) {
        --start.minutes
        start.seconds += 60
    }

    diff.seconds = start.seconds - stop.seconds
    if (stop.minutes > start.minutes) {
        --start.hours
        start.minutes += 60
    }

    diff.minutes = start.minutes - stop.minutes
    diff.hours = start.hours - stop.hours

    return diff
}

class Time(internal var hours: Int, internal var minutes: Int, internal var seconds: Int)