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
        Log.d(TAG, "FCM 알림 정보: ${remoteMessage.notification}")

        try {
            // 1. notification 객체가 있는지 확인
            if (remoteMessage.notification != null) {
                val title = remoteMessage.notification?.title ?: "AI 분석 완료"
                val body = remoteMessage.notification?.body ?: "블랙박스 영상 분석이 완료되었습니다."

                // 2. data에서 reportId 확인
                val reportId = remoteMessage.data["reportId"]

                if (reportId != null) {
                    Log.e(TAG, "📊 분석 완료 메시지 수신: reportId=$reportId, title=$title, body=$body")

                    // 3. 알림 생성
                    val notificationManager = ReportNotificationManager(applicationContext)
                    notificationManager.showReportNotification(title, body, reportId)
                } else {
                    Log.e(TAG, "⚠️ reportId가 없는 FCM 메시지 수신됨")
                }
            } else {
                // notification 객체가 없는 경우 (data-only 메시지)
                val reportId = remoteMessage.data["reportId"]
                val title = remoteMessage.data["title"] ?: "AI 분석 완료"
                val body = remoteMessage.data["body"] ?: "블랙박스 영상 분석이 완료되었습니다."

                if (reportId != null) {
                    Log.e(TAG, "📊 데이터 전용 분석 완료 메시지 수신: reportId=$reportId")

                    val notificationManager = ReportNotificationManager(applicationContext)
                    notificationManager.showReportNotification(title, body, reportId)
                } else {
                    // 기존 로직은 그대로 유지 (type 기반 처리)
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
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FCM 메시지 처리 중 오류 발생", e)
        }
    }
    override fun onNewToken(token: String) {
        Log.e(TAG, "🆕🆕🆕 새 FCM 토큰 발급: $token 🆕🆕🆕")

        // 토큰만 저장하고, 서버 등록은 로그인 후에만 수행되도록 함
        saveFcmTokenLocally(token);
    }

    private fun saveFcmTokenLocally(token: String) {
        // FCM 토큰 저장
        val sharedPref = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fcm_token", token)
            putBoolean("token_registered", false) // 아직 등록되지 않음을 표시
            apply()
        }

        Log.e(TAG, "💾 FCM 토큰이 로컬에 저장됨: ${token.substring(0, 20)}...")
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
