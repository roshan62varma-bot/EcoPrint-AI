package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * CarbonDao specifies SQL access methods for Room DB, acting over
 * custom logged actions (habits) and daily emission snapshots tables.
 */
@Dao
public interface CarbonDao {
    // --- Tracked Days (Emissions calculator state) ---
    
    /**
     * Obtains tracked day attributes by unique primary key date stamp constraint.
     */
    @Query("SELECT * FROM tracked_days WHERE date = :date LIMIT 1")
    public fun getTrackedDay(date: String): Flow<TrackedDay?>

    /**
     * Queries all recorded days ordered chronologically descending.
     */
    @Query("SELECT * FROM tracked_days ORDER BY date DESC")
    public fun getAllTrackedDays(): Flow<List<TrackedDay>>

    /**
     * Inserts or updates tracked emissions indices for a calendar date in database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertTrackedDay(day: TrackedDay): Unit

    // --- Logged Actions (Eco-friendly habits) ---
    
    /**
     * Obtains eco actions lists registered for a specific date in descending sequence.
     */
    @Query("SELECT * FROM logged_actions WHERE date = :date ORDER BY id DESC")
    public fun getLoggedActionsForDate(date: String): Flow<List<LoggedAction>>

    /**
     * Queries all registered user habits across DB records of all time.
     */
    @Query("SELECT * FROM logged_actions ORDER BY date DESC, id DESC")
    public fun getAllLoggedActions(): Flow<List<LoggedAction>>

    /**
     * Registers a reduction activity with its computed carbon saving points details.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertLoggedAction(action: LoggedAction): Unit

    /**
     * Deletes a reduction entry from database by its physical key identifier index.
     */
    @Query("DELETE FROM logged_actions WHERE id = :id")
    public suspend fun deleteLoggedAction(id: Long): Unit
}
