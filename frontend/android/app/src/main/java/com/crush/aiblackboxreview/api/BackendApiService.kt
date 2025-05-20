package com.crush.aiblackboxreview.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path


/**
 * 백엔드 서버 API와의 통신을 위한 Retrofit 인터페이스
 * 서버에서 제공하는 API 엔드포인트를 정의합니다.
 */

interface BackendApiService {
    /**
     * S3 Presigned URL을 요청하는 API 엔드포인트
     *
     * @param request Presigned URL 요청 정보 (파일명, 컨텐츠 타입)
     * @return Presigned URL 및 S3 키 정보가 포함된 응답
     */
    @POST("api/s3/presigned")
    suspend fun getPresignedUrl(@Body request: PresignedUrlRequest): Response<PresignedUrlResponse>

    /**
     * 업로드 완료를 알리는 API 엔드포인트
     *
     * @param request 업로드 완료 알림 요청 (S3 키)
     * @return 성공/실패 정보가 포함된 응답
     */
    @POST("api/videos/upload-notify/auto")
    suspend fun notifyUploadComplete(@Body request: UploadCompleteRequest): Response<UploadCompleteResponse>

    @GET("api/reports/{reportId}")
    suspend fun getReportDetails(@Path("reportId") reportId: String): Response<ReportResponse>

}

/**
 * Presigned URL 요청을 위한 데이터 모델
 *
 * @property fileName 업로드할 파일 이름 (예: 'accident_1234567890.mp4')
 * @property contentType 파일의 MIME 타입 (예: 'video/mp4')
 * @property locationType 위치 타입 (1: 직선도로, 2: T자형교차로, 3: 주차장)
 */
data class PresignedUrlRequest(
    val fileName: String,
    val contentType: String,
    val locationType: Int  // 위치 타입 파라미터 추가
)

/**
 * Presigned URL 응답을 위한 데이터 모델
 *
 * @property url S3에 파일을 업로드할 수 있는 Presigned URL
 * @property s3Key S3에 저장된 파일의 고유 키 (파일 경로)
 */
data class PresignedUrlResponse(
    val presignedUrl: String, // 'url'에서 'presignedUrl'로 변경
    val s3Key: String
)

// 오류 응답을 위한 데이터 모델 추가
data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: String
)

/**
 * 업로드 완료 알림을 위한 데이터 모델
 *
 * @property s3Key S3에 저장된 파일의 고유 키
 */
data class UploadCompleteRequest(
    val fileName: String,
    val s3Key: String,
    val contentType: String = "video/mp4",
    val size: Long
)

/**
 * 업로드 완료 알림 응답을 위한 데이터 모델
 *
 * @property success 업로드 완료 알림 처리 성공 여부
 * @property message 성공/실패 관련 메시지 (선택 사항)
 * @property timestamp 서버 처리 시간 (선택 사항)
 */
data class UploadCompleteResponse(
    val videoId: Int,               // 변경: fileId에서 videoId로
    val fileType: String,           // 유지
    val analysisStatus: String
)

// 보고서 응답 모델 (같은 파일 내에 추가)
data class ReportResponse(
    val reportId: String,
    val videoId: String,
    val accidentDate: String,
    val severity: String,
    val description: String,
    // 기타 필요한 필드
)