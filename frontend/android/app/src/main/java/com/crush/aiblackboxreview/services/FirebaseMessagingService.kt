package com.crush.aiblackboxreview.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.crush.aiblackboxreview.notifications.ReportNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONException

class AiBlackboxReviewFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCMService"

    /**
     * FCM 토큰이 갱신될 때 호출됩니다.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM 토큰 갱신: $token")
        
        try {
            // 토큰을 SharedPreferences에 저장
            val preferences = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("fcm_token", token)
                .putBoolean("token_registered", false)
                .apply()
                
            Log.d(TAG, "FCM 토큰이 SharedPreferences에 저장됨")
        } catch (e: Exception) {
            Log.e(TAG, "FCM 토큰 저장 오류", e)
        }
    }

    /**
     * FCM 메시지가 수신될 때 호출됩니다.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "=================================================================")
        Log.d(TAG, "==== FCM 메시지 수신 시작 ====")
        Log.d(TAG, "FCM 메시지 수신: ${remoteMessage.data}")
        Log.d(TAG, "FCM 알림 정보: ${remoteMessage.notification}")
        Log.d(TAG, "FCM 메시지 ID: ${remoteMessage.messageId}")
        Log.d(TAG, "FCM 발신자: ${remoteMessage.senderId}")
        
        try {
            // FCM 메시지 상세 내용 로깅
            val reportId = remoteMessage.data["reportId"]
            
            Log.d(TAG, "==== FCM 메시지 상세 내용 ====")
            if (remoteMessage.notification != null) {
                val notificationTitle = remoteMessage.notification?.title ?: ""
                val notificationBody = remoteMessage.notification?.body ?: ""
                Log.d(TAG, "FCM Notification 타입 메시지: title=$notificationTitle, body=$notificationBody, reportId=$reportId")
                
                // 분석 완료 메시지인 경우 처리
                if (notificationTitle.contains("AI 분석") || notificationTitle.contains("분석 완료")) {
                    Log.e(TAG, "📊 분석 완료 메시지 수신: reportId=$reportId, title=$notificationTitle, body=$notificationBody")
                    
                    // 보고서 ID가 있으면 저장
                    reportId?.let {
                        saveNewReportId(it)
                    }
                    
                    // 알림 표시 권한 확인
                    if (checkNotificationPermission()) {
                        // 알림 표시
                        val notificationManager = ReportNotificationManager(applicationContext)
                        notificationManager.showReportNotification(
                            title = notificationTitle,
                            message = notificationBody,
                            reportId = reportId ?: "0"
                        )
                    } else {
                        Log.w(TAG, "알림 표시 권한이 없습니다.")
                    }
                }
            } else {
                Log.d(TAG, "FCM Data 타입 메시지: ${remoteMessage.data}")
                
                // 데이터 메시지 처리 (백그라운드에서도 작동)
                reportId?.let {
                    saveNewReportId(it)
                    
                    // 알림 표시 권한 확인 후 알림 생성
                    if (checkNotificationPermission()) {
                        val notificationManager = ReportNotificationManager(applicationContext)
                        notificationManager.showReportNotification(
                            title = "AI 분석 완료",
                            message = "보고서가 생성되었습니다.",
                            reportId = it
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "FCM 메시지 처리 중 오류 발생", e)
        }
        
        Log.d(TAG, "==== FCM 메시지 수신 완료 ====")
        Log.d(TAG, "=================================================================")
    }
    
    /**
     * 새 보고서 ID를 Preferences에 저장합니다.
     */
    private fun saveNewReportId(reportId: String) {
        try {
            Log.d(TAG, "==== saveNewReportId 호출: reportId=$reportId ====")
            
            val preferences = getSharedPreferences("capacitor_new_reports", Context.MODE_PRIVATE)
            val existingIdsStr = preferences.getString("NEW_REPORT_IDS", "[]") ?: "[]"
            Log.d(TAG, "기존 저장된 reportIds: $existingIdsStr")
            
            // JSON 배열로 파싱
            val reportIds = JSONArray(existingIdsStr)
            Log.d(TAG, "JSON 파싱 후 reportIds 목록: $reportIds")
            
            // 중복 확인
            var isDuplicate = false
            for (i in 0 until reportIds.length()) {
                if (reportIds.getString(i) == reportId) {
                    isDuplicate = true
                    break
                }
            }
            
            // 중복이 아니면 추가
            if (!isDuplicate) {
                Log.d(TAG, "새 reportId 추가: $reportId")
                reportIds.put(reportId)
                
                // 다시 문자열로 변환하여 저장
                val newIdsStr = reportIds.toString()
                preferences.edit().putString("NEW_REPORT_IDS", newIdsStr).apply()
                Log.d(TAG, "새 보고서 ID($reportId) 저장 완료: $newIdsStr")
                
                // MainActivity가 활성화되어 있으면 새 보고서 목록 갱신 브로드캐스트 전송
                val intent = Intent("com.crush.aiblackboxreview.NEW_REPORT")
                intent.putExtra("reportId", reportId)
                sendBroadcast(intent)
            } else {
                Log.d(TAG, "이미 존재하는 reportId: $reportId")
            }
            
            Log.d(TAG, "최종 저장된 reportIds: ${preferences.getString("NEW_REPORT_IDS", "[]")}")
            
        } catch (e: JSONException) {
            Log.e(TAG, "JSON 파싱 오류", e)
            
            // 오류 발생 시 새로 배열 생성
            try {
                val newArray = JSONArray()
                newArray.put(reportId)
                getSharedPreferences("capacitor_new_reports", Context.MODE_PRIVATE)
                    .edit()
                    .putString("NEW_REPORT_IDS", newArray.toString())
                    .apply()
                Log.d(TAG, "파싱 오류 후 새로 생성된 배열: $newArray")
            } catch (e2: Exception) {
                Log.e(TAG, "새 배열 저장 오류", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveNewReportId 오류", e)
        }
    }
    
    /**
     * 알림 표시 권한을 확인합니다.
     */
    private fun checkNotificationPermission(): Boolean {
        return NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }
}
