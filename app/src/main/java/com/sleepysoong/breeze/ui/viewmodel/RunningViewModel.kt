package com.sleepysoong.breeze.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import com.sleepysoong.breeze.data.repository.RunningRepository
import com.sleepysoong.breeze.ml.PacePredictionModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val repository: RunningRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val pacePredictionModel = PacePredictionModel(context)
    
    val allRecords: StateFlow<List<RunningRecordEntity>> = repository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val totalRecords: StateFlow<Int> = repository.getAllRecords()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val latestRecord: StateFlow<RunningRecordEntity?> = repository.getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val weeklyRecords: StateFlow<List<RunningRecordEntity>> = repository.getRecordsThisWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()
    
    // AI 모델 상태
    private val _modelStatus = MutableStateFlow(ModelStatus())
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()
    
    // 임시 저장: 러닝 중 수집한 데이터
    private var pendingPaceSegmentsJson: String = "[]"
    private var pendingRoutePointsJson: String = "[]"
    
    init {
        updateModelStatus()
    }
    
    private fun updateModelStatus() {
        _modelStatus.value = ModelStatus(
            hasTrainedModel = pacePredictionModel.hasTrainedModel(),
            trainingCount = pacePredictionModel.getTrainingCount(),
            strideLength = pacePredictionModel.getStrideLength()
        )
    }
    
    /**
     * 러닝 완료 시 데이터 임시 저장
     */
    fun setPendingRunningData(paceSegmentsJson: String, routePointsJson: String) {
        pendingPaceSegmentsJson = paceSegmentsJson
        pendingRoutePointsJson = routePointsJson
    }
    
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
                    averagePaceSeconds = averagePaceSeconds,
                    routePoints = pendingRoutePointsJson,
                    paceSegments = pendingPaceSegmentsJson
                )
                _saveResult.value = SaveResult.Success(id)
                
                // 저장 후 pending 초기화
                pendingPaceSegmentsJson = "[]"
                pendingRoutePointsJson = "[]"
                
                // 저장 후 모델 재학습
                trainModelFromAllRecords()
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(e.message ?: "저장 실패")
            }
        }
    }
    
    /**
     * 모든 기록으로 AI 모델 학습
     */
    fun trainModelFromAllRecords() {
        viewModelScope.launch {
            val records = allRecords.value
            if (records.isNotEmpty()) {
                pacePredictionModel.trainFromRecords(records)
                updateModelStatus()
            }
        }
    }
    
    /**
     * AI 모델 초기화
     */
    fun resetModel() {
        pacePredictionModel.resetModel()
        updateModelStatus()
    }
    
    fun deleteRecord(record: RunningRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            // 삭제 후 모델 재학습
            trainModelFromAllRecords()
        }
    }
    
    fun deleteAllRecords() {
        viewModelScope.launch {
            repository.deleteAllRecords()
            // 전체 삭제 시 모델도 초기화
            resetModel()
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

data class ModelStatus(
    val hasTrainedModel: Boolean = false,
    val trainingCount: Int = 0,
    val strideLength: Double = 0.8
)
