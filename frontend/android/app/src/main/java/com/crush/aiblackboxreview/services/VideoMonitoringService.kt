package com.crush.aiblackboxreview.services

import android.app.*
import android.content.Context
import android.content.Intent
import java.io.File
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.crush.aiblackboxreview.MainActivity
import com.crush.aiblackboxreview.R
import com.crush.aiblackboxreview.observers.VideoContentObserver
import android.content.pm.ServiceInfo
import com.crush.aiblackboxreview.analysis.AccidentAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class VideoMonitoringService : Service() {

    private val TAG = "VideoMonitoringService"
    private lateinit var contentObserver: VideoContentObserver
    private lateinit var fileObserver: FileObserver
    private val CHANNEL_ID = "VideoMonitoringChannel"
    private val NOTIFICATION_ID = 1001

    // 사고 분석기 추가
    private lateinit var accidentAnalyzer: AccidentAnalyzer

    // 주기적 스캔을 위한 핸들러와 Runnable
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "주기적 폴더 스캔 실행")
            contentObserver.scanDirectory()
            // 15분마다 실행 (900000ms) - 효율성 향상을 위해 간격 증가
            handler.postDelayed(this, 900000)
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VideoMonitoringService = this@VideoMonitoringService
    }

    // 저장소 모니터링 작업
    private var storageMonitorJob: kotlinx.coroutines.Job? = null

    // 시스템 저장소 상태 모니터링 (간격 증가)
    private fun monitorStorageState() {
        storageMonitorJob = CoroutineScope(Dispatchers.IO).launch {

            // 이후 주기적으로 확인
            while (isActive) {
                delay(15 * 60 * 1000) // 15분으로 간격 증가
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "서비스 onCreate 호출됨")

        // 사고 분석기 초기화
        accidentAnalyzer = AccidentAnalyzer(this)

        createNotificationChannel()

        // ContentObserver 등록
        val handler = Handler(Looper.getMainLooper())
        contentObserver = VideoContentObserver(
            this,
            handler,
            accidentAnalyzer
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
        Log.d(TAG, "ContentObserver 등록 완료")

        // 주기적인 폴더 스캔 시작 (필요에 따라 유지 또는 제거)
        handler.post(scanRunnable)
        Log.d(TAG, "주기적 폴더 스캔 시작됨")

        // 포그라운드 서비스 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        Log.d(TAG, "포그라운드 서비스 시작됨")

        // 저장소 상태 모니터링 시작
        monitorStorageState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "서비스 onStartCommand 호출됨")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "서비스 onDestroy 호출됨")

        // ContentObserver 해제
        contentResolver.unregisterContentObserver(contentObserver)

        // 주기적 스캔 중지
        handler.removeCallbacks(scanRunnable)

        // 저장소 모니터링 작업 취소
        storageMonitorJob?.cancel()

        // VideoContentObserver 리소스 정리
        if (::contentObserver.isInitialized) {
            contentObserver.shutdown()
        }

        // AccidentAnalyzer 리소스 정리
        if (::accidentAnalyzer.isInitialized) {
            accidentAnalyzer.onDestroy()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Video Monitoring Service"
            val descriptionText = "Monitors new videos in gallery"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
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
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI블리 실행 중")
            .setContentText("DCIM 폴더의 MP4 영상 모니터링 중입니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
}