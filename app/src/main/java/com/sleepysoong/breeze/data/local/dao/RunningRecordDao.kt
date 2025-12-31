package com.sleepysoong.breeze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningRecordDao {
    
    @Query("SELECT * FROM running_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<RunningRecordEntity>>
    
    @Query("SELECT * FROM running_records ORDER BY startTime DESC LIMIT 1")
    fun getLatestRecord(): Flow<RunningRecordEntity?>
    
    @Query("SELECT * FROM running_records WHERE id = :id")
    suspend fun getRecordById(id: Long): RunningRecordEntity?
    
    @Query("SELECT * FROM running_records WHERE startTime >= :startOfWeek ORDER BY startTime DESC")
    fun getRecordsThisWeek(startOfWeek: Long): Flow<List<RunningRecordEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RunningRecordEntity): Long
    
    @Delete
    suspend fun deleteRecord(record: RunningRecordEntity)
    
    @Query("DELETE FROM running_records")
    suspend fun deleteAllRecords()
    
    @Query("SELECT COUNT(*) FROM running_records")
    suspend fun getRecordCount(): Int
}
