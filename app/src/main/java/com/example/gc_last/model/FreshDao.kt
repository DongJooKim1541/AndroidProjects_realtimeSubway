package com.example.gc_last.model

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FreshDao {

    @Insert
    fun insertFresh(freshData: List<FreshData>)

    @Insert
    fun insertSave(saveItem: SaveItem): Long

    @Query("SELECT * FROM SaveItem")
    fun loadSaveItems(): DataSource.Factory<Int, SaveItem>

    //지하철역명 select
    @Query("SELECT station_name FROM Subway WHERE id = :saveId")
    fun loadFreshStationNameData(saveId: Long):String?
    //지하철역 select
    @Query("SELECT selectSubway FROM Subway WHERE id = :saveId")
    fun loadFreshSubwayData(saveId: Long):String?
    //요일 select
    @Query("SELECT selectDay FROM Subway WHERE id = :saveId")
    fun loadFreshDayData(saveId: Long):String?
    //상/하행 select
    @Query("SELECT resultDirection FROM Subway WHERE id = :saveId")
    fun loadFreshDirectionData(saveId: Long):String?

    @Query("SELECT * FROM Subway WHERE saveId = :saveId")
    fun loadFreshData(saveId: Long): DataSource.Factory<Int, FreshData>

    @Query("DELETE FROM SaveItem WHERE id = :saveId")
    fun deleteSaveData(saveId: Long)
}