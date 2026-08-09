package com.example.gc_last.model

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FreshDao {

    @Insert
    suspend fun insertFresh(freshData: List<FreshData>)

    @Insert
    suspend fun insertSave(saveItem: SaveItem): Long

    @Query("SELECT * FROM SaveItem")
    fun loadSaveItems(): DataSource.Factory<Int, SaveItem>

    /**
     * 저장 당시의 조회 조건(역/요일/방향)을 가져온다.
     *
     * 이전에는 컬럼별로 쿼리가 네 개 있었고, 모두 `WHERE id = :saveId`로 **자식 행의 PK**를
     * 조건으로 삼고 있었다. 그래서 호출부가 `(saveId * 2) - 1`이라는 계산으로 SaveItem의 id를
     * Subway의 id로 변환해야 했고, 저장 결과가 정확히 2건이 아니면 깨졌다.
     * 지금은 외래키(`saveId`)로 조회한다.
     */
    @Query("SELECT * FROM Subway WHERE saveId = :saveId LIMIT 1")
    suspend fun loadSavedCondition(saveId: Long): FreshData?

    @Query("SELECT * FROM Subway WHERE saveId = :saveId")
    fun loadFreshData(saveId: Long): DataSource.Factory<Int, FreshData>

    @Query("DELETE FROM SaveItem WHERE id = :saveId")
    suspend fun deleteSaveData(saveId: Long)
}
