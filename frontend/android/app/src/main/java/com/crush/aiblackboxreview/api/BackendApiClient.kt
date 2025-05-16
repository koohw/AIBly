package com.crush.aiblackboxreview.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 백엔드 API 서비스에 접근하기 위한 Retrofit 클라이언트
 * 싱글톤 객체로 구현되어 앱 전체에서 공유됩니다.
 */
object BackendApiClient {
    // 백엔드 서버 기본 URL (실제 서버 URL로 변경 필요)
    private const val BASE_URL = "https://k12e203.p.ssafy.io/"

    // 테스트용 임시 토큰 (실제 배포 전에 반드시 변경 필요)
    private const val AUTH_TOKEN =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrYWthbzo0MjUxNzkzNzA2IiwidXNlcklkIjoxMSwiaWF0IjoxNzQ3Mjg5NzkwLCJleHAiOjE3NDczNzYxOTB9.FJ8gEd7DF1mqB4KpzgavwJzLt7vdha4ni1yugowe8JU"

    // 인증 토큰을 동적으로 가져오기 위한 함수
    private fun getAuthToken(context: Context): String {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", AUTH_TOKEN) ?: AUTH_TOKEN
    }

    // 동적 인증 인터셉터 생성
    private fun createAuthInterceptor(authToken: String): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json") // Content-Type 헤더 추가
                .addHeader("Accept", "application/json") // Accept 헤더 추가
                .build()
            chain.proceed(request)
        }
    }
    /**
     * API 요청을 위한 OkHttp 클라이언트 설정
     * 대용량 영상 파일 업로드를 고려하여 타임아웃 시간을 넉넉하게 설정
     */
    private fun createOkHttpClient(authToken: String): OkHttpClient {
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

    // 기본 인증 토큰을 사용한 정적 Retrofit 인스턴스
    private val defaultRetrofit = createRetrofit(createOkHttpClient(AUTH_TOKEN))

    /**
     * 백엔드 API 서비스 인스턴스
     * 앱 전체에서 이 인스턴스를 통해 API 호출 가능
     */
    val backendApiService: BackendApiService = defaultRetrofit.create(BackendApiService::class.java)

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
}