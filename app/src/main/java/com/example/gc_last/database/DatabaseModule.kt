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
        private var database: DatabaseModule? = null

        private const val ROOM_DB = "subway.db"

        fun getDatabase(context: Context): DatabaseModule {
            if (database == null) {
                database = Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseModule::class.java, ROOM_DB
                ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
            }
            return database!!
        }
    }
}
