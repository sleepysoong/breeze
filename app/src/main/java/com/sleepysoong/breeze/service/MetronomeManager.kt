package com.sleepysoong.breeze.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.sleepysoong.breeze.ml.PacePredictionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class MetronomeManager(
    private val context: Context,
    private val pacePredictionModel: PacePredictionModel? = null
) {
    
    private var metronomeJob: Job? = null
    private var currentBpm: Int = 0
    private var volume: Float = 0.7f
    
    // 스마트 BPM 관련
    private var targetPaceSeconds: Int = 390
    private var expectedTotalDistanceMeters: Double = 5000.0
    private var useSmartBpm: Boolean = false
    
    private val sampleRate = 44100
    private var tickSound: ShortArray? = null
    
    init {
        generateTickSound()
    }
    
    /**
     * 구름을 밟는 듯한 부드러운 "꿍" 소리 생성
     * 낮은 주파수의 부드러운 펄스 사운드
     */
    private fun generateTickSound() {
        val durationMs = 80 // 짧은 사운드
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        tickSound = ShortArray(numSamples)
        
        val baseFreq = 120.0 // 낮은 주파수 (꿍 소리)
        val harmonicFreq = 180.0 // 하모닉
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            
            // 엔벨로프 (빠른 어택, 부드러운 디케이)
            val attack = 0.005 // 5ms
            val decay = durationMs / 1000.0 - attack
            
            val envelope = if (t < attack) {
                t / attack
            } else {
                exp(-(t - attack) / (decay * 0.3))
            }
            
            // 기본 주파수 + 하모닉
            val baseWave = sin(2 * PI * baseFreq * t)
            val harmonic = 0.3 * sin(2 * PI * harmonicFreq * t)
            
            // 주파수 하강 효과 (피치 드롭)
            val pitchDrop = sin(2 * PI * (baseFreq * (1 - t * 2)) * t)
            
            val sample = envelope * (baseWave * 0.6 + harmonic * 0.2 + pitchDrop * 0.2)
            tickSound!![i] = (sample * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
    }
    
    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * 스마트 BPM 모드 설정
     * @param targetPace 목표 페이스 (초/km)
     * @param expectedDistance 예상 총 거리 (m)
     * @param enableSmart 스마트 모드 활성화 여부 (학습된 모델이 있을 때만)
     */
    fun configureSmartMode(
        targetPace: Int,
        expectedDistance: Double = 5000.0,
        enableSmart: Boolean = true
    ) {
        targetPaceSeconds = targetPace
        expectedTotalDistanceMeters = expectedDistance
        useSmartBpm = enableSmart && (pacePredictionModel?.hasTrainedModel() == true)
    }
    
    fun start(bpm: Int) {
        if (bpm <= 0) return
        
        currentBpm = bpm
        stop()
        
        metronomeJob = CoroutineScope(Dispatchers.Default).launch {
            val intervalMs = (60_000.0 / bpm).toLong()
            
            while (isActive) {
                playTick()
                delay(intervalMs)
            }
        }
    }
    
    fun updateBpm(bpm: Int) {
        if (bpm != currentBpm && bpm > 0) {
            start(bpm)
        }
    }
    
    /**
     * 현재 진행 상황에 따라 스마트 BPM 업데이트
     * @param currentDistanceMeters 현재 거리 (m)
     * @param currentTimeMs 현재 경과 시간 (ms)
     * @return 업데이트된 BPM
     */
    fun updateSmartBpm(currentDistanceMeters: Double, currentTimeMs: Long): Int {
        val newBpm = if (useSmartBpm && pacePredictionModel != null) {
            pacePredictionModel.calculateSmartBpm(
                targetPaceSeconds = targetPaceSeconds,
                currentDistanceMeters = currentDistanceMeters,
                expectedTotalDistanceMeters = expectedTotalDistanceMeters,
                currentTimeMs = currentTimeMs
            )
        } else {
            calculateBpm(targetPaceSeconds, pacePredictionModel?.getStrideLength() ?: 0.8)
        }
        
        // BPM이 변경되었으면 업데이트
        if (newBpm != currentBpm && newBpm > 0) {
            start(newBpm)
        }
        
        return currentBpm
    }
    
    /**
     * 현재 BPM 반환
     */
    fun getCurrentBpm(): Int = currentBpm
    
    /**
     * 스마트 모드 활성화 여부
     */
    fun isSmartModeEnabled(): Boolean = useSmartBpm
    
    fun stop() {
        metronomeJob?.cancel()
        metronomeJob = null
    }
    
    private suspend fun playTick() {
        withContext(Dispatchers.IO) {
            tickSound?.let { sound ->
                try {
                    val bufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    
                    val audioTrack = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(maxOf(bufferSize, sound.size * 2))
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    
                    audioTrack.write(sound, 0, sound.size)
                    audioTrack.setVolume(volume)
                    audioTrack.play()
                    
                    // 재생 완료 대기 후 해제
                    Thread.sleep(100)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // 오디오 재생 실패 시 무시
                }
            }
        }
    }
    
    fun release() {
        stop()
        tickSound = null
    }
    
    companion object {
        /**
         * BPM 계산 (첫 번째 러닝용)
         * BPM = (1000m / 보폭) / 목표 페이스(분)
         */
        fun calculateBpm(targetPaceSeconds: Int, strideLength: Double = 0.8): Int {
            if (targetPaceSeconds <= 0) return 0
            val stepsPerKm = 1000.0 / strideLength
            val paceMinutes = targetPaceSeconds / 60.0
            return (stepsPerKm / paceMinutes).toInt().coerceIn(100, 220)
        }
    }
}
