package com.crush.aiblackboxreview.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.TimeUnit
import com.crush.aiblackboxreview.api.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.security.MessageDigest
import com.google.gson.Gson
import kotlin.math.pow

/**
 * 사고 영상 업로드를 관리하는 클래스
 * - Presigned URL 요청
 * - S3 업로드
 * - 업로드 완료 알림
 * - 알림(Notification) 관리
 *
 * @param context 애플리케이션 컨텍스트 (알림 표시용)
 * @param backendApiService 백엔드 API 서비스 인스턴스 (기본값: BackendApiClient.backendApiService)
 */
class UploadManager(
    private val context: Context,
    private val backendApiService: BackendApiService = BackendApiClient.getBackendApiService(context)
) {
    // 코루틴 스코프 - IO 스레드에서 네트워크 작업 수행
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 알림 관리자 및 알림 ID
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 1001

    /**
     * 초기화 블록 - 알림 채널 생성
     */
    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상 필수)
     * 업로드 상태를 알리기 위한 채널 설정
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "upload_channel",
                "업로드 알림",
                NotificationManager.IMPORTANCE_LOW  // 낮은 중요도 (소리/진동 없음)
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 사고 영상 처리 메서드
     * AccidentAnalyzer에서 사고로 판단된 영상을 처리합니다.
     *
     * 처리 순서:
     * 1. Presigned URL 요청
     * 2. S3에 영상 업로드
     * 3. 업로드 완료 알림
     *
     * @param videoFile 업로드할 사고 영상 파일
     */
    fun handleAccidentVideo(videoFile: File) {
        scope.launch {
            try {
                // 업로드 시작 알림 표시
                showUploadNotification(0)

                // 1. Presigned URL 요청
                Log.d(TAG, "Presigned URL 요청 시작: ${videoFile.name}")
                val presignedUrlResponse = getPresignedUrl(videoFile)
                Log.d(TAG, "Presigned URL 요청 성공: presignedUrl=${presignedUrlResponse.presignedUrl?.take(50)}..., s3Key=${presignedUrlResponse.s3Key}")
                showUploadNotification(30)

                // 2. S3 업로드
                Log.d(TAG, "S3 업로드 시작: ${videoFile.name}")
                val uploadSuccess = uploadToS3(presignedUrlResponse.presignedUrl, videoFile)
                Log.d(TAG, "S3 업로드 결과: $uploadSuccess")
                showUploadNotification(80)

                // 3. 완료 알림
                if (uploadSuccess) {
                    Log.d(TAG, "업로드 완료 알림 시작: s3Key=${presignedUrlResponse.s3Key}")
                    val notifySuccess = notifyUploadComplete(presignedUrlResponse.s3Key, videoFile)
                    Log.d(TAG, "업로드 완료 알림 결과: $notifySuccess")

                    if (notifySuccess) {
                        // 업로드 및 알림 모두 성공 - 응답에서 analysisStatus를 가져와 표시할 수 있음
                        // 이를 위해서는 notifyUploadComplete 메서드가 UploadCompleteResponse를 반환하도록 수정 필요
                        showUploadCompleteNotification()
                    } else {
                        // 업로드는 성공했으나 완료 알림 실패
                        showUploadFailedNotification("업로드 완료 알림 실패")
                    }
                } else {
                    // S3 업로드 실패
                    showUploadFailedNotification("S3 업로드 실패")
                }
            } catch (e: Exception) {
                // 예외 발생 시 로그 기록 및 실패 알림
                Log.e(TAG, "사고 영상 처리 실패", e)
                showUploadFailedNotification(e.message ?: "알 수 없는 오류")
            }
        }
    }


    /**
     * Presigned URL 요청 메서드
     * 백엔드 API를 호출하여 S3 업로드용 presigned URL을 받아옵니다.
     *
     * @param videoFile 업로드할 영상 파일
     * @return PresignedUrlResponse 객체 (URL 및 S3 키 포함)
     * @throws Exception API 호출 실패 시 예외 발생
     */
    private suspend fun getPresignedUrl(videoFile: File): PresignedUrlResponse {
        return withContext(Dispatchers.IO) {
            try {
                // 타임스탬프를 포함한 고유한 파일명 생성
                val fileName = "accident_${System.currentTimeMillis()}_${videoFile.name}"

                // 파일 해시값 계산
                val fileHash = calculateFileHash(videoFile)

                // 수정된 요청 객체 생성 (4개 인자)
                val request = PresignedUrlRequest(
                    fileName = fileName,
                    contentType = "video/mp4",
                    size = videoFile.length(),     // 파일 크기 추가
                    fileHash = fileHash            // 파일 해시값 추가
                )

                Log.d(TAG, "Presigned URL 요청: fileName=$fileName, contentType=video/mp4, size=${videoFile.length()}, fileHash=$fileHash")

                // API 호출
                val response = backendApiService.getPresignedUrl(request)
                // 응답 상태 로깅
                Log.d(TAG, "Presigned URL 응답 코드: ${response.code()}")

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()

                    // Gson을 사용하여 오류 응답 파싱
                    val errorResponse = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    // 오류 메시지 로깅 및 예외 발생
                    val errorMessage = when (response.code()) {
                        400 -> {
                            if (errorResponse?.message?.contains("필수") == true) {
                                "필수 값이 누락되었습니다: ${errorResponse.message}"
                            } else if (errorResponse?.message?.contains("파일 타입") == true) {
                                "지원하지 않는 파일 타입입니다: ${errorResponse.message}"
                            } else {
                                "요청 오류: ${errorResponse?.message ?: errorBody ?: "알 수 없는 오류"}"
                            }
                        }
                        500 -> "서버 내부 오류: ${errorResponse?.message ?: errorBody ?: "알 수 없는 오류"}"
                        else -> "Presigned URL 요청 실패 (${response.code()}): ${errorResponse?.message ?: errorBody ?: "알 수 없는 오류"}"
                    }

                    Log.e(TAG, errorMessage)
                    throw Exception(errorMessage)
                }

                // 응답 본문 반환, 없으면 예외 발생
                val responseBody = response.body()
                if (responseBody == null) {
                    Log.e(TAG, "Presigned URL 응답 본문이 null입니다")
                    throw Exception("응답 데이터 없음")
                }

                Log.d(TAG, "Presigned URL 응답: presignedUrl=${responseBody.presignedUrl?.take(50)}, s3Key=${responseBody.s3Key}")
                Log.d(TAG, "Presigned URL 유효 시간: 300초")

                // presignedUrl null 체크
                if (responseBody.presignedUrl.isNullOrEmpty()) {
                    Log.e(TAG, "Presigned URL이 null 또는 빈 문자열입니다")
                    throw Exception("Presigned URL이 비어 있음")
                }
                responseBody
            } catch (e: Exception) {
                Log.e(TAG, "Presigned URL 가져오기 실패", e)
                throw e  // 상위 호출자에게 예외 전파
            }
        }
    }

    // 파일 해시값 계산 함수 추가
    private fun calculateFileHash(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            file.inputStream().use { inputStream ->
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    md.update(buffer, 0, bytesRead)
                    bytesRead = inputStream.read(buffer)
                }
            }
            val hashBytes = md.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "파일 해시 계산 중 오류 발생", e)
            // 해시 계산 실패 시 대체값 사용 (실제 환경에서는 더 나은 오류 처리가 필요할 수 있음)
            "error_calculating_hash_${System.currentTimeMillis()}"
        }
    }

    /**
     * S3 업로드 메서드
     * Presigned URL을 사용하여 영상 파일을 S3에 직접 업로드합니다.
     *
     * @param presignedUrl S3 업로드용 Presigned URL
     * @param videoFile 업로드할 영상 파일
     * @return 업로드 성공 여부 (true: 성공, false: 실패)
     */
    private suspend fun uploadToS3(presignedUrl: String, videoFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                // URL null 체크
                if (presignedUrl.isNullOrEmpty()) {
                    Log.e(TAG, "S3 업로드 실패: presignedUrl이 null 또는 빈 문자열입니다")
                    return@withContext false
                }

                Log.d(TAG, "S3 업로드 URL: ${presignedUrl.take(50)}...")
                Log.d(TAG, "파일 정보: 이름=${videoFile.name}, 크기=${videoFile.length()}bytes")

                // 대용량 파일 업로드를 위한 OkHttp 클라이언트 설정
                val client = OkHttpClient.Builder()
                    .writeTimeout(15, TimeUnit.MINUTES)  // 15분 타임아웃 (대용량 파일 고려)
                    .build()

                // 파일을 RequestBody로 변환
                val requestBody = videoFile.asRequestBody("video/mp4".toMediaType())

                // PUT 요청 생성 (S3 Presigned URL은 PUT 메서드 사용)
                val request = Request.Builder()
                    .url(presignedUrl)
                    .put(requestBody)
                    .build()

                // 요청 실행 및 응답 확인
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "S3 업로드 실패", e)
                false  // 예외 발생 시 실패 반환
            }
        }
    }

    /**
     * 업로드 완료 알림 메서드
     * 백엔드 API를 호출하여 업로드 완료를 알립니다.
     *
     * @param s3Key 업로드된 파일의 S3 키
     * @return 알림 성공 여부 (true: 성공, false: 실패)
     */
    private suspend fun notifyUploadComplete(s3Key: String, videoFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 업로드 완료 알림 요청 생성 (Postman 형식과 일치시킴)
                val request = UploadCompleteRequest(
                    fileName = videoFile.name,
                    s3Key = s3Key,
                    contentType = "video/mp4",
                    size = videoFile.length()
                )

                Log.d(TAG, "업로드 완료 알림 요청: $request")

                // API 호출
                val response = backendApiService.notifyUploadComplete(request)

                // 응답 로깅
                Log.d(TAG, "업로드 완료 알림 응답 코드: ${response.code()}")

                // 응답 확인
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()

                    // Gson을 사용하여 오류 응답 파싱
                    val errorResponse = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    // 오류 메시지 로깅 및 처리
                    val errorMessage = when (response.code()) {
                        400 -> {
                            if (errorResponse?.message?.contains("사용자 없음") == true) {
                                "사용자 정보를 찾을 수 없습니다: ${errorResponse.message}"
                            } else {
                                "요청 오류: ${errorResponse?.message ?: errorBody ?: "알 수 없는 오류"}"
                            }
                        }
                        409 -> "이미 업로드된 파일입니다: ${errorResponse?.message ?: errorBody ?: "중복 파일"}"
                        502 -> "AI 분석 요청 실패: ${errorResponse?.message ?: errorBody ?: "AI 서버 연결 문제"}"
                        else -> "업로드 완료 알림 실패 (${response.code()}): ${errorResponse?.message ?: errorBody ?: "알 수 없는 오류"}"
                    }

                    Log.e(TAG, errorMessage)
                    return@withContext false
                }

                val responseBody = response.body()
                Log.d(TAG, "업로드 완료 알림 응답: $responseBody")

                if (responseBody != null) {
                    Log.d(TAG, "분석 상태: ${responseBody.analysisStatus}, 비디오 ID: ${responseBody.videoId}, 파일 타입: ${responseBody.fileType}")
                }

                true  // 성공 반환
            } catch (e: Exception) {
                Log.e(TAG, "업로드 완료 알림 실패", e)
                false  // 예외 발생 시 실패 반환
            }
        }
    }
    /**
     * 업로드 진행 알림 표시 메서드
     *
     * @param progress 업로드 진행률 (0-100)
     */
    private fun showUploadNotification(progress: Int) {
        val builder = NotificationCompat.Builder(context, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_sys_upload)  // 업로드 아이콘
            .setContentTitle("사고 영상 업로드 중")
            .setContentText("업로드 진행 중...")
            .setPriority(NotificationCompat.PRIORITY_LOW)  // 낮은 우선순위
            .setProgress(100, progress, false)  // 진행률 표시 (100 중 progress %)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * 업로드 완료 알림 표시 메서드
     */
    private fun showUploadCompleteNotification(analysisStatus: String = "ANALYZING") {
        val title = "업로드 완료"
        val message = when (analysisStatus) {
            "ANALYZING" -> "사고 영상 업로드가 완료되었습니다. AI 분석이 시작되었습니다."
            "PENDING" -> "사고 영상 업로드가 완료되었습니다. AI 분석 대기 중입니다."
            else -> "사고 영상 업로드가 완료되었습니다."
        }

        val builder = NotificationCompat.Builder(context, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)  // 업로드 완료 아이콘
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)  // 기본 우선순위

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * 업로드 실패 알림 표시 메서드
     *
     * @param errorMessage 오류 메시지
     */
    private fun showUploadFailedNotification(errorMessage: String) {
        val builder = NotificationCompat.Builder(context, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_notify_error)  // 오류 아이콘
            .setContentTitle("업로드 실패")
            .setContentText("사고 영상 업로드 실패: $errorMessage")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)  // 기본 우선순위

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        private const val TAG = "UploadManager"  // 로그 태그
    }
}