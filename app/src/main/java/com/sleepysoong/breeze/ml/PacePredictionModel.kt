package com.sleepysoong.breeze.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 피로 패턴 예측 모델
 * 
 * 사용자의 과거 러닝 기록에서 피로 패턴을 학습하여
 * 러닝 중 구간별 최적의 BPM을 예측합니다.
 * 
 * 목표: 러닝 종료 시 평균 페이스가 목표값과 일치하도록 조절
 * - 초반: 목표보다 약간 빠른 BPM (에너지 저금)
 * - 후반: 목표보다 약간 느린 BPM (피로 감안)
 */
@Singleton
class PacePredictionModel @Inject constructor(
    private val context: Context
) {
    private val gson = Gson()
    
    // 학습된 피로 패턴 가중치 (구간별 페이스 조정 비율)
    // key: 진행률 구간 (0.0~1.0), value: 페이스 조정 비율 (1.0 = 기본)
    private var fatiguePattern: FloatArray = floatArrayOf(
        0.95f,  // 0-10%: 5% 빠르게
        0.96f,  // 10-20%
        0.97f,  // 20-30%
        0.98f,  // 30-40%
        1.00f,  // 40-50%: 기준
        1.01f,  // 50-60%
        1.02f,  // 60-70%
        1.03f,  // 70-80%
        1.04f,  // 80-90%
        1.05f   // 90-100%: 5% 느리게
    )
    
    // 학습 데이터 수
    private var trainingCount = 0
    
    // 보폭 (자동 보정됨)
    private var strideLength = 0.8
    
    // 학습률
    private val learningRate = 0.1f
    
    init {
        loadModel()
    }
    
    /**
     * 과거 러닝 기록에서 피로 패턴 학습
     */
    suspend fun trainFromRecords(records: List<RunningRecordEntity>) {
        if (records.isEmpty()) return
        
        withContext(Dispatchers.Default) {
            val newPattern = FloatArray(10) { 0f }
            val counts = IntArray(10) { 0 }
            
            for (record in records) {
                val segments = parsePaceSegments(record.paceSegments)
                if (segments.isEmpty()) continue
                
                val targetPace = record.targetPace.toFloat()
                if (targetPace <= 0) continue
                
                // 전체 거리 계산
                val totalKm = (record.totalDistance / 1000.0).toFloat()
                if (totalKm < 0.5) continue // 500m 미만은 제외
                
                // 각 구간의 페이스 비율 계산
                for ((index, segment) in segments.withIndex()) {
                    val progress = (index + 0.5f) / segments.size
                    val bucketIndex = (progress * 10).toInt().coerceIn(0, 9)
                    
                    // 페이스 비율: 실제 페이스 / 목표 페이스
                    val paceRatio = segment.pace.toFloat() / targetPace
                    newPattern[bucketIndex] += paceRatio
                    counts[bucketIndex]++
                }
                
                // 보폭 학습 (총 거리 / 추정 걸음수)
                val estimatedSteps = record.totalTime / 1000.0 * 3.0 // 초당 약 3걸음 가정
                if (estimatedSteps > 100) {
                    val estimatedStride = record.totalDistance / estimatedSteps
                    if (estimatedStride in 0.5..1.5) {
                        strideLength = strideLength * 0.9 + estimatedStride * 0.1
                    }
                }
            }
            
            // 평균 계산 및 기존 패턴과 블렌딩
            for (i in 0 until 10) {
                if (counts[i] > 0) {
                    val avgRatio = newPattern[i] / counts[i]
                    // 기존 패턴과 새 패턴을 학습률에 따라 블렌딩
                    fatiguePattern[i] = fatiguePattern[i] * (1 - learningRate) + avgRatio * learningRate
                }
            }
            
            // 패턴 정규화: 평균이 1.0이 되도록
            val avgPattern = fatiguePattern.average().toFloat()
            if (avgPattern > 0) {
                for (i in 0 until 10) {
                    fatiguePattern[i] /= avgPattern
                }
            }
            
            trainingCount += records.size
            saveModel()
        }
    }
    
    /**
     * 현재 진행 상황에 따른 최적 BPM 계산
     * 
     * @param targetPaceSeconds 목표 페이스 (초/km)
     * @param currentDistanceMeters 현재 거리 (m)
     * @param expectedTotalDistanceMeters 예상 총 거리 (m), 0이면 기본값 5km
     * @param currentTimeMs 현재 경과 시간 (ms)
     * @return 최적 BPM
     */
    fun calculateSmartBpm(
        targetPaceSeconds: Int,
        currentDistanceMeters: Double,
        expectedTotalDistanceMeters: Double = 5000.0,
        currentTimeMs: Long = 0L
    ): Int {
        if (targetPaceSeconds <= 0) return 0
        
        val totalDistance = if (expectedTotalDistanceMeters > 0) expectedTotalDistanceMeters else 5000.0
        
        // 진행률 계산
        val progress = (currentDistanceMeters / totalDistance).coerceIn(0.0, 1.0)
        
        // 진행률에 해당하는 피로 패턴 가중치 가져오기
        val paceMultiplier = getPaceMultiplier(progress.toFloat())
        
        // 조정된 목표 페이스 계산
        val adjustedPace = targetPaceSeconds * paceMultiplier
        
        // BPM 계산
        val stepsPerKm = 1000.0 / strideLength
        val paceMinutes = adjustedPace / 60.0
        
        return (stepsPerKm / paceMinutes).toInt().coerceIn(100, 220)
    }
    
    /**
     * 단순 BPM 계산 (첫 러닝 또는 데이터 없을 때)
     */
    fun calculateSimpleBpm(targetPaceSeconds: Int): Int {
        if (targetPaceSeconds <= 0) return 0
        val stepsPerKm = 1000.0 / strideLength
        val paceMinutes = targetPaceSeconds / 60.0
        return (stepsPerKm / paceMinutes).toInt().coerceIn(100, 220)
    }
    
    /**
     * 진행률에 따른 페이스 조정 비율 반환
     * 선형 보간으로 부드러운 전환
     */
    private fun getPaceMultiplier(progress: Float): Float {
        val exactIndex = progress * 9
        val lowerIndex = exactIndex.toInt().coerceIn(0, 9)
        val upperIndex = (lowerIndex + 1).coerceIn(0, 9)
        val fraction = exactIndex - lowerIndex
        
        // 선형 보간
        return fatiguePattern[lowerIndex] * (1 - fraction) + fatiguePattern[upperIndex] * fraction
    }
    
    /**
     * 학습된 모델이 있는지 확인
     */
    fun hasTrainedModel(): Boolean = trainingCount > 0
    
    /**
     * 현재 보폭 반환
     */
    fun getStrideLength(): Double = strideLength
    
    /**
     * 피로 패턴 조회 (디버깅/시각화용)
     */
    fun getFatiguePattern(): FloatArray = fatiguePattern.copyOf()
    
    /**
     * 학습 데이터 수 반환
     */
    fun getTrainingCount(): Int = trainingCount
    
    /**
     * 모델 저장
     */
    private fun saveModel() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FATIGUE_PATTERN, gson.toJson(fatiguePattern.toList()))
            .putInt(KEY_TRAINING_COUNT, trainingCount)
            .putFloat(KEY_STRIDE_LENGTH, strideLength.toFloat())
            .apply()
    }
    
    /**
     * 모델 로드
     */
    private fun loadModel() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        trainingCount = prefs.getInt(KEY_TRAINING_COUNT, 0)
        strideLength = prefs.getFloat(KEY_STRIDE_LENGTH, 0.8f).toDouble()
        
        val patternJson = prefs.getString(KEY_FATIGUE_PATTERN, null)
        if (patternJson != null) {
            try {
                val type = object : TypeToken<List<Float>>() {}.type
                val list: List<Float> = gson.fromJson(patternJson, type)
                if (list.size == 10) {
                    fatiguePattern = list.toFloatArray()
                }
            } catch (e: Exception) {
                // 파싱 실패 시 기본값 사용
            }
        }
    }
    
    /**
     * 모델 초기화
     */
    fun resetModel() {
        fatiguePattern = floatArrayOf(
            0.95f, 0.96f, 0.97f, 0.98f, 1.00f,
            1.01f, 1.02f, 1.03f, 1.04f, 1.05f
        )
        trainingCount = 0
        strideLength = 0.8
        saveModel()
    }
    
    /**
     * PaceSegment JSON 파싱
     */
    private fun parsePaceSegments(json: String): List<PaceSegment> {
        return try {
            val type = object : TypeToken<List<PaceSegment>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "pace_prediction_model"
        private const val KEY_FATIGUE_PATTERN = "fatigue_pattern"
        private const val KEY_TRAINING_COUNT = "training_count"
        private const val KEY_STRIDE_LENGTH = "stride_length"
    }
}

/**
 * 구간 페이스 데이터
 */
data class PaceSegment(
    val segmentIndex: Int,  // 구간 인덱스 (0부터)
    val pace: Int,          // 페이스 (초/km)
    val startTime: Long,    // 구간 시작 시간
    val endTime: Long       // 구간 종료 시간
)
