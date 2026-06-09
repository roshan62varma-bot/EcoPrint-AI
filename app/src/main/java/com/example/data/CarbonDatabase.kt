package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackedDay::class, LoggedAction::class], version = 2, exportSchema = false)
abstract class CarbonDatabase : RoomDatabase() {
    abstract fun carbonDao(): CarbonDao

    companion object {
        @Volatile
        private var INSTANCE: CarbonDatabase? = null

        fun getDatabase(context: Context): CarbonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarbonDatabase::class.java,
                    "carbon_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
