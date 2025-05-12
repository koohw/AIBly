package com.crush.aiblackboxreview.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

import android.app.Notification
import android.media.AudioAttributes
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
    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    // 특정 폴더 경로 (블랙박스 앱이 저장하는 경로)
    private val targetFolderPath = "/storage/emulated/0/DCIM"

    // 마지막으로 처리한 파일 목록을 저장하는 Set
    private val processedFiles = mutableSetOf<String>()

    // 알림 관련 상수 추가
    private val CHANNEL_ID = "video_detection_channel"
    private val notificationIdGenerator = AtomicInteger(2000) // 고유한 알림 ID 생성을 위한 변수

    // 초기화 블록에서 알림 채널 생성
    init {
        createNotificationChannel()
    }

    // 알림 채널 생성 메소드
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "블랙박스 영상 감지"
            val descriptionText = "새로운 블랙박스 영상이 감지되었을 때 알림을 표시합니다"
            val importance = NotificationManager.IMPORTANCE_HIGH // 중요도 높게 설정
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
        Log.d(TAG, "onChange 호출됨: $uri")

        // 백그라운드 스레드에서 실행
        executor.execute {
            uri?.let {
                checkIfNewVideoAdded(it)
            }

            // uri와 관계없이 직접 폴더도 확인 (추가 안전장치)
            //scanDirectoryForNewVideos()
        }
    }

    // 폴더 스캔 최적화
//    private var lastScanTime = 0L
//    private val MIN_SCAN_INTERVAL = 5000L // 최소 5초 간격으로 스캔

    // 테스트용 메소드: 특정 폴더 직접 스캔
//    fun scanDirectory() {
//        val currentTime = System.currentTimeMillis()
//
//        // 너무 자주 스캔하지 않도록 방지
//        if (currentTime - lastScanTime < MIN_SCAN_INTERVAL) {
//            Log.d(TAG, "최근에 스캔 완료 (${(currentTime - lastScanTime) / 1000}초 전), 스캔 건너뜀")
//            return
//        }
//
//        lastScanTime = currentTime
//
//        executor.execute {
//            Log.d(TAG, "수동 폴더 스캔 시작: $targetFolderPath")
//            scanDirectoryForNewVideos()
//        }
//    }

    // 테스트용 메소드: 특정 폴더 직접 스캔 (필요에 따라 유지)
    fun scanDirectory() {
        executor.execute {
            Log.d(TAG, "수동 폴더 스캔 시작")
            scanDirectoryForNewVideos()
        }
    }


    // 폴더 직접 스캔 메소드
    private fun scanDirectoryForNewVideos() {
//        val directory = File(targetFolderPath)
//
//        if (!directory.exists()) {
//            Log.d(TAG, "폴더가 존재하지 않음: $targetFolderPath")
//            return
//        }
//
//        val files = directory.listFiles()
//        Log.d(TAG, "폴더 내 파일 수: ${files?.size ?: 0}")
//
//        // 파일 정렬 추가 (가장 오래된 파일부터 처리)
//        val sortedFiles = files?.sortedBy { it.lastModified() }
//
//
//        sortedFiles?.forEach { file ->
//            if (file.name.endsWith(
//                    ".mp4",
//                    ignoreCase = true
//                ) && !processedFiles.contains(file.absolutePath)
//            ) {
//                Log.d(TAG, "새 MP4 파일 발견 (직접 스캔): ${file.absolutePath}")
//
//                // 알림 표시
//                showVideoDetectedNotification(file.name, file.absolutePath)
//
//                // 처리 목록에 추가
//                processedFiles.add(file.absolutePath)
//
//                // 분석 요청
//                analyzeVideo(file.absolutePath)
//            }
//        }
    }

    private fun checkIfNewVideoAdded(uri: Uri) {
        Log.d(TAG, "비디오 확인 시작 - URI: $uri")

        // 먼저 직접 URI를 확인해 보세요
        if (uri.toString().startsWith("content://media/external/video/media/")) {
            Log.d(TAG, "비디오 URI 직접 확인: $uri")

            try {
                // 해당 URI의 콘텐츠 직접 가져오기
                val contentUri = uri
                val projection = arrayOf(
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.MIME_TYPE
                )

                context.contentResolver.query(
                    contentUri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val pathColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                        val nameColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                        val mimeTypeColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)

                        val filePath = if (pathColumnIndex >= 0) cursor.getString(pathColumnIndex) else null
                        val fileName = if (nameColumnIndex >= 0) cursor.getString(nameColumnIndex) else "Unknown"
                        val mimeType = if (mimeTypeColumnIndex >= 0) cursor.getString(mimeTypeColumnIndex) else ""

                        Log.d(TAG, "URI에서 직접 파일 정보 가져옴: 경로=$filePath, 이름=$fileName, MIME=$mimeType")

                        if (filePath != null && isVideoFile(filePath, mimeType) && !processedFiles.contains(filePath)) {
                            Log.d(TAG, "새 비디오 파일 감지됨 (URI 직접 확인): $filePath")

                            // 알림 및 분석 요청
                            showVideoDetectedNotification(fileName, filePath)
                            processedFiles.add(filePath)
                            analyzeVideo(filePath)
                            return
                        }
                        else {
                            // 명시적인 else 절 추가
                            Log.d(TAG, "파일이 비디오가 아니거나 이미 처리됨")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "URI 직접 확인 중 오류: ${e.message}")
            }
        }


        // 현재 시간 기록 (디버깅용)
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val timeThreshold = currentTimeSeconds - 60
        Log.d(TAG, "현재 시간(초): $currentTimeSeconds, 임계값: $timeThreshold")

        // 미디어 스토어에서 최근 추가된 비디오 확인
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ?"
        val selectionArgs = arrayOf((System.currentTimeMillis() / 1000 - 300).toString()) // 5분 이내

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val count = cursor.count
                Log.d(TAG, "최근 추가된 비디오 파일 수: $count")

                while (cursor.moveToNext()) {
                    val pathColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val idColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                    val mimeTypeColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                    val nameColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateAddedColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                    if (pathColumnIndex >= 0 && idColumnIndex >= 0) {
                        val filePath = cursor.getString(pathColumnIndex)
                        val id = cursor.getLong(idColumnIndex)
                        val mimeType = if (mimeTypeColumnIndex >= 0) cursor.getString(mimeTypeColumnIndex) else ""
                        val fileName = if (nameColumnIndex >= 0) cursor.getString(nameColumnIndex) else "Unknown"
                        val dateAdded = if (dateAddedColumnIndex >= 0) cursor.getLong(dateAddedColumnIndex) else 0

                        Log.d(TAG, "비디오 파일 정보: 경로=$filePath, ID=$id, 이름=$fileName, 추가시간=$dateAdded, MIME=$mimeType")
                        // 비디오 파일인지 확인 (MP4, 3GP, MKV 등 다양한 비디오 포맷 허용)
                        if (isVideoFile(filePath, mimeType) && !processedFiles.contains(filePath)) {
                            Log.d(TAG, "새 비디오 파일 감지됨: $filePath")

                            showVideoDetectedNotification(fileName, filePath)
                            processedFiles.add(filePath)

                            // GPT API에 영상 분석 요청
                            analyzeVideo(filePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "새 비디오 확인 중 오류 발생", e)
        }
    }

    // 비디오 파일 여부를 확인하는 헬퍼 메소드 추가
    private fun isVideoFile(filePath: String, mimeType: String): Boolean {
        // 먼저 로깅 추가
        Log.d(TAG, "파일 확인: 경로=$filePath, MIME=$mimeType")

        // MIME 타입으로 확인
        if (mimeType.startsWith("video/")) {
            return true
        }

        // 확장자로 확인 (MIME 타입이 없는 경우)
        val extension = filePath.lowercase().substringAfterLast('.', "")
        val videoExtensions = setOf("mp4", "3gp", "mkv", "avi", "mov", "webm", "flv", "wmv", "ts")

        val isVideo = extension in videoExtensions
        if (isVideo) {
            Log.d(TAG, "확장자로 비디오 파일 확인됨: $extension")
        }

        return isVideo
    }

    private fun showVideoDetectedNotification(fileName: String, filePath: String) {
        try {
            // 알림 채널 확인 및 설정
            val channelId = "video_detection_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // 기존 채널 삭제 (설정을 완전히 초기화하기 위해)
                notificationManager.deleteNotificationChannel(channelId)

                // 새 채널 생성
                val channel = NotificationChannel(
                    channelId,
                    "블랙박스 영상 감지",
                    NotificationManager.IMPORTANCE_HIGH  // 높은 중요도 설정
                ).apply {
                    description = "새로운 블랙박스 영상이 감지되었을 때 알림을 표시합니다"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    enableLights(true)
                    setSound(
                        android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    // 헤드업 알림을 강제로 활성화
                    importance = NotificationManager.IMPORTANCE_HIGH
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "알림 채널 다시 설정됨 (헤드업 알림 활성화)")
            }

            // 인텐트 설정
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("VIDEO_PATH", filePath)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),  // 매번 다른 요청 코드 사용
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성 (헤드업 알림 강제)
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⚠️ 새로운 블랙박스 영상 감지됨!")  // 이모지 추가
                .setContentText("파일명: $fileName")
                .setPriority(NotificationCompat.PRIORITY_MAX)  // 최대 우선순위
                .setCategory(NotificationCompat.CATEGORY_ALARM)  // 알람 카테고리 사용
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)  // 모든 기본 설정 사용
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("새로운 블랙박스 영상이 감지되었습니다.\n파일명: $fileName\n경로: $filePath"))
                // 헤드업 알림을 위한 추가 설정
                .setFullScreenIntent(pendingIntent, true)  // 전체 화면 인텐트 추가

            // 매번 새로운 알림 ID 사용
            val notificationId = System.currentTimeMillis().toInt()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(TAG, "알림 표시됨 (헤드업 모드): $fileName (ID: $notificationId)")
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
}