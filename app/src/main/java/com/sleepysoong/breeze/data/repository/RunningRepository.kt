package com.sleepysoong.breeze.data.repository

import com.sleepysoong.breeze.data.local.dao.RunningRecordDao
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunningRepository @Inject constructor(
    private val runningRecordDao: RunningRecordDao
) {
    fun getAllRecords(): Flow<List<RunningRecordEntity>> {
        return runningRecordDao.getAllRecords()
    }
    
    fun getLatestRecord(): Flow<RunningRecordEntity?> {
        return runningRecordDao.getLatestRecord()
    }
    
    fun getRecordsThisWeek(): Flow<List<RunningRecordEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return runningRecordDao.getRecordsThisWeek(calendar.timeInMillis)
    }
    
    suspend fun saveRecord(
        distanceMeters: Double,
        elapsedTimeMs: Long,
        targetPaceSeconds: Int,
        averagePaceSeconds: Int,
        routePoints: String = "[]",
        paceSegments: String = "[]"
    ): Long {
        val now = System.currentTimeMillis()
        val calories = calculateCalories(distanceMeters)
        
        val record = RunningRecordEntity(
            startTime = now - elapsedTimeMs,
            endTime = now,
            totalDistance = distanceMeters,
            totalTime = elapsedTimeMs,
            targetPace = targetPaceSeconds,
            averagePace = averagePaceSeconds,
            calories = calories,
            routePoints = routePoints,
            paceSegments = paceSegments
        )
        
        return runningRecordDao.insertRecord(record)
    }
    
    suspend fun deleteRecord(record: RunningRecordEntity) {
        runningRecordDao.deleteRecord(record)
    }
    
    suspend fun deleteAllRecords() {
        runningRecordDao.deleteAllRecords()
    }
    
    suspend fun getRecordCount(): Int {
        return runningRecordDao.getRecordCount()
    }
    
    private fun calculateCalories(distanceMeters: Double): Int {
        // 간단 공식: 거리(km) * 체중(kg) * 1.036
        // 평균 체중 65kg 가정
        val distanceKm = distanceMeters / 1000.0
        return (distanceKm * 65 * 1.036).toInt()
    }
}
