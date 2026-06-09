package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarbonDao {
    // --- Tracked Days (Emissions calculator state) ---
    @Query("SELECT * FROM tracked_days WHERE date = :date LIMIT 1")
    fun getTrackedDay(date: String): Flow<TrackedDay?>

    @Query("SELECT * FROM tracked_days ORDER BY date DESC")
    fun getAllTrackedDays(): Flow<List<TrackedDay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedDay(day: TrackedDay)

    // --- Logged Actions (Eco-friendly habits) ---
    @Query("SELECT * FROM logged_actions WHERE date = :date ORDER BY id DESC")
    fun getLoggedActionsForDate(date: String): Flow<List<LoggedAction>>

    @Query("SELECT * FROM logged_actions ORDER BY date DESC, id DESC")
    fun getAllLoggedActions(): Flow<List<LoggedAction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedAction(action: LoggedAction)

    @Query("DELETE FROM logged_actions WHERE id = :id")
    suspend fun deleteLoggedAction(id: Long)
}
