package com.crush.aiblackboxreview

import com.crush.aiblackboxreview.BuildConfig

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Native App Key를 꼭 여기에 넣어주세요.
    val key = BuildConfig.KAKAO_NATIVE_APP_KEY
    KakaoSdk.init(this, key)
  }
}
