package com.crush.aiblackboxreview.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class OpenAIClient {
    private val TAG = "OpenAIClient"

    // 여기에 실제 OpenAI API 키를 입력하세요
    private val apiKey = "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"
    private val baseUrl = "https://api.openai.com/"
    private val service: OpenAIService

    init {
        val client = OkHttpClient.Builder().build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(OpenAIService::class.java)
    }

    // 비트맵을 Base64로 인코딩
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    // 이미지 분석 함수
    suspend fun analyzeTrafficAccident(frame: Bitmap): Boolean {
        return try {
            Log.d(TAG, "API 호출 시작 - 이미지 크기: ${frame.width}x${frame.height}")

            val base64Image = bitmapToBase64(frame)
            Log.d(TAG, "이미지 Base64 인코딩 완료 (길이: ${base64Image.length})")

            val imageUrl = "data:image/jpeg;base64,$base64Image"

            val request = ChatCompletionRequest(
                model = "gpt-4.1",
                messages = listOf(
                    // 시스템 역할 메시지 추가
                    Message(
                        role = "system",
                        content = listOf(
                            Content(
                                type = "text",
                                text = "당신은 차량 블랙박스 영상에서 차대차 충돌(교통사고)을 감지하는 AI입니다. 오직 사고 발생 여부만을 true 또는 false로 판단합니다."
                            )
                        )
                    ),
                    // 사용자 역할 메시지 (개선된 프롬프트)
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "text",
                                text = "다음 영상은 차량 블랙박스 또는 제3자의 카메라로 촬영된 교통 장면입니다.\n" +
                                        "이 영상이 **차량과 차량 사이의 충돌(교통사고)** 장면을 포함하는 경우, 다음 기준을 바탕으로 true/false로만 명확하게 판단해 주세요.\n" +
                                        "- 충돌은 앞, 뒤, 옆, 또는 모서리 등 어떤 각도에서든 발생할 수 있습니다.\n" +
                                        "- 근접 통과, 주행만 있는 장면은 false로 판단하세요.\n" +
                                        "- 차량 외의 대상(사람, 자전거, 구조물 등)과의 충돌은 false로 판단하세요.\n" +
                                        "- 제3자 관찰 시점의 영상도 포함됩니다.\n" +
                                        "- 차대차 충돌이 명확히 시각적으로 나타나는 경우에만 true로 응답해 주세요.\n" +
                                        "**답변은 오직 아래 중 하나로 해주세요:**\n" +
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

            // API 호출 시작 로그
            Log.d(TAG, "API 요청 생성 완료")
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "API 요청 시작 시간: $startTime")


            try {
                val response = service.analyzeImage("Bearer $apiKey", request)
                val endTime = System.currentTimeMillis()
                Log.d(TAG, "API 응답 완료 (소요 시간: ${endTime - startTime}ms)")

                val result = response.choices.firstOrNull()?.message?.content ?: "false"
                Log.d(TAG, "OpenAI 응답: $result")

                result.lowercase().contains("true")
            } catch (e: Exception) {
                Log.e(TAG, "API 호출 실패: ${e.javaClass.simpleName} - ${e.message}")
                if (e is retrofit2.HttpException) {
                    Log.e(TAG, "HTTP 에러 코드: ${e.code()}")
                    try {
                        Log.e(TAG, "에러 응답: ${e.response()?.errorBody()?.string()}")
                    } catch (e2: Exception) {
                        Log.e(TAG, "에러 응답 파싱 실패")
                    }
                }
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 분석 과정 에러: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            false
        }
    }
}