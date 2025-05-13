package com.crush.aiblackboxreview;

import android.app.Application;
import android.util.Log;

import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.common.util.Utility;

public class GlobalApplication extends Application {
    private static final String TAG = "GlobalApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 카카오 SDK 초기화
        String kakaoAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY;
        KakaoSdk.init(this, kakaoAppKey);
        
        // 현재 키 해시 로그로 출력 (개발용)
        try {
            String keyHash = Utility.INSTANCE.getKeyHash(this);
            Log.d(TAG, "Kakao Key Hash: " + keyHash);
        } catch (Exception e) {
            Log.e(TAG, "키 해시 가져오기 실패", e);
        }
    }
} 