package com.crush.aiblackboxreview.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.crush.aiblackboxreview.MainActivity
import com.crush.aiblackboxreview.analysis.AccidentAnalyzer
import com.crush.aiblackboxreview.R
import java.util.concurrent.atomic.AtomicInteger

class VideoContentObserver(
    private val context: Context,
    handler: Handler,
    private val accidentAnalyzer: AccidentAnalyzer
) : ContentObserver(handler) {

    private val TAG = "VideoContentObserver"

    // 스레드 풀 설정을 통한 리소스 관리 개선
    private val executor = Executors.newSingleThreadExecutor()

    // 특정 폴더 경로 (블랙박스 앱이 저장하는 경로)
    private val targetFolderPath = "/storage/emulated/0/DCIM"

    // 마지막으로 처리한 파일을 관리하는 LRU 캐시로 변경 (메모리 누수 방지)
    private val processedFiles = LinkedHashSet<String>(500) // 최대 500개 경로만 저장
    private val processLock = Any() // 스레드 안전성을 위한 잠금 객체

    // 알림 관련 상수 추가
    private val CHANNEL_ID = "video_detection_channel"
    private val notificationIdGenerator = AtomicInteger(2000) // 고유한 알림 ID 생성을 위한 변수

    // 마지막 스캔 시간 (불필요한 중복 스캔 방지)
    private var lastScanTime = 0L
    private val MIN_SCAN_INTERVAL = 10 * 60 * 1000L // 최소 10분 간격

    init {
        createNotificationChannel()
    }

    // 알림 채널 생성 메소드
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "블랙박스 영상 감지"
            val descriptionText = "새로운 블랙박스 영상이 감지되었을 때 알림을 표시합니다"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        Log.d(TAG, "미디어 콘텐츠 변경 감지됨: $uri")

        // 백그라운드 스레드에서 실행
        executor.execute {
            uri?.let {
                checkIfNewVideoAdded(it)
            }
        }
    }

    fun scanDirectory() {
        val currentTime = System.currentTimeMillis()

        // 너무 자주 스캔하지 않도록 최소 10분 간격으로 스캔 제한
        if (currentTime - lastScanTime < MIN_SCAN_INTERVAL) {
            Log.d(TAG, "최근에 스캔 완료 (${(currentTime - lastScanTime) / 1000}초 전), 스캔 건너뜀")
            return
        }

        lastScanTime = currentTime
        Log.d(TAG, "주기적 폴더 스캔 시작")

        // 별도 스레드에서 실행
        executor.execute {
            scanDCIMForMP4()
        }
    }

    // DCIM 폴더의 MP4 파일만 스캔하는 효율적인 메소드
    private fun scanDCIMForMP4() {
        val prefs = context.getSharedPreferences("AutoDetectSettings", Context.MODE_PRIVATE)
        val autoDetectEnabled = prefs.getBoolean("autoDetectEnabled", true)

        if (!autoDetectEnabled) {
            Log.d(TAG, "자동 감지 비활성화 상태: 스캔 건너뜀")
            return
        }

        try {
            // 최근 10분 내 추가된 MP4 파일만 검색으로 변경
            val tenMinutesAgo = (System.currentTimeMillis() / 1000) - (10 * 60) // 10분 = 600초
            Log.d(TAG, "최근 10분 내 추가된 파일 스캔 시작")
            val projection = arrayOf(
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DISPLAY_NAME
            )

            // 효율적인 SQL 쿼리로 DCIM 폴더의 MP4, AVI 파일만 필터링
            val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ? AND " +
                    "${MediaStore.Video.Media.DATA} LIKE ? AND " +
                    "(${MediaStore.Video.Media.DATA} LIKE ? OR ${MediaStore.Video.Media.DATA} LIKE ?)"

            val selectionArgs = arrayOf(
                tenMinutesAgo.toString(),
                "$targetFolderPath%",  // DCIM 폴더 및 모든 하위 폴더
                "%.mp4",               // MP4 파일
                "%.avi"                // AVI 파일 추가
            )

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val count = cursor.count
                Log.d(TAG, "최근 10분 내 DCIM 폴더의 MP4 파일 수: $count")

                var processedCount = 0

                while (cursor.moveToNext()) {
                    val pathColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val nameColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)

                    if (pathColumnIndex >= 0) {
                        val filePath = cursor.getString(pathColumnIndex)
                        val fileName = if (nameColumnIndex >= 0) cursor.getString(nameColumnIndex) else "Unknown"

                        synchronized(processLock) {
                            if (!processedFiles.contains(filePath)) {
                                Log.d(TAG, "새 MP4 파일 감지됨 (주기적 스캔): $filePath")

                                val file = File(filePath)
                                if (file.exists() && file.length() > 0) {
                                    showVideoDetectedNotification(fileName, filePath)
                                    addToProcessedFiles(filePath)
                                    analyzeVideo(filePath)
                                    processedCount++
                                }
                            }
                        }

                        // 한 번에 최대 5개만 처리
                        if (processedCount >= 5) {
                            Log.d(TAG, "주기적 스캔에서 최대 5개 파일 처리 제한에 도달")
                            break
                        }
                    }
                }

                Log.d(TAG, "주기적 스캔에서 처리된 새 MP4 파일 수: $processedCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "주기적 스캔 중 오류 발생", e)
        }
    }

    private fun checkIfNewVideoAdded(uri: Uri) {

        // 서비스로부터 설정 확인
        val prefs = context.getSharedPreferences("AutoDetectSettings", Context.MODE_PRIVATE)
        val autoDetectEnabled = prefs.getBoolean("autoDetectEnabled", true)

        // 자동 감지가 비활성화되어 있으면 처리하지 않음
        if (!autoDetectEnabled) {
            Log.d(TAG, "자동 감지 비활성화 상태: 미디어 변경 무시")
            return
        }

        // 직접 URI 확인 (효율적인 방법)
        if (uri.toString().startsWith("content://media/external/video/media/")) {
            try {
                val projection = arrayOf(
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME
                )

                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val pathColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                        val nameColumnIndex =
                            cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)

                        val filePath =
                            if (pathColumnIndex >= 0) cursor.getString(pathColumnIndex) else null
                        val fileName =
                            if (nameColumnIndex >= 0) cursor.getString(nameColumnIndex) else "Unknown"

                        synchronized(processLock) {
                            if (filePath != null &&
                                filePath.startsWith(targetFolderPath) &&
                                (filePath.lowercase().endsWith(".mp4") || filePath.lowercase().endsWith(".avi")) &&
                                !processedFiles.contains(filePath)
                            ) {
                                Log.d(TAG, "새 비디오 파일 감지됨 (URI 직접 확인): $filePath")

                                showVideoDetectedNotification(fileName, filePath)
                                addToProcessedFiles(filePath)
                                analyzeVideo(filePath)
                                return
                            } else if (filePath != null) {
                                // 처리되지 않은 이유 로그
                                val inDCIM = filePath.startsWith(targetFolderPath)
                                val isVideoFormat = filePath.lowercase().endsWith(".mp4") || filePath.lowercase().endsWith(".avi")
                                val isProcessed = processedFiles.contains(filePath)

                                Log.d(
                                    TAG,
                                    "파일 처리 안됨: $filePath (DCIM폴더=$inDCIM, 비디오포맷=$isVideoFormat, 이미처리=$isProcessed)"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "URI 직접 확인 중 오류: ${e.message}")
            }
        }

        // 최근 추가된 비디오 확인 (URI가 직접 확인되지 않은 경우의 백업 방법)
        val fiveMinutesAgo = (System.currentTimeMillis() / 1000) - 300 // 5분 이내

        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        // 효율적인 SQL 필터링
        val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ? AND " +
                "${MediaStore.Video.Media.DATA} LIKE ? AND " +
                "${MediaStore.Video.Media.DATA} LIKE ?"

        val selectionArgs = arrayOf(
            fiveMinutesAgo.toString(),
            "$targetFolderPath%",
            "%.mp4",
            "%.avi"
        )

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val count = cursor.count
                if (count > 0) {
                    Log.d(TAG, "최근 5분 내 추가된 비디오 파일 수: $count")
                }

                while (cursor.moveToNext()) {
                    val pathColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val nameColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)

                    if (pathColumnIndex >= 0) {
                        val filePath = cursor.getString(pathColumnIndex)
                        val fileName =
                            if (nameColumnIndex >= 0) cursor.getString(nameColumnIndex) else "Unknown"

                        // 동기화 블록으로 스레드 안전성 보장
                        synchronized(processLock) {
                            if (!processedFiles.contains(filePath)) {
                                Log.d(TAG, "새 MP4 파일 감지됨 (최근 파일 검색): $filePath")

                                val file = File(filePath)
                                if (file.exists() && file.length() > 0) {
                                    showVideoDetectedNotification(fileName, filePath)
                                    addToProcessedFiles(filePath)
                                    analyzeVideo(filePath)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
                Log.e(TAG, "새 비디오 확인 중 오류 발생", e)
        }

    }

    // processedFiles 관리 - 메모리 누수 방지
    private fun addToProcessedFiles(filePath: String) {
        synchronized(processLock) {
            processedFiles.add(filePath)
            // 최대 크기 제한
            if (processedFiles.size > 500) {
                // 가장 오래된 항목 제거 (LinkedHashSet의 첫 번째 항목)
                processedFiles.remove(processedFiles.iterator().next())
            }
        }
    }

    private fun showVideoDetectedNotification(fileName: String, filePath: String) {
        try {
            // 알림 채널 설정
            val channelId = CHANNEL_ID

            // 중요: 매번 채널을 삭제하고 다시 생성하는 것은 비효율적이며 필요하지 않음
            // 기존 채널이 있으면 재사용하고, 없을 때만 생성하도록 수정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // 채널이 없을 때만 생성
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId,
                        "블랙박스 영상 감지",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "새로운 블랙박스 영상이 감지되었을 때 알림을 표시합니다"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 250, 500)
                        enableLights(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }

            // 인텐트 설정
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("VIDEO_PATH", filePath)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⚠️ 새로운 블랙박스 영상 감지됨!")
                .setContentText("파일명: $fileName")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("새로운 블랙박스 영상이 감지되었습니다.\n파일명: $fileName\n경로: $filePath"))
                .setFullScreenIntent(pendingIntent, true)

            val notificationId = notificationIdGenerator.incrementAndGet()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(TAG, "알림 표시됨: $fileName (ID: $notificationId)")
        } catch (e: Exception) {
            Log.e(TAG, "알림 표시 중 오류 발생", e)
        }
    }

    private fun analyzeVideo(videoPath: String) {
        val file = File(videoPath)
        if (file.exists() && file.length() > 0) {
            Log.d(TAG, "AccidentAnalyzer에 분석 요청: $videoPath (파일 크기: ${file.length()} bytes)")
            accidentAnalyzer.analyzeVideoForAccident(file)
        } else {
            Log.e(TAG, "파일이 존재하지 않거나 비어있습니다: $videoPath")
        }
    }

    // 서비스 종료 시 리소스 정리
    fun shutdown() {
        try {
            executor.shutdown()
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: Exception) {
            executor.shutdownNow()
            Log.e(TAG, "Executor 종료 중 오류", e)
        }

        synchronized(processLock) {
            processedFiles.clear()
        }
    }
}