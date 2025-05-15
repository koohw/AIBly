package com.crush.aiblackboxreview.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * FCM 토큰 등록 요청 데이터 클래스
 */
data class FcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String
)

/**
 * FCM 토큰 등록 응답 데이터 클래스
 */
data class FcmTokenResponse(
    val message: String? = null // null 허용으로 변경
)

/**
 * FCM 토큰 등록을 위한 API 인터페이스
 */
interface FcmTokenApiService {
    // 슬래시 위치 확인 - BASE_URL에 슬래시가 있다면 여기서는 제거
    @POST("api/user/fcm-token") // 앞의 슬래시 제거해보기
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequest
    ): Response<Void>
}