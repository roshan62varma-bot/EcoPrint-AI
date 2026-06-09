package com.example.data

import kotlinx.coroutines.flow.Flow

class CarbonRepository(private val carbonDao: CarbonDao) {
    fun getTrackedDay(date: String): Flow<TrackedDay?> = carbonDao.getTrackedDay(date)
    
    fun getAllTrackedDays(): Flow<List<TrackedDay>> = carbonDao.getAllTrackedDays()
    
    suspend fun insertTrackedDay(day: TrackedDay) {
        carbonDao.insertTrackedDay(day)
    }
    
    fun getLoggedActionsForDate(date: String): Flow<List<LoggedAction>> = carbonDao.getLoggedActionsForDate(date)
    
    fun getAllLoggedActions(): Flow<List<LoggedAction>> = carbonDao.getAllLoggedActions()
    
    suspend fun logAction(action: LoggedAction) {
        carbonDao.insertLoggedAction(action)
    }
    
    suspend fun deleteLoggedAction(id: Long) {
        carbonDao.deleteLoggedAction(id)
    }
}
