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
    val paceSegments: String
)
