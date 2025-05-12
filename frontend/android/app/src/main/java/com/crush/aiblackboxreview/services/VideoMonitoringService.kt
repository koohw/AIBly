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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
            // 2분마다 실행 (120000ms)
            handler.postDelayed(this, 120000)
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VideoMonitoringService = this@VideoMonitoringService
    }

    // 클래스 레벨 변수 추가
    private var storageMonitorJob: kotlinx.coroutines.Job? = null

    // 시스템 저장소 상태 모니터링 추가
    private fun monitorStorageState() {
        storageMonitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val targetPath = "/storage/emulated/0/DCIM"
                    val directory = File(targetPath)

                    if (!directory.exists() || !directory.canRead()) {
                        Log.w(TAG, "저장소 접근 불가: $targetPath (존재: ${directory.exists()}, 읽기 가능: ${directory.canRead()})")

                        // 디렉토리 생성 시도
                        if (!directory.exists()) {
                            val created = directory.mkdirs()
                            Log.d(TAG, "폴더 생성 시도: $created")
                        }

                        // 권한 확인 및 요청 로직 추가 가능
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "저장소 모니터링 중 오류", e)
                }

                delay(30000) // 30초마다 확인
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

//        // 대상 폴더 경로
//        val targetPath = "/storage/emulated/0/DCIM"
//
//        // 폴더가 없으면 생성 시도
//        val directory = File(targetPath)
//        if (!directory.exists()) {
//            try {
//                val created = directory.mkdirs()
//                Log.d(TAG, "폴더 생성 결과: $created ($targetPath)")
//            } catch (e: Exception) {
//                Log.e(TAG, "폴더 생성 실패: ${e.message}")
//            }
//        }
//
//        // FileObserver 등록 (Android 버전에 따라 다르게 구현)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            fileObserver = object : FileObserver(File(targetPath),
//                FileObserver.CREATE or FileObserver.MOVED_TO) {
//                override fun onEvent(event: Int, path: String?) {
//                    handleFileEvent(event, path, targetPath)
//                }
//            }
//        } else {
//            @Suppress("DEPRECATION")
//            fileObserver = object : FileObserver(targetPath,
//                FileObserver.CREATE or FileObserver.MOVED_TO) {
//                override fun onEvent(event: Int, path: String?) {
//                    handleFileEvent(event, path, targetPath)
//                }
//            }
//        }
//
//        fileObserver.startWatching()
//        Log.d(TAG, "FileObserver 감시 시작됨")

        // 주기적인 폴더 스캔 시작 (첫 스캔은 10초 후)
//        handler.postDelayed({
//            contentObserver.scanDirectory()
//            // 이후부터 주기적 스캔
//            handler.postDelayed(scanRunnable, 120000)
//        }, 10000) // 10초 지연
//
//        Log.d(TAG, "주기적 폴더 스캔은 10초 후 시작됨")

        // 주기적인 폴더 스캔 시작 (필요에 따라 유지 또는 제거)
        handler.post(scanRunnable)
        Log.d(TAG, "주기적 폴더 스캔 시작됨")

        // 시작 시 즉시 첫 스캔 수행 (필요에 따라 유지 또는 제거)
        contentObserver.scanDirectory()

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

                    // 비디오 파일 분석 시작 - AccidentAnalyzer 사용
                    accidentAnalyzer.analyzeVideoForAccident(file)

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
        // 저장소 모니터링 작업 취소 추가
        storageMonitorJob?.cancel()
        // AccidentAnalyzer 리소스 정리
        accidentAnalyzer.onDestroy()
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