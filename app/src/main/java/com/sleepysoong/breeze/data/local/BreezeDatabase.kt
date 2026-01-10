package com.sleepysoong.breeze.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sleepysoong.breeze.data.local.dao.RunningRecordDao
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity

@Database(
    entities = [RunningRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BreezeDatabase : RoomDatabase() {
    abstract fun runningRecordDao(): RunningRecordDao
    
    companion object {
        /**
         * Migration from version 1 to 2
         * 요일/시간/계절 컨텍스트 필드 추가
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 새 컬럼들 추가 (기본값 포함)
                database.execSQL("ALTER TABLE running_records ADD COLUMN dayOfWeek INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE running_records ADD COLUMN hourOfDay INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE running_records ADD COLUMN isWeekend INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE running_records ADD COLUMN season INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE running_records ADD COLUMN temperature REAL DEFAULT NULL")
            }
        }
    }
}
