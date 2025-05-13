package com.crush.aiblackboxreview.analysis

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.crush.aiblackboxreview.MainActivity
import com.crush.aiblackboxreview.R
import com.crush.aiblackboxreview.api.OpenAIClient
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AccidentAnalyzer(private val context: Context) {
    private val TAG = "AccidentAnalyzer"
    private val openAIClient = OpenAIClient()

    // 작업 큐 추가
    private val videoQueue = ConcurrentLinkedQueue<File>()

    // 현재 처리 중인지 상태 관리
    private var isProcessing = AtomicBoolean(false)

    // 분석용 코루틴 스코프
    private val analyzerScope = CoroutineScope(Dispatchers.IO)

    // 비디오 분석 시작하는 메소드
    fun analyzeVideoForAccident(videoFile: File) {
        val filePath = videoFile.absolutePath

        // 파일 크기 확인 - 0바이트인 경우 처리하지 않음
        if (videoFile.length() == 0L) {
            Log.d(TAG, "파일 크기가 0인 파일은 처리하지 않음 (아직 쓰기 중): ${videoFile.name}")
            return
        }

        // 이미 큐에 있는지 확인
        if (videoQueue.any { it.absolutePath == filePath }) {
            Log.d(TAG, "이미 큐에 있는 파일입니다: ${videoFile.name}")
            return
        }

        Log.d(TAG, "비디오 분석 큐에 추가: ${videoFile.name}")
        videoQueue.add(videoFile)

        // 처리가 진행 중이 아니면 처리 시작
        if (isProcessing.compareAndSet(false, true)) {
            processNextVideo()
        } else {
            Log.d(TAG, "다른 비디오 처리 중, 큐에서 대기 중: ${videoFile.name}")
        }
    }
    // 비디오 큐에서 다음 항목 처리
    private fun processNextVideo() {
        val videoFile = videoQueue.poll()

        if (videoFile == null) {
            Log.d(TAG, "처리할 비디오가 더 이상 없음, 대기 상태로 전환")
            isProcessing.set(false)
            return
        }

        Log.d(TAG, "큐에서 다음 비디오 처리 시작: ${videoFile.name} (큐 크기: ${videoQueue.size})")

        // 각 비디오 처리를 위한 작업 생성
        val videoJob = analyzerScope.launch {
            try {
                // 파일 상태 확인
                if (!videoFile.exists() || !videoFile.canRead() || videoFile.length() < 1024) {
                    Log.e(TAG, "파일이 유효하지 않음: ${videoFile.name}, 존재=${videoFile.exists()}, 읽기가능=${videoFile.canRead()}, 크기=${videoFile.length()}bytes")
                    return@launch
                }

                // 파일 시스템 안정화를 위한 지연 추가 (2초)
                Log.d(TAG, "파일 시스템 안정화 지연 시작: ${videoFile.name}")
                delay(2000)
                Log.d(TAG, "파일 시스템 안정화 지연 완료: ${videoFile.name}")

                // 시스템 메모리 상태 출력
                val rt = Runtime.getRuntime()
                val usedMemory = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
                val maxMemory = rt.maxMemory() / 1024 / 1024
                Log.d(TAG, "현재 메모리 사용량: ${usedMemory}MB / 최대: ${maxMemory}MB")

                // GC 힌트 (메모리 확보 시도)
                System.gc()
                delay(500)  // GC에 약간의 시간 제공

                // 타임아웃이 적용된 비디오 처리 작업
                val processingResult = withTimeoutOrNull(5 * 60 * 1000L) { // 5분 타임아웃
                    processVideoWithFrames(videoFile)
                }

                if (processingResult == null) {
                    // 타임아웃이 발생한 경우
                    Log.e(TAG, "비디오 처리 타임아웃 (3분 초과): ${videoFile.name}")
                } else {
                    Log.d(TAG, "비디오 처리 완료: ${videoFile.name}, 사고 감지 여부: $processingResult")
                }

            } catch (e: Exception) {
                Log.e(TAG, "비디오 분석 중 오류 발생: ${e.message}", e)
            } finally {
                // 약간의 지연 후 다음 비디오 처리 (시스템 부하 완화)
                Log.d(TAG, "${videoFile.name} 처리 완료, 다음 비디오 처리 준비 중...")
                delay(2000) // 2초 지연
                processNextVideo()
            }
        }

        // 작업이 지나치게 오래 실행되는 것을 방지하기 위한 추가 보호 장치
        analyzerScope.launch {
            delay(5 * 60 * 1000L) // 5분 후 확인
            if (videoJob.isActive) {
                // 5분 이상 실행 중인 작업은 강제 종료
                Log.w(TAG, "비디오 작업이 5분 이상 실행 중, 강제 종료: ${videoFile.name}")
                videoJob.cancel()

                // 다음 비디오로 이동 (현재 작업이 멈췄을 가능성 고려)
                if (isProcessing.get()) {
                    delay(3000) // 3초 기다린 후
                    if (videoJob.isCancelled || videoJob.isCompleted) {
                        processNextVideo()
                    }
                }
            }
        }
    }

    // 프레임 추출 및 분석 작업을 수행하는 보조 메소드
    private suspend fun processVideoWithFrames(videoFile: File): Boolean {
        // 비디오에서 프레임 추출 (3번까지 재시도)
        var frames = listOf<Bitmap>()
        var extractRetryCount = 0

        while (frames.isEmpty() && extractRetryCount < 3) {
            // 프레임 추출 시도에 타임아웃 적용
            frames = withTimeoutOrNull(60 * 1000L) { // 60초 타임아웃
                extractKeyFramesWithRetry(videoFile.absolutePath, 10)
            } ?: listOf() // 타임아웃 시 빈 리스트 반환

            if (frames.isEmpty()) {
                extractRetryCount++
                Log.e(TAG, "프레임 추출 실패 또는 타임아웃, 재시도 ${extractRetryCount}/3: ${videoFile.name}")
                delay(2000L * extractRetryCount) // 점진적으로 더 긴 지연
            }
        }

        if (frames.isEmpty()) {
            Log.e(TAG, "최대 재시도 횟수 초과, 프레임을 추출할 수 없습니다: ${videoFile.name}")
            return false
        }

        try {
            // 중후반부 프레임 우선 분석 전략 적용
            var isAccident = false

            // 프레임의 분석 순서 설정 (중후반부 우선)
            val frameIndices = getPriorityFrameIndices(frames.size)
            Log.d(TAG, "${videoFile.name}의 분석 순서: $frameIndices")

            // 1. 우선순위 프레임 분석
            for (index in frameIndices) {
                if (index >= frames.size) continue

                val frame = frames[index]
                Log.d(TAG, "${videoFile.name}의 우선순위 프레임[${index}] 분석 중...")

                try {
                    // 프레임 분석에 타임아웃 적용
                    isAccident = withTimeoutOrNull(45 * 1000L) { // 45초 타임아웃
                        openAIClient.analyzeTrafficAccident(frame)
                    } ?: run {
                        Log.e(TAG, "프레임[${index}] 분석 타임아웃 (45초 초과): ${videoFile.name}")
                        false // 타임아웃 시 사고 아님으로 처리
                    }

                    // 사고가 감지되면 알림 표시하고 중단
                    if (isAccident) {
                        Log.d(TAG, "교통사고 감지됨! (프레임[${index}]): ${videoFile.name}")
                        withContext(Dispatchers.Main) {
                            sendAccidentNotification(videoFile)
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "프레임[${index}] 분석 중 오류 발생: ${e.message}")
                }
            }

            // 2. 우선순위 프레임에서 사고가 감지되지 않은 경우 나머지 프레임 분석
            if (!isAccident) {
                val remainingIndices = (0 until frames.size).filter { it !in frameIndices }
                Log.d(TAG, "우선순위 프레임에서 사고 미감지, 나머지 프레임 분석: $remainingIndices")

                for (index in remainingIndices) {
                    val frame = frames[index]
                    Log.d(TAG, "${videoFile.name}의 일반 프레임[${index}] 분석 중...")

                    try {
                        // 프레임 분석에 타임아웃 적용
                        isAccident = withTimeoutOrNull(45 * 1000L) { // 45초 타임아웃
                            openAIClient.analyzeTrafficAccident(frame)
                        } ?: run {
                            Log.e(TAG, "프레임[${index}] 분석 타임아웃 (45초 초과): ${videoFile.name}")
                            false
                        }

                        // 사고가 감지되면 알림 표시하고 중단
                        if (isAccident) {
                            Log.d(TAG, "교통사고 감지됨! (프레임[${index}]): ${videoFile.name}")
                            withContext(Dispatchers.Main) {
                                sendAccidentNotification(videoFile)
                            }
                            break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "프레임[${index}] 분석 중 오류 발생: ${e.message}")
                    }
                }
            }

            if (!isAccident) {
                Log.d(TAG, "교통사고 미감지: ${videoFile.name}")
            }

            // 메모리 해제
            for (frame in frames) {
                if (!frame.isRecycled) {
                    frame.recycle()
                }
            }
            frames = emptyList()
            System.gc()

            return isAccident

        } catch (e: Exception) {
            Log.e(TAG, "프레임 분석 중 예외 발생: ${e.message}", e)

            // 메모리 해제
            for (frame in frames) {
                if (!frame.isRecycled) {
                    frame.recycle()
                }
            }
            frames = emptyList()
            System.gc()

            return false
        }
    }

    // 우선순위 프레임 인덱스 계산 함수 추가
    private fun getPriorityFrameIndices(totalFrames: Int): List<Int> {
        return when {
            totalFrames <= 3 -> (0 until totalFrames).toList() // 3개 이하면 모두 우선

            else -> listOf(
                totalFrames - 1,       // 마지막 프레임 (90-100% 지점)
                totalFrames - 2,       // 뒤에서 두 번째 프레임 (80-90% 지점)
                totalFrames * 7 / 10,  // 70% 지점 프레임
                totalFrames * 6 / 10,  // 60% 지점 프레임
                totalFrames * 5 / 10   // 50% 지점 프레임 (중간)
            )
        }
    }

    // 비디오에서 키프레임 추출하는 메소드 (파일 디스크립터 방식)
    private fun extractKeyFramesWithRetry(videoPath: String, frameCount: Int): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        val videoFile = File(videoPath)

        if (!videoFile.exists()) {
            Log.e(TAG, "비디오 파일이 존재하지 않습니다: $videoPath")
            return frames
        }

        if (!videoFile.canRead()) {
            Log.e(TAG, "비디오 파일을 읽을 수 없습니다: $videoPath")
            return frames
        }


        // 첫 번째 방법: 직접 경로 사용
        if (!extractFramesWithDirectPath(videoPath, frames)) {
            Log.d(TAG, "직접 경로 방식 실패, 파일 디스크립터 방식 시도: $videoPath")
            // 두 번째 방법: 파일 디스크립터 사용
            extractFramesWithFileDescriptor(videoPath, frames)
        }

        Log.d(TAG, "총 ${frames.size}개의 프레임을 추출했습니다")
        return frames
    }

    // 직접 경로 방식으로 프레임 추출
    private fun extractFramesWithDirectPath(videoPath: String, frames: MutableList<Bitmap>): Boolean {
        var retriever: MediaMetadataRetriever? = null
        try {
            // 항상 새 인스턴스 생성
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)

            // 미디어 정보 로깅
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "알 수 없음"
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "알 수 없음"
            val mimetype = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "알 수 없음"

            Log.d(TAG, "비디오 정보: ${width}x${height}, 타입=${mimetype}")

            // 비디오 길이 확인
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationStr == null) {
                Log.e(TAG, "비디오 길이 메타데이터를 찾을 수 없습니다: $videoPath")
                return false
            }

            val duration = durationStr.toLong()
            Log.d(TAG, "비디오 길이(ms): $duration")

            if (duration <= 0) {
                Log.e(TAG, "비디오 길이가 유효하지 않습니다: $duration")
                return false
            }

            // 프레임 추출 전략: 10개 프레임, 중후반부 집중
            // 초반 3개 (0%, 10%, 20%), 중반 2개 (30%, 40%), 후반 5개 (50%, 60%, 70%, 80%, 90%)
            val percentages = listOf(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)

            for (percentage in percentages) {
                val timeMs = (duration * percentage).toLong()
                val timeUs = timeMs * 1000 // 마이크로초로 변환
                Log.d(TAG, "영상의 ${percentage * 100}% 지점(${timeMs}ms)에서 프레임 추출 시도")

                try {
                    val bitmap = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )

                    if (bitmap == null) {
                        Log.e(TAG, "시간(${timeMs}ms)에서 프레임을 추출할 수 없습니다")
                        continue
                    }

                    // 너무 큰 이미지는 리사이즈
                    val resizedBitmap = resizeBitmapIfNeeded(bitmap)
                    frames.add(resizedBitmap)
                    Log.d(TAG, "영상의 ${percentage * 100}% 지점 프레임 추출 성공 (${resizedBitmap.width}x${resizedBitmap.height})")

                    // 원본 비트맵이 다르면 메모리 해제
                    if (resizedBitmap != bitmap && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "프레임 추출 중 오류: ${e.message}")
                }
            }

            return frames.isNotEmpty()

        } catch (e: Exception) {
            Log.e(TAG, "직접 경로 방식 비디오 메타데이터 추출 중 오류 발생", e)
            return false
        } finally {
            try {
                retriever?.release()
                retriever = null
            } catch (e: Exception) {
                Log.e(TAG, "MediaMetadataRetriever 해제 중 오류", e)
            }
        }
    }

    // 파일 디스크립터 방식으로 프레임 추출
    private fun extractFramesWithFileDescriptor(videoPath: String, frames: MutableList<Bitmap>): Boolean {
        var retriever: MediaMetadataRetriever? = null
        var fis: FileInputStream? = null

        try {
            // 파일 입력 스트림 및 파일 디스크립터 획득
            fis = FileInputStream(videoPath)
            val fd = fis.fd

            // 항상 새 인스턴스 생성
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)

            val timePoints = listOf(
                0L,             // 0초 (0%)
                2000000L,       // 2초 (20%)
                4000000L,       // 4초 (40%)
                5000000L,       // 5초 (50%)
                6000000L,       // 6초 (60%)
                7000000L,       // 7초 (70%)
                8000000L,       // 8초 (80%)
                8500000L,       // 8.5초 (85%)
                9000000L,       // 9초 (90%)
                9500000L        // 9.5초 (95%)
            )

            for ((index, timeUs) in timePoints.withIndex()) {
                try {
                    val bitmap = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )

                    bitmap?.let {
                        val resizedBitmap = resizeBitmapIfNeeded(it)
                        frames.add(resizedBitmap)
                        val seconds = timeUs / 1000000.0
                        val percentage = (seconds / 10.0) * 100.0  // 10초 기준으로 백분율 계산
                        Log.d(TAG, "FD 방식: 프레임 ${index+1}/10 - ${seconds}초 지점(${percentage}%)에서 프레임 추출 성공")

                        // 원본 비트맵이 다르면 메모리 해제
                        if (resizedBitmap != bitmap && !bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "FD 방식: 프레임 추출 실패 - ${e.message}")
                }
            }

            return frames.isNotEmpty()

        } catch (e: Exception) {
            Log.e(TAG, "파일 디스크립터 방식 비디오 처리 중 오류 발생", e)
            return false
        } finally {
            try {
                fis?.close()
            } catch (e: Exception) {
                Log.e(TAG, "FileInputStream 닫기 오류", e)
            }

            try {
                retriever?.release()
                retriever = null
            } catch (e: Exception) {
                Log.e(TAG, "MediaMetadataRetriever(FD) 해제 중 오류", e)
            }
        }
    }

    // 비트맵 리사이징 헬퍼 함수
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        return if (bitmap.width > 800 || bitmap.height > 800) {
            val scale = 800f / Math.max(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Log.d(TAG, "비트맵 리사이징: ${bitmap.width}x${bitmap.height} -> ${newWidth}x${newHeight}")
            val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            resized
        } else {
            bitmap
        }
    }

    // 사고 감지 시 알림을 표시하는 메소드
    private fun sendAccidentNotification(videoFile: File) {
        val accidentChannelId = "accident_channel"

        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "교통사고 알림"
            val descriptionText = "교통사고가 감지되었을 때 표시되는 알림"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(accidentChannelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 클릭 시 앱 실행
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 생성
        val builder = NotificationCompat.Builder(context, accidentChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("교통사고 감지")
            .setContentText("블랙박스 영상에서 교통사고가 감지되었습니다: ${videoFile.name}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        // 알림 표시 (ID를 다르게 해서 기존 알림과 겹치지 않게)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2001, builder.build())
    }

    // 클래스 리소스 정리
    fun onDestroy() {
        analyzerScope.cancel()
        videoQueue.clear()
        isProcessing.set(false)
    }
}