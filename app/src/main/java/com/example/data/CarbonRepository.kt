package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository layer acting as an abstraction over Room database data access actions.
 * Simplifies and unites operations regarding custom habits tracking and historical tracked days.
 */
public class CarbonRepository(private val carbonDao: CarbonDao) {
    
    /**
     * Retrives the recorded Day footprint calculator configurations for the navigated date.
     */
    public fun getTrackedDay(date: String): Flow<TrackedDay?> = carbonDao.getTrackedDay(date)
    
    /**
     * Retrieves all recorded calculated carbon logs across database history.
     */
    public fun getAllTrackedDays(): Flow<List<TrackedDay>> = carbonDao.getAllTrackedDays()
    
    /**
     * Writes/overwrites the calculator values of the current day to the local Room database.
     */
    public suspend fun insertTrackedDay(day: TrackedDay): Unit {
        carbonDao.insertTrackedDay(day)
    }
    
    /**
     * Retrieves the custom carbon-offset habits logged for the navigated date in descending ID sequence.
     */
    public fun getLoggedActionsForDate(date: String): Flow<List<LoggedAction>> = carbonDao.getLoggedActionsForDate(date)
    
    /**
     * Displays all logged reduction habits registered globally in descending sequence.
     */
    public fun getAllLoggedActions(): Flow<List<LoggedAction>> = carbonDao.getAllLoggedActions()
    
    /**
     * Inserts a single reduction habit action record in-memory or database ledger.
     */
    public suspend fun logAction(action: LoggedAction): Unit {
        carbonDao.insertLoggedAction(action)
    }
    
    /**
     * Deletes a registered organic green log habit from persistent DB.
     */
    public suspend fun deleteLoggedAction(id: Long): Unit {
        carbonDao.deleteLoggedAction(id)
    }
}
