package com.crush.aiblackboxreview.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class OpenAIClient {
    private val TAG = "OpenAIClient"

    // 여기에 실제 OpenAI API 키를 입력하세요
    private val apiKey = "enter_your_api_key"
    private val baseUrl = "https://api.openai.com/"
    private val service: OpenAIService

    // 타임아웃 상수
    private val CONNECTION_TIMEOUT = 10L // 연결 타임아웃: 10초
    private val READ_TIMEOUT = 30L       // 읽기 타임아웃: 30초
    private val WRITE_TIMEOUT = 30L      // 쓰기 타임아웃: 30초
    private val API_TIMEOUT = 60000L     // API 전체 타임아웃: 60초

    init {
        // OkHttpClient에 타임아웃 설정 추가
        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(OpenAIService::class.java)
    }

    // 비트맵을 Base64로 효율적으로 인코딩 (IO 디스패처로 이동)
    private suspend fun bitmapToBase64(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        ByteArrayOutputStream().use { outputStream ->
            // 이미지 크기가 큰 경우 리사이징 고려
            val resizedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                val scale = 1024f / Math.max(bitmap.width, bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }
            // 압축률 최적화 (70%는 적절한 값)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            // 원본과 다른 경우에만 리사이징된 비트맵 해제
            if (resizedBitmap != bitmap && !resizedBitmap.isRecycled) {
                resizedBitmap.recycle()
            }

            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP) // NO_WRAP 플래그로 개행 문자 제거
        }
    }

    // analyzeTrafficAccident 함수 수정 - 반환 타입 변경
    suspend fun analyzeTrafficAccident(frame: Bitmap): Pair<Boolean, Int> {
        return try {
            // 전체 API 호출 과정에 타임아웃 적용
            withTimeout(API_TIMEOUT) {
                executeAnalysis(frame)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "API 호출 타임아웃 발생 (${API_TIMEOUT}ms 초과)")
            Pair(false, 0) // 실패 시 사고 아님(false)과 위치 0 반환
        } catch (e: Exception) {
            Log.e(TAG, "이미지 분석 과정 에러: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            Pair(false, 0) // 실패 시 사고 아님(false)과 위치 0 반환
        }
    }
    // 실제 API 호출 로직 분리
    private suspend fun executeAnalysis(frame: Bitmap): Pair<Boolean, Int> {
        try {
            Log.d(TAG, "API 호출 준비 - 이미지 크기: ${frame.width}x${frame.height}")

            val base64Image = bitmapToBase64(frame)
            Log.d(TAG, "이미지 Base64 인코딩 완료 (길이: ${base64Image.length})")

            val imageUrl = "data:image/jpeg;base64,$base64Image"

            val request = ChatCompletionRequest(
                model = "gpt-4.1",
                messages = listOf(
                    // 시스템 역할 메시지
                    Message(
                        role = "system",
                        content = listOf(
                            Content(
                                type = "text",
                                text = "You are an AI specialized in detecting car crashes from dashcam footage and classifying the location type. Be liberal in your detection - if you see any sign that might indicate a collision, classify it as a crash."
                            )
                        )
                    ),
                    // 사용자 역할 메시지
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "text",
                                text = """
Analyze this dashcam frame and determine:
1. If it shows evidence of a vehicle-to-vehicle crash
2. The type of location where the incident occurred

Part 1: Crash Detection
Return "CRASH: true" if you see:
- Any signs of impact, camera shake, or abnormal movement
- Reactionary motion from the vehicle (e.g., sudden lurching or angle change)
- Signs of collision even without seeing the other vehicle (especially from the side or rear)
- Obscured view due to potential impact

Return "CRASH: false" only if the image clearly shows normal, uninterrupted driving with no anomalies.

Part 2: Location Classification
If a crash is detected, classify the location as ONE of the following categories:
- LOCATION: 1 (for crashes on straight road sections)
- LOCATION: 2 (for crashes at T-shaped intersections)
- LOCATION: 3 (for crashes in parking areas)

Format your response exactly as:
CRASH: true/false
LOCATION: 1/2/3

IMPORTANT: If uncertain about crash, lean towards saying "CRASH: true". If you cannot determine the location type, choose the most likely option based on visual cues.
""".trimIndent()
                            ),
                            Content(
                                type = "image_url",
                                image_url = ImageUrl(imageUrl)
                            )
                        )
                    )
                )
            )

            Log.d(TAG, "API 요청 생성 완료")
            val startTime = System.currentTimeMillis()

            // API 호출 부분
            val response = withContext(Dispatchers.IO) {
                service.analyzeImage("Bearer $apiKey", request)
            }

            val endTime = System.currentTimeMillis()
            val elapsedTime = endTime - startTime
            Log.d(TAG, "API 응답 완료 (소요 시간: ${elapsedTime}ms)")

            // 메모리 정리 힌트
            Runtime.getRuntime().gc()

            // 응답 처리
            val content = response.choices.firstOrNull()?.message?.content ?: "CRASH: false\nLOCATION: 1"
            Log.d(TAG, "OpenAI 응답: $content")

            // 결과 파싱
            val lines = content.split("\n")
            val isCrash = lines.firstOrNull { it.startsWith("CRASH:") }?.contains("true") ?: false

            // 위치 코드 추출 (1, 2, 3)
            val locationCode = if (isCrash) {
                lines.firstOrNull { it.startsWith("LOCATION:") }
                    ?.replace("LOCATION:", "")
                    ?.trim()
                    ?.toIntOrNull() ?: 1
            } else {
                0 // 사고 아닌 경우 0 (처리 안함)
            }

            return Pair(isCrash, locationCode)

        } catch (e: Exception) {
            // 예외 처리 로직
            Log.e(TAG, "예상치 못한 에러 발생: ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }

}