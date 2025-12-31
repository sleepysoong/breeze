package com.sleepysoong.breeze.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sleepysoong.breeze.data.local.dao.RunningRecordDao
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity

@Database(
    entities = [RunningRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BreezeDatabase : RoomDatabase() {
    abstract fun runningRecordDao(): RunningRecordDao
}
