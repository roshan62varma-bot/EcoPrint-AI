package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * CarbonDatabase provides underlying Room local storage structure for our EcoPrint records.
 * It houses entities for day-by-day calculated emissions and logged sustainable habit actions.
 */
@Database(entities = [TrackedDay::class, LoggedAction::class], version = 2, exportSchema = false)
public abstract class CarbonDatabase : RoomDatabase() {

    /**
     * Retrieves the primary SQL Data Access Object interface for carbon tracking tables.
     *
     * @return CarbonDao database access interface.
     */
    public abstract fun carbonDao(): CarbonDao

    public companion object {
        @Volatile
        private var INSTANCE: CarbonDatabase? = null

        /**
         * Safely fetches or instantiates the singleton Room Database instance.
         *
         * @param context Application environment context.
         * @return Standard initialized CarbonDatabase singleton object.
         */
        public fun getDatabase(context: Context): CarbonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarbonDatabase::class.java,
                    "carbon_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

