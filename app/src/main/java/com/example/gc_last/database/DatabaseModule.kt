package com.example.gc_last.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gc_last.model.FreshDao
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.SaveItem

@Database(entities = [FreshData::class, SaveItem::class], version = 1)
abstract class DatabaseModule : RoomDatabase() {

    abstract fun freshDao(): FreshDao

    companion object {
        private const val ROOM_DB = "subway.db"

        @Volatile
        private var database: DatabaseModule? = null

        /**
         * `allowMainThreadQueries()`는 제거했다. 모든 조회/삭제는 suspend DAO 함수로 바뀌어
         * 코루틴(IO)에서 실행된다.
         */
        fun getDatabase(context: Context): DatabaseModule =
            database ?: synchronized(this) {
                database ?: Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseModule::class.java,
                    ROOM_DB
                ).fallbackToDestructiveMigration().build().also { database = it }
            }
    }
}
