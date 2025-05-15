package com.crush.aiblackboxreview.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
    private const val AUTH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrYWthbzo0MjUxNzkzNzA2IiwidXNlcklkIjoxLCJpYXQiOjE3NDcxODcxNzcsImV4cCI6MTc0NzI3MzU3N30.-FCP7rgelIMae8QR5I67Pb0OwHQK8HAIpMNRz9NnW9k"

    // 인증 인터셉터 추가
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $AUTH_TOKEN")
            .build()
        chain.proceed(request)
    }

    /**
     * API 요청을 위한 OkHttp 클라이언트 설정
     * 대용량 영상 파일 업로드를 고려하여 타임아웃 시간을 넉넉하게 설정
     */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃
        .readTimeout(2, TimeUnit.MINUTES)     // 읽기 타임아웃
        .writeTimeout(90, TimeUnit.SECONDS)    // 쓰기 타임아웃 (업로드에 더 많은 시간 필요)
        .addInterceptor(authInterceptor)
        .build()

    /**
     * Retrofit 인스턴스 설정
     * - 기본 URL 설정
     * - OkHttp 클라이언트 설정
     * - JSON 변환을 위한 Gson 컨버터 설정
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * 백엔드 API 서비스 인스턴스
     * 앱 전체에서 이 인스턴스를 통해 API 호출 가능
     */
    val backendApiService: BackendApiService = retrofit.create(BackendApiService::class.java)
}