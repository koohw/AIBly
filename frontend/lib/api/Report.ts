import { apiClient } from './CustomAxios';
import type { AxiosError } from 'axios';

/**
 * 보고서 목록 조회 응답 타입
 */
export interface ReportListResponse {
  id: number;
  title: string;
  accidentCode: string;
  createdAt: string;
}


/**
 * 보고서 목록 조회 API
 * GET /api/my/reports
 * @returns ReportListResponse[]
 */
export async function getReportList(): Promise<ReportListResponse[]> {
  try {
    console.log('🎯 내 보고서 목록 조회 요청');
    const res = await apiClient.get<ReportListResponse[]>('/api/my/reports');
    console.log('✅ 내 보고서 목록 조회 성공:', res.data);
    return res.data;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ 내 보고서 목록 조회 실패:', err.response?.data || err.message);
    throw err;
  }
}


/**
 * 보고서 상세 조회 응답 타입
 */
// lib/api/Report.ts

export interface ReportDetailResponse {
  id: number
  title: string
  accidentType: string
  laws: string
  precedents: string       // 기존 decisions 대신 precedents
  carAProgress: string     // 진행 방향이나 상태
  carBProgress: string
  faultA: number           // 과실 비율 (%)
  faultB: number
  createdAt: string
  damageLocation: string | null
  eventTimeline: string    // 필요한 경우 JSON.parse로 파싱
  // …필요한 다른 필드들도 여기 추가
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
