package com.crush.aiblackboxreview.plugins

import android.content.Context
import android.content.Intent
import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "AutoDetect") // 이름을 "AutoDetect"로 수정
class AutoDetectPlugin : Plugin() {
    private val TAG = "AutoDetectPlugin"

    @PluginMethod
    fun updateSetting(call: PluginCall) {
        val enabled = call.getBoolean("enabled", true) ?: true

        // 설정 값을 SharedPreferences에 저장
        val prefs = context.getSharedPreferences("AutoDetectSettings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("autoDetectEnabled", enabled).apply()

        // 브로드캐스트 인텐트를 통해 서비스에 설정 변경 알림
        val intent = Intent("com.crush.aiblackboxreview.AUTO_DETECT_SETTING_CHANGED")
        intent.putExtra("enabled", enabled)
        context.sendBroadcast(intent)

        // 결과 반환
        val ret = JSObject()
        ret.put("success", true)
        call.resolve(ret)
    }

    @PluginMethod
    fun getStatus(call: PluginCall) {
        // 현재 설정 상태 조회
        val prefs = context.getSharedPreferences("AutoDetectSettings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("autoDetectEnabled", true) // 기본값은 true

        val ret = JSObject()
        ret.put("enabled", enabled)
        call.resolve(ret)
    }
}