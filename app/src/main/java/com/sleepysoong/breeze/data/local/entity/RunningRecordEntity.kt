package com.sleepysoong.breeze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_records")
data class RunningRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Double,
    val totalTime: Long,
    val targetPace: Int,
    val averagePace: Int,
    val calories: Int,
    val routePoints: String,
    val paceSegments: String,
    // AI 예측용 컨텍스트 데이터
    val dayOfWeek: Int = 0,        // 0=일요일, 1=월요일, ..., 6=토요일
    val hourOfDay: Int = 0,        // 0~23시
    val isWeekend: Boolean = false, // 주말 여부
    val season: Int = 0,           // 0=봄, 1=여름, 2=가을, 3=겨울
    val temperature: Float? = null  // 기온 (선택적, 향후 날씨 API 연동용)
)
