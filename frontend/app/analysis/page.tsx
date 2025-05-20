'use client'

import React, { Suspense, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import ClientReport from '@/components/analysis/ClientReport'

// 전역 window 타입 확장
declare global {
  interface Window {
    __FROM_FCM_NOTIFICATION?: boolean;
    __FCM_REPORT_ID?: string;
  }
}

// 메인 페이지 컴포넌트
export default function AnalysisPage() {
  const [isClient, setIsClient] = useState(false);
  const [reportId, setReportId] = useState<string | null>(null);
  const router = useRouter();
  
  // 클라이언트 사이드에서만 실행
  useEffect(() => {
    // 클라이언트 환경 표시
    setIsClient(true);
    
    if (typeof window !== 'undefined') {
      // 1. URL 파라미터에서 ID 추출
      const urlParams = new URLSearchParams(window.location.search);
      let id = urlParams.get('id');
      
      // 2. FCM 환경 변수에서 ID 추출 (백업)
      if (!id && window.__FCM_REPORT_ID) {
        console.log('URL에서 ID를 찾지 못했지만 FCM 환경 변수에서 ID 발견:', window.__FCM_REPORT_ID);
        id = window.__FCM_REPORT_ID;
      }
      
      // 3. 둘 다 없으면 대시보드로 리다이렉트
      if (!id) {
        console.log('보고서 ID를 찾을 수 없어 대시보드로 이동합니다');
        router.replace('/dashboard');
        return;
      }
      
      // 4. ID가 있으면 설정
      console.log('분석할 보고서 ID 설정:', id);
      setReportId(id);
      
      // 5. 로그 출력 (디버깅용)
      if (window.__FROM_FCM_NOTIFICATION) {
        console.log('FCM 알림을 통해 분석 페이지가 열렸습니다. ID:', id);
      }
    }
  }, [router]);
  
  // 클라이언트 사이드 확인
  if (!isClient) {
    return <div className="p-4 text-center">분석 페이지 로딩 중...</div>;
  }
  
  // 보고서 ID가 없으면 로딩 표시
  if (!reportId) {
    return <div className="p-4 text-center">보고서 ID를 확인하는 중...</div>;
  }
  
  // 보고서 컴포넌트 렌더링
  return (
    <Suspense fallback={<div className="p-4 text-center">보고서를 불러오는 중...</div>}>
      <ClientReport id={reportId} />
    </Suspense>
  );
}