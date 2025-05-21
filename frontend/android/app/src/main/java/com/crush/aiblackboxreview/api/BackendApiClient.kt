package com.crush.aiblackboxreview.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log
import android.widget.Toast

/**
 * 백엔드 API 서비스에 접근하기 위한 Retrofit 클라이언트
 * 싱글톤 객체로 구현되어 앱 전체에서 공유됩니다.
 */
object BackendApiClient {
    // 백엔드 서버 기본 URL (실제 서버 URL로 변경 필요)
    private const val BASE_URL = "https://k12e203.p.ssafy.io/"


    // BackendApiClient.kt - getAuthToken 메서드 수정
    fun getAuthToken(context: Context): String? {
        // 두 저장소에서 토큰을 찾음

        // 1. Capacitor 저장소 확인
        val capacitorPref = context.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE)
        val capacitorToken = capacitorPref.getString("AUTH_TOKEN", null)

        // 2. 네이티브 저장소 확인
        val nativePref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val nativeToken = nativePref.getString("auth_token", null)

        // 디버깅을 위한 로그 추가
        if (capacitorToken != null) {
            Log.d("TokenDebug", "Capacitor에서 저장한 토큰을 찾음: ${capacitorToken.substring(0, Math.min(capacitorToken.length, 20))}...")
        }

        if (nativeToken != null) {
            Log.d("TokenDebug", "네이티브에서 저장한 토큰을 찾음: ${nativeToken.substring(0, Math.min(nativeToken.length, 20))}...")
        }

        // Capacitor 토큰 우선, 없으면 네이티브 토큰, 모두 없으면 기본 토큰
        val selectedToken = capacitorToken ?: nativeToken

        if (selectedToken != null) {
            Log.d("TokenDebug", "선택된 인증 토큰: ${selectedToken.substring(0, Math.min(selectedToken.length, 20))}...")
            return selectedToken
        } else {
            // 토큰이 없는 경우 처리
            Log.e("TokenDebug", "인증 토큰을 찾을 수 없음")
            throw Exception("인증 토큰을 찾을 수 없습니다. 로그인이 필요합니다.")
        }
    }
    // 동적 인증 인터셉터 생성
    private fun createAuthInterceptor(authToken: String?): Interceptor {
        return Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            // 토큰이 있을 때만 Authorization 헤더 추가
            if (authToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer $authToken")
                Log.d("TokenDebug", "요청에 인증 토큰이 포함되었습니다.")
            } else {
                Log.w("TokenDebug", "요청에 인증 토큰이 포함되지 않았습니다. 인증이 필요한 API는 실패할 수 있습니다.")
            }

            chain.proceed(requestBuilder.build())
        }
    }
    /**
     * API 요청을 위한 OkHttp 클라이언트 설정
     * 대용량 영상 파일 업로드를 고려하여 타임아웃 시간을 넉넉하게 설정
     */
    private fun createOkHttpClient(authToken: String?): OkHttpClient {
        // 로깅 인터셉터 추가
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 바디 내용 포함 로깅
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃
            .readTimeout(2, TimeUnit.MINUTES)     // 읽기 타임아웃
            .writeTimeout(90, TimeUnit.SECONDS)    // 쓰기 타임아웃 (업로드에 더 많은 시간 필요)
            .addInterceptor(createAuthInterceptor(authToken))
            .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
            .build()
    }

    /**
     * Retrofit 인스턴스 생성
     * 동적으로 인증 토큰이 적용된 OkHttpClient 사용
     */
    private fun createRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    /**
     * 백엔드 API 서비스 인스턴스
     * 앱 전체에서 이 인스턴스를 통해 API 호출 가능
     */
    fun getBackendApiService(context: Context): BackendApiService {
        val authToken = getAuthToken(context)
        val client = createOkHttpClient(authToken)
        val retrofit = createRetrofit(client)
        return retrofit.create(BackendApiService::class.java)
    }
    /**
     * FCM 토큰 API 서비스 인스턴스 생성
     * 컨텍스트를 통해 현재 인증 토큰을 사용
     */
    fun getFcmTokenService(context: Context): FcmTokenApiService {
        val authToken = getAuthToken(context)
        val client = createOkHttpClient(authToken)
        val retrofit = createRetrofit(client)
        return retrofit.create(FcmTokenApiService::class.java)
    }

    /**
     * 로그인 상태 확인
     * 토큰이 있는지 확인하여 로그인 상태를 반환합니다.
     */
    fun isLoggedIn(context: Context): Boolean {
        return getAuthToken(context) != null
    }
}