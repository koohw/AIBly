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
    private val apiKey = "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"
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

    // 이미지 분석 함수
    suspend fun analyzeTrafficAccident(frame: Bitmap): Boolean {
        return try {
            // 전체 API 호출 과정에 타임아웃 적용
            withTimeout(API_TIMEOUT) {
                executeAnalysis(frame)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "API 호출 타임아웃 발생 (${API_TIMEOUT}ms 초과)")
            false
        } catch (e: Exception) {
            Log.e(TAG, "이미지 분석 과정 에러: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            false
        }
    }
    // 실제 API 호출 로직 분리
    private suspend fun executeAnalysis(frame: Bitmap): Boolean {
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
                                text = "You are an AI specialized in detecting car crashes from dashcam footage. Be liberal in your detection - if you see any sign that might indicate a collision, classify it as a crash (true). Respond only with 'true' or 'false'."
                            )
                        )
                    ),
                    // 사용자 역할 메시지
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "text",
                                text = "Analyze this dashcam frame and determine if it shows evidence of a car crash (vehicle-to-vehicle collision).\n\n" +
                                        "Consider these as signs of a crash (respond with 'true'):\n" +
                                        "• Any sudden camera movement, shaking, or jerking motion\n" +
                                        "• Visible impact with another vehicle\n" +
                                        "• Debris, broken parts, or glass on the road\n" +
                                        "• Airbag deployment\n" +
                                        "• Abnormal vehicle positioning or orientation\n" +
                                        "• Sudden stops or unexpected vehicle movements\n" +
                                        "• Any visible damage to vehicles\n\n" +

                                        "IMPORTANT: Due to dashcam limitations, you may not always see the other vehicle involved in the crash. Focus on indirect evidence such as camera shake, sudden motion changes, or impact reactions.\n\n" +

                                        "If you're unsure, err on the side of detecting a crash rather than missing one.\n\n" +

                                        "Respond with ONLY one of these answers:\n" +
                                        "- true\n" +
                                        "- false"
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
            val result = response.choices.firstOrNull()?.message?.content ?: "false"
            Log.d(TAG, "OpenAI 응답: $result")

            // 결과가 "true"를 포함하는지 확인
            return result.lowercase().contains("true")

        } catch (e: retrofit2.HttpException) {
            // HTTP 에러 자세히 로깅
            val code = e.code()
            Log.e(TAG, "HTTP 에러 발생 (코드: $code)")

            try {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(TAG, "에러 응답 내용: $errorBody")

                // 특정 에러 코드에 따른 처리
                when (code) {
                    429 -> Log.e(TAG, "비율 제한 초과 (Rate limit exceeded)")
                    500, 502, 503, 504 -> Log.e(TAG, "서버 오류 발생")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "에러 응답 파싱 실패")
            }
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "네트워크 타임아웃 발생")
            throw e
        } catch (e: java.io.IOException) {
            Log.e(TAG, "네트워크 I/O 에러 발생: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "예상치 못한 에러 발생: ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }
}