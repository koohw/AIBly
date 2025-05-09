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

class VideoMonitoringService : Service() {

    private val TAG = "VideoMonitoringService"
    private lateinit var contentObserver: VideoContentObserver
    private lateinit var fileObserver: FileObserver
    private val CHANNEL_ID = "VideoMonitoringChannel"
    private val NOTIFICATION_ID = 1001

    // 주기적 스캔을 위한 핸들러와 Runnable
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "주기적 폴더 스캔 실행")
            contentObserver.scanDirectory()
            // 1분마다 실행 (60000ms)
            handler.postDelayed(this, 60000)
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VideoMonitoringService = this@VideoMonitoringService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "서비스 onCreate 호출됨")

        createNotificationChannel()

        // ContentObserver 등록
        val handler = Handler(Looper.getMainLooper())
        contentObserver = VideoContentObserver(this, handler)
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
        Log.d(TAG, "ContentObserver 등록 완료")

        // 대상 폴더 경로
        val targetPath = "/storage/emulated/0/DCIM/finevu_cloud"

        // 폴더가 없으면 생성 시도
        val directory = File(targetPath)
        if (!directory.exists()) {
            try {
                val created = directory.mkdirs()
                Log.d(TAG, "폴더 생성 결과: $created ($targetPath)")
            } catch (e: Exception) {
                Log.e(TAG, "폴더 생성 실패: ${e.message}")
            }
        }

        // FileObserver 등록 (Android 버전에 따라 다르게 구현)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fileObserver = object : FileObserver(File(targetPath),
                FileObserver.CREATE or FileObserver.MOVED_TO) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(event, path, targetPath)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            fileObserver = object : FileObserver(targetPath,
                FileObserver.CREATE or FileObserver.MOVED_TO) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(event, path, targetPath)
                }
            }
        }

        fileObserver.startWatching()
        Log.d(TAG, "FileObserver 감시 시작됨")

        // 주기적인 폴더 스캔 시작
        handler.post(scanRunnable)
        Log.d(TAG, "주기적 폴더 스캔 시작됨")

        // 시작 시 즉시 첫 스캔 수행
        contentObserver.scanDirectory()

        // 포그라운드 서비스 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        Log.d(TAG, "포그라운드 서비스 시작됨")
    }

    private fun handleFileEvent(event: Int, path: String?, targetPath: String) {
        if (path != null) {
            Log.d(TAG, "FileObserver 이벤트 감지됨: 이벤트=$event, 경로=$path")
            val fullPath = "$targetPath/$path"

            // MP4 파일인지 확인
            if (path.endsWith(".mp4", ignoreCase = true)) {
                // 파일 존재 여부 확인
                val file = File(fullPath)
                if (file.exists()) {
                    Log.d(TAG, "새 MP4 파일 감지됨 (FileObserver): ${file.absolutePath}")

                    // ContentObserver에서 감지하도록 미디어 스캔 요청
                    // (해당 파일이 미디어 스토어에 등록되도록)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10 이상에서는 미디어 스캔 방식이 다름
                        Log.d(TAG, "미디어 스캔 요청 (Android 10+)")
                        contentObserver.scanDirectory()
                    } else {
                        Log.d(TAG, "미디어 스캔 요청 (Android 9-)")
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = Uri.fromFile(file)
                        sendBroadcast(mediaScanIntent)
                    }
                }
            }
        }
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
        contentResolver.unregisterContentObserver(contentObserver)
        fileObserver.stopWatching()
        // 주기적 스캔 중지
        handler.removeCallbacks(scanRunnable)
        Log.d(TAG, "서비스 onDestroy 호출됨")
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
            .setContentText("블랙박스 영상 모니터링 중입니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
}