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
        // SharedPreferences에서 인증 토큰 가져오기
        val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val authToken = authPrefs.getString("auth_token", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // API 호출 부분 확인
                val response = BackendApiClient.getFcmTokenService(context)
                    .registerFcmToken(
                        FcmTokenRequest(token) // 요청 객체 형식 확인
                    )

                if (response.isSuccessful) {
                    Log.d(TAG, "FCM 토큰 등록 성공: ${response.code()}")
                    saveFcmToken(token) // 성공 시 로컬에도 저장
                } else {
                    Log.e(TAG, "FCM 토큰 등록 실패: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM 토큰 등록 에러", e)
            }
        }
    }
}