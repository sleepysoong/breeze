package com.sleepysoong.breeze.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sleepysoong.breeze.data.local.entity.RunningRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
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
 * 추가: 요일/시간대별 컨디션 패턴도 학습하여
 * 같은 요일/시간대의 예상 소요 시간을 예측합니다.
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
    
    // 요일별 컨디션 가중치 (0=일요일 ~ 6=토요일)
    // 값이 낮을수록 그 요일에 더 좋은 컨디션 (빠른 페이스)
    private var dayOfWeekCondition: FloatArray = FloatArray(7) { 1.0f }
    
    // 시간대별 컨디션 가중치 (0~23시, 4시간 단위로 그룹화)
    // 새벽(0-3), 이른아침(4-7), 아침(8-11), 낮(12-15), 저녁(16-19), 밤(20-23)
    private var hourBlockCondition: FloatArray = FloatArray(6) { 1.0f }
    
    // 주말 vs 평일 컨디션 가중치
    private var weekendCondition: Float = 1.0f
    private var weekdayCondition: Float = 1.0f
    
    // 계절별 컨디션 가중치 (0=봄, 1=여름, 2=가을, 3=겨울)
    private var seasonCondition: FloatArray = FloatArray(4) { 1.0f }
    
    // 학습 데이터 수
    private var trainingCount = 0
    
    // 보폭 (자동 보정됨)
    private var strideLength = 0.8
    
    // 학습률
    private val learningRate = 0.1f
    
    // 컨텍스트별 기록 저장 (요일/시간대별 평균 페이스)
    private var contextualRecords: MutableMap<String, MutableList<ContextRecord>> = mutableMapOf()
    
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
            
            // 요일별/시간대별/계절별 데이터 수집
            val dayPaceSum = FloatArray(7) { 0f }
            val dayCounts = IntArray(7) { 0 }
            val hourBlockPaceSum = FloatArray(6) { 0f }
            val hourBlockCounts = IntArray(6) { 0 }
            val seasonPaceSum = FloatArray(4) { 0f }
            val seasonCounts = IntArray(4) { 0 }
            var weekendPaceSum = 0f
            var weekendCount = 0
            var weekdayPaceSum = 0f
            var weekdayCount = 0
            
            // 컨텍스트 기록 초기화
            contextualRecords.clear()
            
            for (record in records) {
                val segments = parsePaceSegments(record.paceSegments)
                val targetPace = record.targetPace.toFloat()
                if (targetPace <= 0) continue
                
                val totalKm = (record.totalDistance / 1000.0).toFloat()
                if (totalKm < 0.5) continue
                
                // 페이스 비율 (실제/목표)
                val paceRatio = record.averagePace.toFloat() / targetPace
                
                // 요일별 학습
                val dayOfWeek = record.dayOfWeek.coerceIn(0, 6)
                dayPaceSum[dayOfWeek] += paceRatio
                dayCounts[dayOfWeek]++
                
                // 시간대별 학습 (4시간 블록)
                val hourBlock = (record.hourOfDay / 4).coerceIn(0, 5)
                hourBlockPaceSum[hourBlock] += paceRatio
                hourBlockCounts[hourBlock]++
                
                // 계절별 학습
                val season = record.season.coerceIn(0, 3)
                seasonPaceSum[season] += paceRatio
                seasonCounts[season]++
                
                // 주말/평일 학습
                if (record.isWeekend) {
                    weekendPaceSum += paceRatio
                    weekendCount++
                } else {
                    weekdayPaceSum += paceRatio
                    weekdayCount++
                }
                
                // 컨텍스트 기록 저장
                val contextKey = "${dayOfWeek}_${hourBlock}"
                if (!contextualRecords.containsKey(contextKey)) {
                    contextualRecords[contextKey] = mutableListOf()
                }
                contextualRecords[contextKey]?.add(
                    ContextRecord(
                        distanceMeters = record.totalDistance,
                        timeMs = record.totalTime,
                        targetPace = record.targetPace,
                        averagePace = record.averagePace,
                        dayOfWeek = dayOfWeek,
                        hourBlock = hourBlock,
                        season = season
                    )
                )
                
                // 기존 피로 패턴 학습
                if (segments.isNotEmpty()) {
                    for ((index, segment) in segments.withIndex()) {
                        val progress = (index + 0.5f) / segments.size
                        val bucketIndex = (progress * 10).toInt().coerceIn(0, 9)
                        val segmentPaceRatio = segment.pace.toFloat() / targetPace
                        newPattern[bucketIndex] += segmentPaceRatio
                        counts[bucketIndex]++
                    }
                }
                
                // 보폭 학습
                val estimatedSteps = record.totalTime / 1000.0 * 3.0
                if (estimatedSteps > 100) {
                    val estimatedStride = record.totalDistance / estimatedSteps
                    if (estimatedStride in 0.5..1.5) {
                        strideLength = strideLength * 0.9 + estimatedStride * 0.1
                    }
                }
            }
            
            // 요일별 컨디션 계산
            for (i in 0 until 7) {
                if (dayCounts[i] > 0) {
                    dayOfWeekCondition[i] = dayOfWeekCondition[i] * (1 - learningRate) + 
                        (dayPaceSum[i] / dayCounts[i]) * learningRate
                }
            }
            
            // 시간대별 컨디션 계산
            for (i in 0 until 6) {
                if (hourBlockCounts[i] > 0) {
                    hourBlockCondition[i] = hourBlockCondition[i] * (1 - learningRate) + 
                        (hourBlockPaceSum[i] / hourBlockCounts[i]) * learningRate
                }
            }
            
            // 계절별 컨디션 계산
            for (i in 0 until 4) {
                if (seasonCounts[i] > 0) {
                    seasonCondition[i] = seasonCondition[i] * (1 - learningRate) + 
                        (seasonPaceSum[i] / seasonCounts[i]) * learningRate
                }
            }
            
            // 주말/평일 컨디션 계산
            if (weekendCount > 0) {
                weekendCondition = weekendCondition * (1 - learningRate) + 
                    (weekendPaceSum / weekendCount) * learningRate
            }
            if (weekdayCount > 0) {
                weekdayCondition = weekdayCondition * (1 - learningRate) + 
                    (weekdayPaceSum / weekdayCount) * learningRate
            }
            
            // 피로 패턴 업데이트
            for (i in 0 until 10) {
                if (counts[i] > 0) {
                    val avgRatio = newPattern[i] / counts[i]
                    fatiguePattern[i] = fatiguePattern[i] * (1 - learningRate) + avgRatio * learningRate
                }
            }
            
            // 패턴 정규화
            val avgPattern = fatiguePattern.average().toFloat()
            if (avgPattern > 0) {
                for (i in 0 until 10) {
                    fatiguePattern[i] /= avgPattern
                }
            }
            
            trainingCount = records.size
            saveModel()
        }
    }
    
    /**
     * 현재 컨텍스트(요일/시간)에서 목표 거리의 예상 소요 시간 계산
     * 
     * @param targetDistanceMeters 목표 거리 (m)
     * @param targetPaceSeconds 목표 페이스 (초/km)
     * @return 예상 소요 시간 (ms)
     */
    fun predictFinishTime(
        targetDistanceMeters: Double,
        targetPaceSeconds: Int
    ): Long {
        if (targetPaceSeconds <= 0 || targetDistanceMeters <= 0) return 0L
        
        // 현재 시간 컨텍스트 가져오기
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=일요일
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val hourBlock = (hourOfDay / 4).coerceIn(0, 5)
        val month = calendar.get(Calendar.MONTH)
        val season = when (month) {
            in 2..4 -> 0
            in 5..7 -> 1
            in 8..10 -> 2
            else -> 3
        }
        val isWeekend = dayOfWeek == 0 || dayOfWeek == 6
        
        // 컨디션 가중치 계산
        val conditionMultiplier = calculateConditionMultiplier(dayOfWeek, hourBlock, season, isWeekend)
        
        // 조정된 페이스 계산
        val adjustedPace = targetPaceSeconds * conditionMultiplier
        
        // 예상 시간 계산 (페이스 * 거리)
        val distanceKm = targetDistanceMeters / 1000.0
        val predictedTimeSeconds = adjustedPace * distanceKm
        
        return (predictedTimeSeconds * 1000).toLong()
    }
    
    /**
     * 특정 컨텍스트에서의 컨디션 가중치 계산
     */
    private fun calculateConditionMultiplier(
        dayOfWeek: Int,
        hourBlock: Int,
        season: Int,
        isWeekend: Boolean
    ): Float {
        if (trainingCount == 0) return 1.0f
        
        var multiplier = 1.0f
        
        // 요일 가중치
        multiplier *= dayOfWeekCondition[dayOfWeek.coerceIn(0, 6)]
        
        // 시간대 가중치
        multiplier *= hourBlockCondition[hourBlock.coerceIn(0, 5)]
        
        // 계절 가중치
        multiplier *= seasonCondition[season.coerceIn(0, 3)]
        
        // 주말/평일 가중치
        multiplier *= if (isWeekend) weekendCondition else weekdayCondition
        
        // 전체 평균으로 정규화 (너무 큰 편차 방지)
        val avgMultiplier = (
            dayOfWeekCondition.average() * 
            hourBlockCondition.average() * 
            seasonCondition.average() * 
            (weekendCondition + weekdayCondition) / 2
        ).toFloat()
        
        if (avgMultiplier > 0) {
            multiplier /= avgMultiplier
        }
        
        // 극단값 제한 (±20%)
        return multiplier.coerceIn(0.8f, 1.2f)
    }
    
    /**
     * 현재 시간 컨텍스트의 컨디션 분석 결과 반환
     */
    fun getCurrentConditionAnalysis(): ConditionAnalysis {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val hourBlock = (hourOfDay / 4).coerceIn(0, 5)
        val month = calendar.get(Calendar.MONTH)
        val season = when (month) {
            in 2..4 -> 0
            in 5..7 -> 1
            in 8..10 -> 2
            else -> 3
        }
        val isWeekend = dayOfWeek == 0 || dayOfWeek == 6
        
        val conditionMultiplier = calculateConditionMultiplier(dayOfWeek, hourBlock, season, isWeekend)
        
        val dayNames = listOf("일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")
        val timeBlockNames = listOf("새벽", "이른 아침", "아침", "낮", "저녁", "밤")
        val seasonNames = listOf("봄", "여름", "가을", "겨울")
        
        val conditionLevel = when {
            conditionMultiplier < 0.95f -> ConditionLevel.EXCELLENT
            conditionMultiplier < 1.0f -> ConditionLevel.GOOD
            conditionMultiplier < 1.05f -> ConditionLevel.NORMAL
            conditionMultiplier < 1.1f -> ConditionLevel.FAIR
            else -> ConditionLevel.POOR
        }
        
        return ConditionAnalysis(
            dayOfWeek = dayNames[dayOfWeek.coerceIn(0, 6)],
            timeBlock = timeBlockNames[hourBlock.coerceIn(0, 5)],
            season = seasonNames[season.coerceIn(0, 3)],
            isWeekend = isWeekend,
            conditionMultiplier = conditionMultiplier,
            conditionLevel = conditionLevel,
            hasEnoughData = trainingCount >= 5
        )
    }
    
    /**
     * 현재 진행 상황에 따른 최적 BPM 계산
     */
    fun calculateSmartBpm(
        targetPaceSeconds: Int,
        currentDistanceMeters: Double,
        expectedTotalDistanceMeters: Double = 5000.0,
        currentTimeMs: Long = 0L
    ): Int {
        if (targetPaceSeconds <= 0) return 0
        
        val totalDistance = if (expectedTotalDistanceMeters > 0) expectedTotalDistanceMeters else 5000.0
        val progress = (currentDistanceMeters / totalDistance).coerceIn(0.0, 1.0)
        val paceMultiplier = getPaceMultiplier(progress.toFloat())
        val adjustedPace = targetPaceSeconds * paceMultiplier
        val stepsPerKm = 1000.0 / strideLength
        val paceMinutes = adjustedPace / 60.0
        
        return (stepsPerKm / paceMinutes).toInt().coerceIn(100, 220)
    }
    
    /**
     * 단순 BPM 계산
     */
    fun calculateSimpleBpm(targetPaceSeconds: Int): Int {
        if (targetPaceSeconds <= 0) return 0
        val stepsPerKm = 1000.0 / strideLength
        val paceMinutes = targetPaceSeconds / 60.0
        return (stepsPerKm / paceMinutes).toInt().coerceIn(100, 220)
    }
    
    private fun getPaceMultiplier(progress: Float): Float {
        val exactIndex = progress * 9
        val lowerIndex = exactIndex.toInt().coerceIn(0, 9)
        val upperIndex = (lowerIndex + 1).coerceIn(0, 9)
        val fraction = exactIndex - lowerIndex
        return fatiguePattern[lowerIndex] * (1 - fraction) + fatiguePattern[upperIndex] * fraction
    }
    
    fun hasTrainedModel(): Boolean = trainingCount > 0
    fun getStrideLength(): Double = strideLength
    fun getFatiguePattern(): FloatArray = fatiguePattern.copyOf()
    fun getTrainingCount(): Int = trainingCount
    
    /**
     * 요일별 컨디션 데이터 반환 (UI 표시용)
     */
    fun getDayOfWeekConditions(): Map<String, Float> {
        val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")
        return dayNames.mapIndexed { index, name -> name to dayOfWeekCondition[index] }.toMap()
    }
    
    /**
     * 시간대별 컨디션 데이터 반환 (UI 표시용)
     */
    fun getHourBlockConditions(): Map<String, Float> {
        val blockNames = listOf("새벽\n0-4시", "이른아침\n4-8시", "아침\n8-12시", "낮\n12-16시", "저녁\n16-20시", "밤\n20-24시")
        return blockNames.mapIndexed { index, name -> name to hourBlockCondition[index] }.toMap()
    }
    
    private fun saveModel() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FATIGUE_PATTERN, gson.toJson(fatiguePattern.toList()))
            .putString(KEY_DAY_CONDITION, gson.toJson(dayOfWeekCondition.toList()))
            .putString(KEY_HOUR_CONDITION, gson.toJson(hourBlockCondition.toList()))
            .putString(KEY_SEASON_CONDITION, gson.toJson(seasonCondition.toList()))
            .putFloat(KEY_WEEKEND_CONDITION, weekendCondition)
            .putFloat(KEY_WEEKDAY_CONDITION, weekdayCondition)
            .putInt(KEY_TRAINING_COUNT, trainingCount)
            .putFloat(KEY_STRIDE_LENGTH, strideLength.toFloat())
            .apply()
    }
    
    private fun loadModel() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        trainingCount = prefs.getInt(KEY_TRAINING_COUNT, 0)
        strideLength = prefs.getFloat(KEY_STRIDE_LENGTH, 0.8f).toDouble()
        weekendCondition = prefs.getFloat(KEY_WEEKEND_CONDITION, 1.0f)
        weekdayCondition = prefs.getFloat(KEY_WEEKDAY_CONDITION, 1.0f)
        
        loadFloatArray(prefs.getString(KEY_FATIGUE_PATTERN, null), 10)?.let { fatiguePattern = it }
        loadFloatArray(prefs.getString(KEY_DAY_CONDITION, null), 7)?.let { dayOfWeekCondition = it }
        loadFloatArray(prefs.getString(KEY_HOUR_CONDITION, null), 6)?.let { hourBlockCondition = it }
        loadFloatArray(prefs.getString(KEY_SEASON_CONDITION, null), 4)?.let { seasonCondition = it }
    }
    
    private fun loadFloatArray(json: String?, expectedSize: Int): FloatArray? {
        if (json == null) return null
        return try {
            val type = object : TypeToken<List<Float>>() {}.type
            val list: List<Float> = gson.fromJson(json, type)
            if (list.size == expectedSize) list.toFloatArray() else null
        } catch (e: Exception) {
            null
        }
    }
    
    fun resetModel() {
        fatiguePattern = floatArrayOf(0.95f, 0.96f, 0.97f, 0.98f, 1.00f, 1.01f, 1.02f, 1.03f, 1.04f, 1.05f)
        dayOfWeekCondition = FloatArray(7) { 1.0f }
        hourBlockCondition = FloatArray(6) { 1.0f }
        seasonCondition = FloatArray(4) { 1.0f }
        weekendCondition = 1.0f
        weekdayCondition = 1.0f
        trainingCount = 0
        strideLength = 0.8
        contextualRecords.clear()
        saveModel()
    }
    
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
        private const val KEY_DAY_CONDITION = "day_condition"
        private const val KEY_HOUR_CONDITION = "hour_condition"
        private const val KEY_SEASON_CONDITION = "season_condition"
        private const val KEY_WEEKEND_CONDITION = "weekend_condition"
        private const val KEY_WEEKDAY_CONDITION = "weekday_condition"
        private const val KEY_TRAINING_COUNT = "training_count"
        private const val KEY_STRIDE_LENGTH = "stride_length"
    }
}

/**
 * 구간 페이스 데이터
 */
data class PaceSegment(
    val segmentIndex: Int,
    val pace: Int,
    val startTime: Long,
    val endTime: Long
)

/**
 * 컨텍스트 기록 (학습용)
 */
data class ContextRecord(
    val distanceMeters: Double,
    val timeMs: Long,
    val targetPace: Int,
    val averagePace: Int,
    val dayOfWeek: Int,
    val hourBlock: Int,
    val season: Int
)

/**
 * 컨디션 분석 결과
 */
data class ConditionAnalysis(
    val dayOfWeek: String,
    val timeBlock: String,
    val season: String,
    val isWeekend: Boolean,
    val conditionMultiplier: Float,
    val conditionLevel: ConditionLevel,
    val hasEnoughData: Boolean
)

enum class ConditionLevel {
    EXCELLENT,  // 매우 좋음 (< 0.95)
    GOOD,       // 좋음 (0.95 ~ 1.0)
    NORMAL,     // 보통 (1.0 ~ 1.05)
    FAIR,       // 약간 나쁨 (1.05 ~ 1.1)
    POOR        // 나쁨 (> 1.1)
}
