import { apiClient } from './CustomAxios';
import type { AxiosError } from 'axios';

/**
 * 보고서 상세 조회 응답 타입
 */
export interface ReportDetailResponse {
  reportId: number;
  title: string;
  accidentCode: string;
  accidentType: string;
  carA: string;
  carB: string;
  mainEvidence: string;
  laws: string;
  decisions: string;
  createdAt: string;
}

/**
 * 보고서 상세 조회 API
 * GET /api/my/reports/{reportId}
 * @param reportId 조회할 reportId
 * @returns ReportDetailResponse
 */
export async function getReportDetail(
  reportId: number | string
): Promise<ReportDetailResponse> {
  try {
    console.log(`🎯 보고서 상세 조회 요청: reportId=${reportId}`);
    const res = await apiClient.get<ReportDetailResponse>(
      `/api/my/reports/${reportId}`
    );
    console.log('✅ 보고서 상세 조회 성공:', res.data);
    return res.data;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ 보고서 상세 조회 실패:', err.response?.data || err.message);
    throw err;
  }
}
