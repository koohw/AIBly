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
 * 보고서 목록 항목 타입
 */
export interface ReportListItem {
  reportId: number;
  title: string;
  accidentType: string;
  createdAt: string;
  thumbnailUrl?: string;
  faultRatio?: string;
  isNew: boolean;
}

/**
 * 보고서 목록 응답 타입
 */
export interface ReportListResponse {
  reports: ReportListItem[];
  totalCount: number;
}

/**
 * PDF 다운로드 응답 타입
 */
export interface PdfDownloadResponse {
  downloadUrl: string;
  expirySeconds: number;
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

/**
 * 최근 생성된 보고서 목록 조회 API
 * GET /api/my/reports
 * @param limit 조회할 최대 개수 (기본값: 5)
 * @returns ReportListResponse | ReportListItem[]
 */
export async function getRecentReports(limit: number = 5): Promise<ReportListResponse | ReportListItem[]> {
  try {
    console.log(`🎯 최근 보고서 목록 조회 요청: limit=${limit}`);
    
    // 토큰 로깅
    const token = localStorage.getItem('auth_token') || '토큰 없음';
    console.log(`🔑 현재 사용 중인 토큰(일부): ${token.substring(0, 15)}...`);
    
    const res = await apiClient.get(
      `/api/my/reports?limit=${limit}`
    );
    
    console.log('✅ 최근 보고서 목록 조회 성공');
    console.log('응답 데이터 타입:', typeof res.data);
    console.log('응답 데이터가 배열인가?', Array.isArray(res.data));
    
    // 응답 형식에 따른 처리
    let processedData;
    
    if (Array.isArray(res.data)) {
      // 배열 형태 응답
      console.log('배열 형태 응답, 아이템 수:', res.data.length);
      processedData = res.data;
    } else if (res.data && typeof res.data === 'object') {
      // 객체 형태 응답, "reports" 필드 확인
      if ('reports' in res.data && Array.isArray(res.data.reports)) {
        console.log('객체 형태 응답, reports 배열 아이템 수:', res.data.reports.length);
        processedData = res.data;
      } else {
        // "reports" 필드가 없거나 배열이 아닌 경우 빈 배열로 설정
        console.warn('응답에 reports 배열이 없음, 빈 배열로 처리');
        processedData = [] as ReportListItem[];
      }
    } else {
      // 예상치 못한 응답 형식
      console.warn('예상치 못한 응답 형식, 빈 배열로 처리');
      processedData = [] as ReportListItem[];
    }
    
    // 데이터 내용 일부 로깅
    console.log('처리된 응답 데이터 일부:', 
      JSON.stringify(
        Array.isArray(processedData) 
          ? processedData.slice(0, 2) 
          : processedData.reports.slice(0, 2)
      )
    );
    
    return processedData;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ 최근 보고서 목록 조회 실패:', err.response?.data || err.message);
    console.error('에러 상태 코드:', err.response?.status);
    console.error('에러 상세 정보:', err);
    
    // 에러 발생 시 기본값 반환 (빈 배열)
    return [] as ReportListItem[];
  }
}

/**
 * 보고서 PDF 생성 및 다운로드 링크 조회 API
 * POST /api/reports/{reportId}/pdf
 * @param reportId 보고서 ID
 * @returns PdfDownloadResponse
 */
export async function generateReportPdf(reportId: number | string): Promise<PdfDownloadResponse> {
  try {
    console.log(`🎯 보고서 PDF 생성 요청: reportId=${reportId}`);
    const res = await apiClient.post<PdfDownloadResponse>(
      `/api/reports/${reportId}/pdf`
    );
    console.log('✅ 보고서 PDF 생성 성공:', res.data);
    return res.data;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ 보고서 PDF 생성 실패:', err.response?.data || err.message);
    throw err;
  }
}

/**
 * 기존 생성된 보고서 PDF 다운로드 링크 조회 API
 * GET /api/reports/{reportId}/pdf-url
 * @param reportId 보고서 ID
 * @returns PdfDownloadResponse
 */
export async function getReportPdfUrl(reportId: number | string): Promise<PdfDownloadResponse> {
  try {
    console.log(`🎯 보고서 PDF URL 조회 요청: reportId=${reportId}`);
    const res = await apiClient.get<PdfDownloadResponse>(
      `/api/reports/${reportId}/pdf-url`
    );
    console.log('✅ 보고서 PDF URL 조회 성공:', res.data);
    return res.data;
  } catch (error) {
    const err = error as AxiosError;
    console.error('❌ 보고서 PDF URL 조회 실패:', err.response?.data || err.message);
    throw err;
  }
}

/**
 * 보고서 PDF 다운로드 함수 (새로 생성 또는 기존 PDF 사용)
 * @param reportId 보고서 ID
 * @returns 다운로드 URL
 */
export async function downloadReportPdf(reportId: number | string): Promise<string> {
  try {
    console.log(`🎯 보고서 PDF 다운로드 URL 요청: reportId=${reportId}`);
    
    // presigned URL은 만료 시간이 있으므로 항상 새로운 URL 요청
    let downloadUrl: string;
    
    try {
      // 먼저 기존 PDF URL 조회 시도 (항상 새로운 presigned URL 발급)
      const response = await getReportPdfUrl(reportId);
      downloadUrl = response.downloadUrl;
      console.log(`✅ PDF URL 획득 성공, 만료 시간: ${response.expirySeconds}초`);
    } catch (error) {
      console.log('기존 PDF 없음, 새로 생성합니다.');
      // 기존 PDF가 없으면, PDF 생성 요청 후 URL 획득
      const response = await generateReportPdf(reportId);
      downloadUrl = response.downloadUrl;
      console.log(`✅ PDF 생성 및 URL 획득 성공, 만료 시간: ${response.expirySeconds}초`);
    }
    
    return downloadUrl;
  } catch (error) {
    console.error('PDF 다운로드 URL 획득 실패:', error);
    throw error;
  }
}
