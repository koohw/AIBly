package com.crush.aiblackboxreview.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.crush.aiblackboxreview.services.VideoMonitoringService
import com.crush.aiblackboxreview.api.BackendApiClient
import java.io.File

class BootCompletedReceiver : BroadcastReceiver() {

    private val TAG = "BootCompletedReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "부팅 완료, 서비스 시작 전 인증 상태 확인")

            // 인증 상태 확인
            if (!BackendApiClient.isLoggedIn(context)) {
                Log.d(TAG, "사용자가 로그인하지 않은 상태입니다. 서비스를 시작하지 않습니다.")
                return
            }

            // 대상 폴더 존재 여부 확인 및 생성
            ensureTargetDirectoryExists()

            // 서비스 시작
            val serviceIntent = Intent(context, VideoMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "부팅 후 서비스 시작 요청 완료")
        }
    }

    private fun ensureTargetDirectoryExists() {
        try {
            val directory = File("/storage/emulated/0/DCIM")
            if (!directory.exists()) {
                val created = directory.mkdirs()
                Log.d(TAG, "타겟 디렉토리 생성 결과: $created")
            } else {
                Log.d(TAG, "타겟 디렉토리가 이미 존재함")
            }
        } catch (e: Exception) {
            Log.e(TAG, "디렉토리 생성 중 오류 발생", e)
        }
    }
}