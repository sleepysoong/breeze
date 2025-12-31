package com.sleepysoong.breeze.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sleepysoong.breeze.MainActivity
import com.sleepysoong.breeze.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RunningService : Service() {
    
    private val binder = RunningBinder()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var metronomeManager: MetronomeManager? = null
    
    private val _runningState = MutableStateFlow(RunningState())
    val runningState: StateFlow<RunningState> = _runningState.asStateFlow()
    
    private val _locationPoints = MutableStateFlow<List<LatLngPoint>>(emptyList())
    val locationPoints: StateFlow<List<LatLngPoint>> = _locationPoints.asStateFlow()
    
    private var isRunning = false
    private var isPaused = false
    private var startTimeMs = 0L
    private var pausedTimeMs = 0L
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var targetPaceSeconds = 390
    private var currentBpm = 0
    
    inner class RunningBinder : Binder() {
        fun getService(): RunningService = this@RunningService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        metronomeManager = MetronomeManager(this)
        createNotificationChannel()
        setupLocationCallback()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val elapsedSeconds = if (startTimeMs > 0) {
            ((System.currentTimeMillis() - startTimeMs - pausedTimeMs) / 1000).toInt()
        } else 0
        
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val distanceKm = totalDistanceMeters / 1000.0
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("러닝 중")
            .setContentText(String.format("%.2f km  |  %d:%02d", distanceKm, minutes, seconds))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
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
            }
        }
        lastLocation = location
        
        updateRunningState()
    }
    
    private fun updateRunningState() {
        val elapsedMs = if (startTimeMs > 0) {
            System.currentTimeMillis() - startTimeMs - pausedTimeMs
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
            currentBpm = currentBpm
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
        pausedTimeMs = 0L
        totalDistanceMeters = 0.0
        lastLocation = null
        _locationPoints.value = emptyList()
        
        // BPM 계산 및 메트로놈 시작
        currentBpm = MetronomeManager.calculateBpm(targetPaceSeconds)
        metronomeManager?.start(currentBpm)
        
        startForeground(NOTIFICATION_ID, createNotification())
        
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
            pausedTimeMs = System.currentTimeMillis()
            metronomeManager?.stop()
            updateRunningState()
        }
    }
    
    private fun resumeRunning() {
        if (isRunning && isPaused) {
            val pauseDuration = System.currentTimeMillis() - pausedTimeMs
            pausedTimeMs = pauseDuration
            isPaused = false
            metronomeManager?.start(currentBpm)
            updateRunningState()
        }
    }
    
    private fun stopRunning() {
        isRunning = false
        isPaused = false
        metronomeManager?.stop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun getCurrentState(): RunningState = _runningState.value
    fun getRoutePoints(): List<LatLngPoint> = _locationPoints.value
    
    override fun onDestroy() {
        super.onDestroy()
        metronomeManager?.release()
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
        
        fun startService(context: Context, targetPaceSeconds: Int) {
            val intent = Intent(context, RunningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_PACE, targetPaceSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
    val currentBpm: Int = 0
)

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
