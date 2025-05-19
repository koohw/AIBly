package com.crush.aiblackboxreview.managers

import android.content.Context
import android.util.Log
import com.crush.aiblackboxreview.api.BackendApiClient
import com.crush.aiblackboxreview.api.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmTokenManager(private val context: Context) {
    companion object {
        private const val TAG = "FcmTokenManager"
        private const val PREF_NAME = "fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    // 토큰 저장
    fun saveFcmToken(token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    // 저장된 토큰 가져오기
    fun getFcmToken(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    // 서버에 토큰 등록
    fun registerTokenToServer(token: String) {
        Log.e(TAG, "🚀 FCM 토큰 등록 시작: ${token.substring(0, 20)}...")


        // 두 곳에서 인증 토큰 확인 시도
        val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        var authToken = authPrefs.getString("auth_token", null)

        if (authToken != null) {
            Log.e(TAG, "🔑 네이티브 저장소에서 인증 토큰 찾음: ${authToken.substring(0, 10)}...")
        } else {
            // Capacitor 저장소 확인
            val capacitorPrefs =
                context.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE)
            authToken = capacitorPrefs.getString("auth_token", null)

            if (authToken != null) {
                Log.e(TAG, "🔑 Capacitor 저장소에서 인증 토큰 찾음: ${authToken.substring(0, 10)}...")
            } else {
                Log.e(TAG, "❌ 인증 토큰을 찾을 수 없음. 로그인 필요")
                return  // 토큰이 없으면 등록 중단
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e(TAG, "🌐 FCM 토큰 서버 등록 요청 시작...")

                // API 호출
                val response = BackendApiClient.getFcmTokenService(context)
                    .registerFcmToken(FcmTokenRequest(token))

                if (response.isSuccessful) {
                    Log.e(TAG, "✅ FCM 토큰 서버 등록 성공: ${response.code()}")

                    // FCM 토큰 등록 상태 저장 (두 저장소 모두에)
                    val fcmPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    fcmPrefs.edit()
                        .putString(KEY_FCM_TOKEN, token)
                        .putBoolean("token_registered", true)
                        .putLong("token_registration_time", System.currentTimeMillis())
                        .apply()

                    // Capacitor 저장소에도 등록 상태 저장
                    val capacitorPrefs =
                        context.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE)
                    capacitorPrefs.edit().putString("fcm_token_registered", "true").apply()

                    Log.e(TAG, "💾 FCM 토큰 등록 상태 저장 완료")
                } else {
                    Log.e(
                        TAG,
                        "❌ FCM 토큰 서버 등록 실패: ${response.code()} - ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FCM 토큰 등록 에러", e)
                e.printStackTrace()
            }
        }
    }
}
