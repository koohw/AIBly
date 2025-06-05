package com.crush.aiblackboxreview.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.crush.aiblackboxreview.MainActivity
import com.crush.aiblackboxreview.R

/**
 * 보고서 알림을 관리하는 클래스
 * - 분석 보고서 준비 완료 알림 표시
 */
class ReportNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "ReportNotification"
        private const val CHANNEL_ID = "report_notification_channel"
        private const val NOTIFICATION_ID = 2000 // 완료 알림용
        private const val NOTIFICATION_ID_PROGRESS = 2001 // 진행 중 알림용
    }

    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상 필수)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "사고 분석 보고서"
            val description = "사고 영상 분석 보고서가 준비되었을 때 알림"
            val importance = NotificationManager.IMPORTANCE_HIGH // 헤드업 알림 표시

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // 알림 매니저에 채널 등록
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "알림 채널 생성 완료: $CHANNEL_ID")

            // 채널 설정 확인
            val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Log.e(TAG, "채널 중요도: ${createdChannel.importance}")
            Log.e(TAG, "채널 설명: ${createdChannel.description}")
        } else {
            Log.e(TAG, "Android 8.0 미만이므로 알림 채널 생성 생략")
        }
    }

    /**
     * 알림에 사용할 앱 아이콘 리소스 ID를 반환합니다.
     */
    private fun getAppIcon(): Int {
        return R.drawable.ic_launcher_foreground
    }

    /**
     * 보고서 알림 표시
     *
     * @param title 알림 제목
     * @param message 알림 메시지
     * @param reportId 보고서 ID (인텐트에 포함)
     */
    fun showReportNotification(title: String, message: String, reportId: String) {
        Log.d(TAG, "보고서 알림 표시: $title, reportId=$reportId")

        // 알림음 설정
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // 인텐트 설정 (알림 클릭 시 액티비티 열기)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("REPORT_ID", reportId)
            putExtra("FROM_NOTIFICATION", true)
            putExtra("DIRECT_NAVIGATE", true)
            putExtra("TARGET_URL", "/analysis?id=$reportId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.hashCode(), // 각 보고서별 고유 requestCode 사용
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌더 생성
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 앱 아이콘 사용
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // 긴 텍스트 지원
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 높은 우선순위
            .setContentIntent(pendingIntent)
            .setSound(defaultSoundUri)
            .setAutoCancel(true) // 클릭 시 알림 자동 제거
            .setVibrate(longArrayOf(0, 500, 200, 500))

        // 알림 표시
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        try {
            notificationManager.notify(reportId.hashCode(), notificationBuilder.build())
            Log.d(TAG, "알림 표시 완료")
        } catch (e: Exception) {
            Log.e(TAG, "알림 표시 중 오류 발생", e)
        }
    }

    /**
     * 보고서 준비 완료 알림을 표시합니다.
     */
    fun showReportReadyNotification(reportId: Int, title: String, body: String) {
        // 알림 채널 생성
        createNotificationChannel()
        
        Log.d(TAG, "보고서 알림 생성: reportId=$reportId, title=$title")
        
        // 인텐트 설정 (알림 클릭 시 액티비티 열기)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("REPORT_ID", reportId.toString())
            putExtra("FROM_NOTIFICATION", true)
            putExtra("DIRECT_NAVIGATE", true)
            putExtra("TARGET_URL", "/analysis?id=$reportId")
        }

        // PendingIntent 생성 - 각 reportId에 대해 고유한 requestCode 사용
        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.hashCode(), // 각 보고서별 고유 requestCode 사용
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 생성
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(getAppIcon())
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // 알림 표시
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(reportId, builder.build())
            Log.d(TAG, "알림 표시 완료")
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 표시 권한 없음", e)
        } catch (e: Exception) {
            Log.e(TAG, "알림 표시 중 오류 발생", e)
        }
    }

    /**
     * 분석 중 알림 표시
     *
     * @param videoId 분석 중인 영상 ID 또는 파일명
     */
    fun showAnalysisInProgressNotification(videoId: String) {
        Log.d(TAG, "분석 중 알림 표시: videoId=$videoId")

        // 인텐트 설정 (알림 클릭 시 액티비티 열기)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("VIDEO_ID", videoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌더 생성
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("교통사고 분석 중...")
            .setContentText("영상을 분석하고 있습니다. 완료되면 알려드립니다.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 기본 우선순위 (헤드업 아님)
            .setOngoing(true) // 사용자가 스와이프로 제거 불가능
            .setProgress(0, 0, true) // 진행 중 표시 (불확정 프로그레스바)
            .setContentIntent(pendingIntent)

        // 알림 표시 (고정 ID 사용 - 나중에 업데이트 가능)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notificationBuilder.build())

        Log.d(TAG, "분석 중 알림 표시 완료")
    }

    /**
     * 분석 중 알림 제거
     * 분석이 완료되었거나 취소되었을 때 호출
     */
    fun cancelAnalysisInProgressNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)

        Log.d(TAG, "분석 중 알림 제거 완료")
    }
}