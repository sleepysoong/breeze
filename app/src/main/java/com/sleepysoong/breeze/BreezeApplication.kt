package com.sleepysoong.breeze

import android.app.Application
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class BreezeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(throwable)
            } catch (e: Exception) {
                Log.e("BreezeApp", "Failed to save crash log", e)
            }
            
            // 기본 핸들러 호출 (앱 종료)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun saveCrashLog(throwable: Throwable) {
        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val crashDir = File(downloadDir, "breeze")
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val fileName = "crash_${dateFormat.format(Date())}.txt"
        val crashFile = File(crashDir, fileName)
        
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        
        pw.println("=== Breeze Crash Log ===")
        pw.println("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        pw.println("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        pw.println("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        pw.println()
        pw.println("=== Stack Trace ===")
        throwable.printStackTrace(pw)
        pw.println()
        pw.println("=== Cause ===")
        throwable.cause?.printStackTrace(pw)
        
        crashFile.writeText(sw.toString())
        
        Log.e("BreezeApp", "Crash log saved to: ${crashFile.absolutePath}")
    }
}
