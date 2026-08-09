package com.example.gc_last.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Subway",
    foreignKeys = arrayOf(
        ForeignKey(
            onDelete = ForeignKey.CASCADE,
            entity = SaveItem::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("saveId")
        )
    ),
    // 외래키 컬럼에 인덱스가 없으면 부모 테이블 변경 때마다 전체 스캔이 발생한다.
    indices = arrayOf(Index("saveId"))
)

data class FreshData(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    var saveId: Long? = null,
    var line_num: String,
    var station_name: String,
    var arrivetime: String,
    var subway_end_name: String,
    var timeDistance: String,
    var selectSubway:String?,
    var selectDay:String?,
    var resultDirection:String?
)

@Entity(tableName = "SaveItem")
data class SaveItem(

    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    var saveTitle: String,
    var saveSubwayDirection:String,
    var saveSubwayDays:String,
    var saveSubwayLineNum:String,
    var saveSubwayStationName:String
)
