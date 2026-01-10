package com.sleepysoong.breeze.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.drawable.Icon
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import java.util.Timer
import java.util.TimerTask
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.sleepysoong.breeze.MainActivity
import com.sleepysoong.breeze.R
import com.sleepysoong.breeze.ml.PacePredictionModel
import com.sleepysoong.breeze.ml.PaceSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RunningService : Service() {
    
    private val binder = RunningBinder()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var metronomeManager: MetronomeManager? = null
    private var pacePredictionModel: PacePredictionModel? = null
    private val gson = Gson()
    
    // WakeLock으로 CPU 깨어있게 유지
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 1초 UI 업데이트 타이머
    private var updateTimer: Timer? = null
    
    private val _runningState = MutableStateFlow(RunningState())
    val runningState: StateFlow<RunningState> = _runningState.asStateFlow()
    
    private val _locationPoints = MutableStateFlow<List<LatLngPoint>>(emptyList())
    val locationPoints: StateFlow<List<LatLngPoint>> = _locationPoints.asStateFlow()
    
    // 구간 페이스 기록
    private val _paceSegments = MutableStateFlow<List<PaceSegment>>(emptyList())
    val paceSegments: StateFlow<List<PaceSegment>> = _paceSegments.asStateFlow()
    
    private var isRunning = false
    private var isPaused = false
    private var startTimeMs = 0L
    private var totalPausedTimeMs = 0L
    private var pauseStartTimeMs = 0L
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var targetPaceSeconds = 390
    private var currentBpm = 0
    
    // 구간 페이스 추적
    private var lastSegmentDistance = 0.0
    private var lastSegmentTime = 0L
    private var currentSegmentIndex = 0
    
    // BPM 업데이트 간격 (미터)
    private val BPM_UPDATE_INTERVAL_METERS = 100.0
    
    inner class RunningBinder : Binder() {
        fun getService(): RunningService = this@RunningService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        pacePredictionModel = PacePredictionModel(this)
        metronomeManager = MetronomeManager(this, pacePredictionModel)
        createNotificationChannel()
        setupLocationCallback()
        
        // WakeLock 획득 (CPU 절전 방지)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Breeze::RunningWakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                targetPaceSeconds = intent.getIntExtra(EXTRA_TARGET_PACE, 390)
                startRunning()
            }
            ACTION_PAUSE -> pauseRunning()
            ACTION_RESUME -> resumeRunning()
            ACTION_STOP -> stopRunning()
        }
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "러닝 추적",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "러닝 중 위치를 추적합니다"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_RUNNING
                putExtra(EXTRA_TARGET_PACE, targetPaceSeconds)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val elapsedSeconds = if (startTimeMs > 0) {
            ((System.currentTimeMillis() - startTimeMs - totalPausedTimeMs) / 1000).toInt()
        } else 0
        
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val distanceKm = totalDistanceMeters / 1000.0
        val currentPace = if (distanceKm > 0.01) {
            ((elapsedSeconds.toDouble()) / distanceKm).toInt()
        } else 0
        val paceMin = currentPace / 60
        val paceSec = currentPace % 60
        
        val targetDistanceKm = 5.0
        val progress = ((distanceKm / targetDistanceKm) * 400).toInt().coerceIn(0, 400)
        
        val paceColor = when {
            currentPace == 0 -> Color.GRAY
            currentPace <= targetPaceSeconds -> Color.GREEN
            currentPace <= targetPaceSeconds + 30 -> Color.YELLOW
            else -> Color.RED
        }
        
        val segments = listOf(
            Notification.ProgressStyle.Segment(100).setColor(if (progress >= 100) paceColor else Color.DKGRAY),
            Notification.ProgressStyle.Segment(100).setColor(if (progress >= 200) paceColor else Color.DKGRAY),
            Notification.ProgressStyle.Segment(100).setColor(if (progress >= 300) paceColor else Color.DKGRAY),
            Notification.ProgressStyle.Segment(100).setColor(if (progress >= 400) paceColor else Color.DKGRAY)
        )
        
        val progressStyle = Notification.ProgressStyle()
            .setProgress(progress)
            .setProgressSegments(segments)
            .setProgressTrackerIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
            .setStyledByProgress(true)
        
        val title = if (isPaused) "러닝 일시정지" else "러닝 중"
        val contentText = if (currentPace > 0) {
            String.format("%.2f km  |  %d:%02d  |  %d'%02d\"/km", distanceKm, minutes, seconds, paceMin, paceSec)
        } else {
            String.format("%.2f km  |  %d:%02d", distanceKm, minutes, seconds)
        }
        
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setStyle(progressStyle)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_WORKOUT)
            .setColor(paceColor)
            .build()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isPaused) return
                
                result.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }
    }
    
    private fun processLocation(location: Location) {
        val newPoint = LatLngPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis()
        )
        
        _locationPoints.value = _locationPoints.value + newPoint
        
        lastLocation?.let { last ->
            val distance = last.distanceTo(location)
            if (distance > 1.0 && distance < 100.0) {
                totalDistanceMeters += distance
                
                // 스마트 BPM 업데이트 (100m마다)
                if (totalDistanceMeters - lastSegmentDistance >= BPM_UPDATE_INTERVAL_METERS) {
                    updateSmartBpm()
                }
                
                // 구간 페이스 기록 (1km마다)
                checkAndRecordSegment()
            }
        }
        lastLocation = location
        
        updateRunningState()
    }
    
    /**
     * 스마트 BPM 업데이트
     */
    private fun updateSmartBpm() {
        val elapsedMs = if (startTimeMs > 0) {
            System.currentTimeMillis() - startTimeMs - totalPausedTimeMs
        } else 0L
        
        val newBpm = metronomeManager?.updateSmartBpm(totalDistanceMeters, elapsedMs)
        if (newBpm != null && newBpm > 0) {
            currentBpm = newBpm
        }
    }
    
    /**
     * 1km 구간 페이스 기록
     */
    private fun checkAndRecordSegment() {
        val currentKm = (totalDistanceMeters / 1000.0).toInt()
        val lastKm = (lastSegmentDistance / 1000.0).toInt()
        
        if (currentKm > lastKm && currentKm > currentSegmentIndex) {
            val currentTimeMs = System.currentTimeMillis() - startTimeMs - totalPausedTimeMs
            val segmentTimeMs = currentTimeMs - lastSegmentTime
            val segmentDistanceM = totalDistanceMeters - lastSegmentDistance
            
            // 구간 페이스 계산 (초/km)
            val segmentPace = if (segmentDistanceM > 0) {
                ((segmentTimeMs / 1000.0) / (segmentDistanceM / 1000.0)).toInt()
            } else 0
            
            if (segmentPace > 0) {
                val segment = PaceSegment(
                    segmentIndex = currentSegmentIndex,
                    pace = segmentPace,
                    startTime = lastSegmentTime,
                    endTime = currentTimeMs
                )
                _paceSegments.value = _paceSegments.value + segment
                currentSegmentIndex++
            }
            
            lastSegmentDistance = totalDistanceMeters
            lastSegmentTime = currentTimeMs
        }
    }
    
    private fun updateRunningState() {
        val currentPausedTime = if (isPaused && pauseStartTimeMs > 0) {
            System.currentTimeMillis() - pauseStartTimeMs
        } else 0L
        
        val elapsedMs = if (startTimeMs > 0) {
            System.currentTimeMillis() - startTimeMs - totalPausedTimeMs - currentPausedTime
        } else 0L
        
        val distanceKm = totalDistanceMeters / 1000.0
        val currentPace = if (distanceKm > 0.01) {
            ((elapsedMs / 1000.0) / distanceKm).toInt()
        } else 0
        
        _runningState.value = RunningState(
            isRunning = isRunning,
            isPaused = isPaused,
            distanceMeters = totalDistanceMeters,
            elapsedTimeMs = elapsedMs,
            currentPaceSeconds = currentPace,
            targetPaceSeconds = targetPaceSeconds,
            currentBpm = currentBpm,
            isSmartMode = metronomeManager?.isSmartModeEnabled() ?: false
        )
        
        // 알림 업데이트
        if (isRunning) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }
    
    @Suppress("MissingPermission")
    private fun startRunning() {
        isRunning = true
        isPaused = false
        startTimeMs = System.currentTimeMillis()
        totalPausedTimeMs = 0L
        pauseStartTimeMs = 0L
        totalDistanceMeters = 0.0
        lastLocation = null
        _locationPoints.value = emptyList()
        _paceSegments.value = emptyList()
        
        // 구간 추적 초기화
        lastSegmentDistance = 0.0
        lastSegmentTime = 0L
        currentSegmentIndex = 0
        
        // WakeLock 획득 (최대 6시간)
        wakeLock?.acquire(6 * 60 * 60 * 1000L)
        
        // 스마트 BPM 설정
        metronomeManager?.configureSmartMode(
            targetPace = targetPaceSeconds,
            expectedDistance = 5000.0, // 기본 5km 예상
            enableSmart = true
        )
        
        // 초기 BPM 계산 및 메트로놈 시작
        currentBpm = if (pacePredictionModel?.hasTrainedModel() == true) {
            pacePredictionModel!!.calculateSmartBpm(
                targetPaceSeconds = targetPaceSeconds,
                currentDistanceMeters = 0.0
            )
        } else {
            MetronomeManager.calculateBpm(
                targetPaceSeconds, 
                pacePredictionModel?.getStrideLength() ?: 0.8
            )
        }
        metronomeManager?.start(currentBpm)
        
        // 1초마다 UI 업데이트 타이머 시작
        updateTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (isRunning && !isPaused) {
                        updateRunningState()
                    }
                }
            }, 0L, 1000L)
        }
        
        // 포그라운드 서비스 시작 (location 타입 명시)
        startForeground(
            NOTIFICATION_ID, 
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        updateRunningState()
    }
    
    private fun pauseRunning() {
        if (isRunning && !isPaused) {
            isPaused = true
            pauseStartTimeMs = System.currentTimeMillis()
            metronomeManager?.stop()
            updateRunningState()
        }
    }
    
    private fun resumeRunning() {
        if (isRunning && isPaused) {
            totalPausedTimeMs += System.currentTimeMillis() - pauseStartTimeMs
            pauseStartTimeMs = 0L
            isPaused = false
            metronomeManager?.start(currentBpm)
            updateRunningState()
        }
    }
    
    private fun stopRunning() {
        isRunning = false
        isPaused = false
        updateTimer?.cancel()
        updateTimer = null
        metronomeManager?.stop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        // WakeLock 해제
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun getCurrentState(): RunningState = _runningState.value
    fun getRoutePoints(): List<LatLngPoint> = _locationPoints.value
    fun getRoutePointsJson(): String = gson.toJson(_locationPoints.value)
    fun getPaceSegmentsJson(): String = gson.toJson(_paceSegments.value)
    fun getPacePredictionModel(): PacePredictionModel? = pacePredictionModel
    
    override fun onDestroy() {
        super.onDestroy()
        updateTimer?.cancel()
        updateTimer = null
        metronomeManager?.release()
        
        // WakeLock 해제
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        if (isRunning) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        
        const val EXTRA_TARGET_PACE = "EXTRA_TARGET_PACE"
        
        private const val CHANNEL_ID = "running_channel"
        private const val NOTIFICATION_ID = 1
        
        private const val LOCATION_INTERVAL_MS = 1000L
        private const val LOCATION_FASTEST_INTERVAL_MS = 500L
        private const val MIN_DISTANCE_METERS = 2f
        
        // 서비스 실행 상태 (앱 재시작 시 확인용)
        @Volatile
        var isServiceRunning = false
            private set
        
        fun startService(context: Context, targetPaceSeconds: Int) {
            val intent = Intent(context, RunningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_PACE, targetPaceSeconds)
            }
            context.startForegroundService(intent)
            isServiceRunning = true
        }
        
        fun pauseService(context: Context) {
            val intent = Intent(context, RunningService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }
        
        fun resumeService(context: Context) {
            val intent = Intent(context, RunningService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, RunningService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            isServiceRunning = false
        }
    }
}

data class RunningState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val distanceMeters: Double = 0.0,
    val elapsedTimeMs: Long = 0L,
    val currentPaceSeconds: Int = 0,
    val targetPaceSeconds: Int = 390,
    val currentBpm: Int = 0,
    val isSmartMode: Boolean = false
)

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
