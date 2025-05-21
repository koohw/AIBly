import { apiClient } from './CustomAxios';
import type { AxiosError } from 'axios';

export interface PresignedUrlRequest {
  fileName: string;
  contentType: string;
  locationType: number;
}

export interface PresignedUrlResponse {
  presignedUrl: string;
  s3Key: string;
}

export async function getPresignedUrl(
  params: PresignedUrlRequest
): Promise<PresignedUrlResponse> {
  try {
    const res = await apiClient.post<PresignedUrlResponse>('/api/s3/presigned', params);

    if (res.status === 200) {
      console.log('✅ presigned URL 발급 성공:', res.data.presignedUrl);
      return res.data;
    } else {
      console.warn('⚠️ presigned URL 발급 응답 상태:', res.status);
      // 필요에 따라 throw new Error(...) 해도 좋습니다.
      return res.data;
    }
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ presigned URL 발급 실패:', err.response?.data || err.message);
    throw err;
  }
}

/** 수동 업로드 완료 알림 */
export interface UploadNotifyManualRequest {
  fileName: string;
  s3Key: string;
  contentType: string;
  size: number;
}
export interface UploadNotifyManualResponse {
  /** 백엔드가 반환하는 ID 키가 videoId 입니다 */
  videoId: number;
  fileType: string;
  analysisStatus: string;
}
export async function notifyManualUpload(
  params: UploadNotifyManualRequest
): Promise<UploadNotifyManualResponse> {
  try {
    console.log('📫 수동 업로드 알림 요청 중...', params);
    const res = await apiClient.post<UploadNotifyManualResponse>(
      '/api/videos/upload-notify/manual',
      params
    );
    console.log('✅ 수동 업로드 알림 성공:', res.data);
    return res.data;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ 수동 업로드 알림 실패:', err.response?.data || err.message);
    throw err;
  }
}
  // 영상 분석 상태 Polling API
// GET /api/internal/polling/{videoId}/status
export interface PollingStatusResponse {
  status: string;
  reportId: number;
}
export async function pollVideoStatus(
  videoId: string
): Promise<PollingStatusResponse> {
  try {
    console.log(`🔄 Polling 상태 요청: videoId=${videoId}`);
    const res = await apiClient.get<PollingStatusResponse>(
      `/api/internal/polling/${videoId}/status`
    );
    console.log('✅ Polling 상태 응답:', res.data);
    return res.data;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ Polling 상태 조회 실패:', err.response?.data || err.message);
    throw err;
  }
}