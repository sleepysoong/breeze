package com.sleepysoong.breeze.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import com.sleepysoong.breeze.data.repository.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val repository: RunningRepository
) : ViewModel() {
    
    val allRecords: StateFlow<List<RunningRecordEntity>> = repository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val latestRecord: StateFlow<RunningRecordEntity?> = repository.getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val weeklyRecords: StateFlow<List<RunningRecordEntity>> = repository.getRecordsThisWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()
    
    fun saveRunningRecord(
        distanceMeters: Double,
        elapsedTimeMs: Long,
        targetPaceSeconds: Int,
        averagePaceSeconds: Int
    ) {
        viewModelScope.launch {
            try {
                _saveResult.value = SaveResult.Loading
                val id = repository.saveRecord(
                    distanceMeters = distanceMeters,
                    elapsedTimeMs = elapsedTimeMs,
                    targetPaceSeconds = targetPaceSeconds,
                    averagePaceSeconds = averagePaceSeconds
                )
                _saveResult.value = SaveResult.Success(id)
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(e.message ?: "저장 실패")
            }
        }
    }
    
    fun deleteRecord(record: RunningRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }
    
    fun deleteAllRecords() {
        viewModelScope.launch {
            repository.deleteAllRecords()
        }
    }
    
    fun resetSaveResult() {
        _saveResult.value = SaveResult.Idle
    }
}

sealed class SaveResult {
    object Idle : SaveResult()
    object Loading : SaveResult()
    data class Success(val id: Long) : SaveResult()
    data class Error(val message: String) : SaveResult()
}
