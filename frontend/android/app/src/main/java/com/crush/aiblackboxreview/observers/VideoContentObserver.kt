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

class VideoContentObserver(
    private val context: Context,
    handler: Handler
) : ContentObserver(handler) {

    private val TAG = "VideoContentObserver"
    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    // 특정 폴더 경로 (블랙박스 앱이 저장하는 경로)
    private val targetFolderPath = "/storage/emulated/0/DCIM/finevu_cloud"

    // 마지막으로 처리한 파일 목록을 저장하는 Set
    private val processedFiles = mutableSetOf<String>()

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        Log.d(TAG, "미디어 콘텐츠 변경 감지됨: $uri")
        Log.d(TAG, "onChange 호출됨: $uri")
        Log.d(TAG, "현재 감시 중인 폴더: $targetFolderPath")
        // 백그라운드 스레드에서 실행
        executor.execute {
            uri?.let {
                checkIfNewVideoAdded(it)
            }

            // uri와 관계없이 직접 폴더도 확인 (추가 안전장치)
            scanDirectoryForNewVideos()
        }
    }

    // 테스트용 메소드: 특정 폴더 직접 스캔
    fun scanDirectory() {
        executor.execute {
            Log.d(TAG, "수동 폴더 스캔 시작: $targetFolderPath")
            scanDirectoryForNewVideos()
        }
    }

    // 폴더 직접 스캔 메소드
    private fun scanDirectoryForNewVideos() {
        val directory = File(targetFolderPath)

        if (!directory.exists()) {
            Log.d(TAG, "폴더가 존재하지 않음: $targetFolderPath")
            // 폴더 생성 시도
            try {
                val created = directory.mkdirs()
                Log.d(TAG, "폴더 생성 시도 결과: $created")
            } catch (e: Exception) {
                Log.e(TAG, "폴더 생성 실패: ${e.message}")
            }
            return
        }

        val files = directory.listFiles()
        Log.d(TAG, "폴더 내 파일 수: ${files?.size ?: 0}")

        files?.forEach { file ->
            if (file.name.endsWith(".mp4", ignoreCase = true) && !processedFiles.contains(file.absolutePath)) {
                Log.d(TAG, "새 MP4 파일 발견 (직접 스캔): ${file.absolutePath}")
                // 분석 로직 호출
                analyzeVideoWithGPT(file.absolutePath)
                // 처리 목록에 추가
                processedFiles.add(file.absolutePath)
            }
        }
    }

    private fun checkIfNewVideoAdded(uri: Uri) {
        Log.d(TAG, "비디오 확인 시작 - URI: $uri")
        // 현재 시간 기록 (디버깅용)
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val timeThreshold = currentTimeSeconds - 60
        Log.d(TAG, "현재 시간(초): $currentTimeSeconds, 임계값: $timeThreshold")

        // 미디어 스토어에서 최근 추가된 비디오 확인
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media._ID
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

                    if (pathColumnIndex >= 0 && idColumnIndex >= 0) {
                        val filePath = cursor.getString(pathColumnIndex)
                        val id = cursor.getLong(idColumnIndex)

                        Log.d(TAG, "미디어 스토어 항목 발견: $filePath (ID: $id)")

                        // 특정 폴더에 있는 비디오인지 확인 및 중복 처리 방지
                        if (filePath.startsWith(targetFolderPath) && !processedFiles.contains(filePath)) {
                            Log.d(TAG, "블랙박스 영상 감지됨: $filePath")
                            processedFiles.add(filePath)

                            // GPT API에 영상 분석 요청
                            analyzeVideoWithGPT(filePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "새 비디오 확인 중 오류 발생", e)
        }
    }

    private fun analyzeVideoWithGPT(videoPath: String) {
        // 실제 API 호출은 구현해야 합니다
        // 이 예제에서는 비디오 경로만 로깅합니다
        Log.d(TAG, "GPT에 분석 요청할 비디오 경로: $videoPath")

        // TODO: OpenAI API를 호출하여 영상 분석
        // 예시 코드:
        val requestBody = FormBody.Builder()
            .add("video_path", videoPath)
            .build()

        val request = Request.Builder()
            .url("https://your-backend-server.com/analyze-video")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API 호출 실패", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "API 응답: $responseBody")

                try {
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    val isAccident = jsonResponse.optBoolean("is_accident", false)

                    if (isAccident) {
                        // 사고 영상인 경우, AI 서버로 전송
                        Log.d(TAG, "사고 영상으로 판단됨: $videoPath")
                        sendToAIServer(videoPath)
                    } else {
                        Log.d(TAG, "일반 영상으로 판단됨")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "응답 파싱 실패", e)
                }
            }
        })
    }

    private fun sendToAIServer(videoPath: String) {
        // AI 서버로 영상 전송 로직
        Log.d(TAG, "AI 서버로 전송: $videoPath")
        // TODO: 실제 업로드 로직 구현
    }
}