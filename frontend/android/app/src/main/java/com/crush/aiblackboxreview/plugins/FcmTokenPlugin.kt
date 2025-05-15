package com.crush.aiblackboxreview.plugins

import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.google.firebase.messaging.FirebaseMessaging
import com.crush.aiblackboxreview.api.BackendApiClient
import com.crush.aiblackboxreview.api.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "FcmToken")
class FcmTokenPlugin : Plugin() {

    companion object {
        private const val TAG = "FcmTokenPlugin"
    }

    @PluginMethod
    fun registerFcmToken(call: PluginCall) {
        val authToken = call.getString("authToken")

        if (authToken == null) {
            call.reject("인증 토큰이 필요합니다")
            return
        }

        Log.d(TAG, "FCM 토큰 등록 시작 - 인증 토큰: ${authToken.take(10)}...")

        // 인증 토큰 저장
        val sharedPref =
            context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("auth_token", authToken)
            apply()
        }

        // FCM 토큰 요청 및 서버 등록
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FCM 토큰 획득 실패", task.exception)
                call.reject("FCM 토큰 획득 실패: ${task.exception?.message}")
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM 토큰: $token")

            // FCM 토큰 저장
            val fcmPref =
                context.getSharedPreferences("fcm_prefs", android.content.Context.MODE_PRIVATE)
            with(fcmPref.edit()) {
                putString("fcm_token", token)
                apply()
            }

            // 서버에 등록 - Retrofit 클라이언트 사용
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // API 요청 객체 생성
                    val fcmTokenRequest = FcmTokenRequest(token)

                    // API 요청 보내기
                    val response = BackendApiClient.getFcmTokenService(context)
                        .registerFcmToken(fcmTokenRequest)

                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM 토큰 등록 성공: ${response.code()}")
                        fcmPref.edit().putBoolean("token_registered", true).apply()

                        // 성공 응답
                        val result = JSObject()
                        result.put("success", true)
                        result.put("message", "FCM 토큰 등록 성공")
                        CoroutineScope(Dispatchers.Main).launch {
                            call.resolve(result)
                        }
                    } else {
                        Log.e(
                            TAG,
                            "FCM 토큰 등록 실패: ${response.code()} - ${response.errorBody()?.string()}"
                        )
                        fcmPref.edit().putBoolean("token_registered", false).apply()

                        CoroutineScope(Dispatchers.Main).launch {
                            call.reject("FCM 토큰 등록 실패: ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    // EOFException 처리 추가
                    if (e is java.io.EOFException) {
                        Log.d(TAG, "FCM 토큰 등록 성공 (빈 응답)")
                        fcmPref.edit().putBoolean("token_registered", true).apply()

                        val result = JSObject()
                        result.put("success", true)
                        result.put("message", "FCM 토큰 등록 성공 (빈 응답)")
                        CoroutineScope(Dispatchers.Main).launch {
                            call.resolve(result)
                        }
                    } else {
                        // 다른 예외 처리 (이 부분이 누락되어 있음)
                        Log.e(TAG, "FCM 토큰 등록 에러", e)
                        CoroutineScope(Dispatchers.Main).launch {
                            call.reject("FCM 토큰 등록 에러: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}