//package com.crush.aiblackboxreview.services
//
//import android.util.Log
//import com.crush.aiblackboxreview.notifications.ReportNotificationManager
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    companion object {
//        private const val TAG = "FCMService"
//    }
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        Log.d(TAG, "FCM 메시지 수신: ${remoteMessage.data}")
//
//        // 데이터 메시지 처리
//        val data = remoteMessage.data
//        if (data.isNotEmpty()) {
//            when (data["type"]) {
//                "analysis_started" -> {
//                    // 분석 중 알림 표시
//                    val videoId = data["video_id"] ?: return
//                    val notificationManager = ReportNotificationManager(applicationContext)
//                    notificationManager.showAnalysisInProgressNotification(videoId)
//                }
//                "analysis_complete" -> {
//                    // 분석 중 알림 제거
//                    val notificationManager = ReportNotificationManager(applicationContext)
//                    notificationManager.cancelAnalysisInProgressNotification()
//
//                    // 분석 완료 알림 표시
//                    val reportId = data["report_id"] ?: return
//                    val title = data["title"] ?: "사고 분석 완료"
//                    val message = data["message"] ?: "사고 영상 분석이 완료되었습니다."
//                    notificationManager.showReportNotification(title, message, reportId)
//                }
//            }
//        }
//
//        // 알림 메시지 처리 (선택사항)
//        remoteMessage.notification?.let {
//            Log.d(TAG, "메시지 알림 본문: ${it.body}")
//        }
//    }
//
//    override fun onNewToken(token: String) {
//        Log.d(TAG, "새 FCM 토큰: $token")
//        // 토큰을 서버에 전송하는 코드 추가
//        sendRegistrationToServer(token)
//    }
//
//    private fun sendRegistrationToServer(token: String) {
//        // 실제 구현에서는 이 토큰을 백엔드 서버에 전송하여
//        // 특정 기기에 푸시 알림을 보낼 수 있게 함
//        Log.d(TAG, "FCM 토큰을 서버에 전송 (구현 필요): $token")
//    }
//}