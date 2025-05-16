package com.crush.aiblackboxreview.services

import android.util.Log
import com.crush.aiblackboxreview.api.BackendApiClient
import com.crush.aiblackboxreview.api.FcmTokenRequest
import com.crush.aiblackboxreview.notifications.ReportNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM 메시지 수신: ${remoteMessage.data}")

        // 데이터 메시지 처리
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            when (data["type"]) {
                "analysis_started" -> {
                    // 분석 중 알림 표시
                    val videoId = data["video_id"] ?: return
                    val notificationManager = ReportNotificationManager(applicationContext)
                    notificationManager.showAnalysisInProgressNotification(videoId)
                }

                "analysis_complete" -> {
                    // 분석 중 알림 제거
                    val notificationManager = ReportNotificationManager(applicationContext)
                    notificationManager.cancelAnalysisInProgressNotification()

                    // 분석 완료 알림 표시
                    val reportId = data["report_id"] ?: return
                    val title = data["title"] ?: "사고 분석 완료"
                    val message = data["message"] ?: "사고 영상 분석이 완료되었습니다."
                    notificationManager.showReportNotification(title, message, reportId)
                }
            }
        }

        // 알림 메시지 처리 (선택사항)
        remoteMessage.notification?.let {
            Log.d(TAG, "메시지 알림 본문: ${it.body}")
        }
    }
    override fun onNewToken(token: String) {
        Log.e(TAG, "🆕🆕🆕 새 FCM 토큰 발급: $token 🆕🆕🆕")

        // 토큰을 서버에 전송
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // FCM 토큰 저장
        val sharedPref = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fcm_token", token)
            apply()
        }

        // 인증 토큰 확인
        val authPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        if (authPref.contains("auth_token")) {
            val authToken = authPref.getString("auth_token", null) ?: return
            Log.e(TAG, "🔄🔄🔄 자동으로 FCM 토큰 서버 등록 시도 🔄🔄🔄")

            // 인증된 상태면 서버에 등록 시도
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = BackendApiClient.getFcmTokenService(applicationContext)
                        .registerFcmToken(FcmTokenRequest(token))

                    if (response.isSuccessful) {
                        Log.e(TAG, "✅✅✅ 자동 FCM 토큰 서버 등록 성공: ${response.code()} ✅✅✅")
                        // 등록 성공 상태 저장
                        sharedPref.edit().putBoolean("token_registered", true).apply()
                        // 현재 시간 저장
                        val currentTime = System.currentTimeMillis()
                        sharedPref.edit().putLong("token_registration_time", currentTime).apply()
                    } else {
                        Log.e(
                            TAG,
                            "❌❌❌ 자동 FCM 토큰 서버 등록 실패: ${response.code()} - ${response.errorBody()?.string()} ❌❌❌"
                        )
                        sharedPref.edit().putBoolean("token_registered", false).apply()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌❌❌ 자동 FCM 토큰 서버 등록 에러 ❌❌❌", e)
                    sharedPref.edit().putBoolean("token_registered", false).apply()
                }
            }
        } else {
            Log.e(TAG, "⚠️⚠️⚠️ 인증 토큰이 없습니다. 로그인 후 FCM 토큰이 등록됩니다. ⚠️⚠️⚠️")
            sharedPref.edit().putBoolean("token_registered", false).apply()
        }
    }
}
