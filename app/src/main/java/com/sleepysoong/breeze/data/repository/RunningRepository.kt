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
        val startTime = now - elapsedTimeMs
        val calories = calculateCalories(distanceMeters)
        
        // 시간 컨텍스트 데이터 계산
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
        val dayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=일요일
        val hourOfDay = startCalendar.get(Calendar.HOUR_OF_DAY)
        val isWeekend = dayOfWeek == 0 || dayOfWeek == 6
        val month = startCalendar.get(Calendar.MONTH)
        val season = when (month) {
            in 2..4 -> 0   // 봄 (3~5월)
            in 5..7 -> 1   // 여름 (6~8월)
            in 8..10 -> 2  // 가을 (9~11월)
            else -> 3      // 겨울 (12~2월)
        }
        
        val record = RunningRecordEntity(
            startTime = startTime,
            endTime = now,
            totalDistance = distanceMeters,
            totalTime = elapsedTimeMs,
            targetPace = targetPaceSeconds,
            averagePace = averagePaceSeconds,
            calories = calories,
            routePoints = routePoints,
            paceSegments = paceSegments,
            dayOfWeek = dayOfWeek,
            hourOfDay = hourOfDay,
            isWeekend = isWeekend,
            season = season
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
