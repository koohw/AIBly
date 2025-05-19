"use client";

import { useEffect, useState, useRef } from "react";
import { useRouter } from "next/navigation";
import { App } from "@capacitor/app";
import { Preferences } from "@capacitor/preferences";
import { registerFcmToken } from "@/lib/api/Fcm"; // FCM 토큰 등록 유틸리티 import

export default function DeepLink() {
  const router = useRouter();
  const [disabled, setDisabled] = useState(false);
  const initialized = useRef(false);

  // 컴포넌트 초기화 시 FCM 플래그 확인
  useEffect(() => {
    if (initialized.current) return;
    
    if (typeof window !== 'undefined') {
      // FCM 알림 플래그 확인 - 여러 위치에서 확인
      const isFcmNavigation = 
        window.__FROM_FCM_NOTIFICATION === true || 
        window.__FCM_REPORT_ID || 
        window.location.search.includes('__fcm=true');
      
      if (isFcmNavigation) {
        console.log('[DeepLink] FCM 알림 관련 플래그가 감지되어 딥링크 처리 비활성화됨');
        setDisabled(true);
        initialized.current = true;
        return;
      }
      
      // URL 검사 - analysis 경로와 id 파라미터 검사
      const isAnalysisPath = window.location.pathname.includes('/analysis');
      const searchParams = new URLSearchParams(window.location.search);
      const hasReportId = searchParams.has('id');
      
      if (isAnalysisPath && hasReportId) {
        console.log('[DeepLink] 분석 페이지와 ID 파라미터가 감지되어 딥링크 처리 비활성화됨');
        setDisabled(true);
        initialized.current = true;
        return;
      }
    }
    
    initialized.current = true;
  }, []);

  // 백엔드 API URL을 가져오는 함수
  const getBackendUrl = () => {
    // 환경 변수에서 백엔드 URL 가져오기
    const envUrl = process.env.NEXT_PUBLIC_BACKEND_URL;

    // 환경 변수가 없으면 기본 URL 사용
    if (!envUrl) {
      console.log(
        "[DeepLink] NEXT_PUBLIC_BACKEND_URL 환경 변수가 없습니다. 기본 URL을 사용합니다."
      );
      return "https://k12e203.p.ssafy.io/api";
    }

    return envUrl;
  };

  // 인가 코드로 카카오 액세스 토큰 얻기
  const getKakaoAccessToken = async (code: string) => {
    console.log("[DeepLink] 인가 코드로 카카오 액세스 토큰 요청 시작");

    try {
      const api = getBackendUrl();
      const endpoint = `${api}/oauth/kakao/code-callback`;

      try {
        const res = await fetch(endpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ code }),
        });

        if (!res.ok) {
          const errorText = await res
            .text()
            .catch(() => "응답 본문을 가져올 수 없음");
          console.error("[DeepLink] 백엔드 응답 오류:", errorText);
          throw new Error(`HTTP ${res.status}: ${errorText}`);
        }

        const responseData = await res.json();
        return responseData;
      } catch (error: any) {
        console.error("[DeepLink] fetch 요청 실패:", error.message);
        throw error;
      }
    } catch (error: any) {
      console.error("[DeepLink] 카카오 액세스 토큰 요청 실패:", error.message);
      throw error;
    }
  };

  // FCM 알림 URL인지 확인하는 함수
  const isFcmAnalysisUrl = (url: string): boolean => {
    try {
      const parsed = new URL(url);
      const pathWithQuery = parsed.pathname + parsed.search;
      
      // analysis 경로와 id 파라미터를 확인
      const isAnalysisPath = parsed.pathname.includes('/analysis');
      const hasIdParam = parsed.searchParams.has('id');
      
      // FCM 알림에서 온 URL이거나 명시적 FCM 플래그가 있는 경우
      const hasFcmParam = parsed.searchParams.has('__fcm');
      
      // 분석 페이지 + id 파라미터 조합이면 FCM URL로 간주
      return (isAnalysisPath && hasIdParam) || hasFcmParam;
    } catch (e) {
      return false;
    }
  };

  // 딥링크 처리 함수
  const processDeepLink = async (url: string) => {
    try {
      console.log("[DeepLink] 처리 중:", url);
      
      // 이미 비활성화되었는지 확인
      if (disabled) {
        console.log("[DeepLink] 컴포넌트가 비활성화되어 처리를 건너뜁니다");
        return;
      }
      
      // FCM 알림으로부터의 요청인지 확인
      if (isFcmAnalysisUrl(url)) {
        console.log("[DeepLink] FCM 알림 URL 감지, 처리를 건너뜁니다:", url);
        return; // FCM 알림 URL은 여기서 처리하지 않음
      }

      // FCM 플래그가 설정되어 있으면 DeepLink 처리 중지
      if (typeof window !== 'undefined' && window.__FROM_FCM_NOTIFICATION) {
        console.log("[DeepLink] FCM 플래그가 설정되어 있어 처리를 중지합니다");
        setDisabled(true);
        return;
      }

      // 인가 코드 파싱 부분
      let code: string | null = null;
      try {
        const parsed = new URL(url);
        code = parsed.searchParams.get("code");
      } catch (e) {
        console.error("[DeepLink] URL 파싱 실패");
        return;
      }

      if (!code) {
        console.log("[DeepLink] authorization code가 없습니다");
        return;
      }

      console.log("[DeepLink] 인가 코드로 토큰 교환 시작");

      // 인가 코드로 액세스 토큰 요청 및 백엔드에 전송
      const data = await getKakaoAccessToken(code);

      // token 또는 authToken 필드 확인
      const authToken = data.authToken || data.token;

      if (!authToken) {
        console.error("[DeepLink] 응답에 토큰이 없습니다:", data);
        throw new Error("응답에 유효한 토큰이 없습니다");
      }

      // Preferences 를 이용해 네이티브 저장소에 토큰 저장
      await Preferences.set({ key: "AUTH_TOKEN", value: authToken });
      console.log("[DeepLink] 토큰 저장 완료");

      // FCM 토큰 등록 (추가)
      try {
        console.log("[DeepLink] FCM 토큰 등록 시도");
        await registerFcmToken(authToken);
        console.log("[DeepLink] FCM 토큰 등록 완료");
      } catch (fcmError) {
        console.error("[DeepLink] FCM 토큰 등록 실패 (계속 진행):", fcmError);
        // FCM 등록 실패해도 로그인 프로세스는 계속 진행
      }

      // 딥링크 처리 완료 후 저장된 URL 제거
      await Preferences.remove({ key: "PENDING_DEEP_LINK" });
      console.log("[DeepLink] 딥링크 처리 완료");

      // 대시보드 페이지로 이동
      router.replace("/dashboard");
    } catch (e: any) {
      console.error("[DeepLink] 로그인 완료 후 처리 실패:", e.message || e);
    }
  };

  useEffect(() => {
    // 이미 비활성화되었으면 처리하지 않음
    if (disabled) {
      console.log("[DeepLink] 컴포넌트가 비활성화되어 있어 리스너를 등록하지 않습니다");
      return;
    }
    
    // FCM 플래그 확인
    if (typeof window !== 'undefined' && window.__FROM_FCM_NOTIFICATION) {
      console.log("[DeepLink] FCM 플래그가 설정되어 비활성화됩니다");
      setDisabled(true);
      return;
    }

    // 1. 먼저 저장된 딥링크가 있는지 확인하고 자동 처리
    const checkPendingDeepLink = async () => {
      try {
        const { value } = await Preferences.get({ key: "PENDING_DEEP_LINK" });
        if (value) {
          console.log("[DeepLink] 저장된 딥링크 발견:", value);
          
          // FCM 알림 URL이면 건너뛰기
          if (isFcmAnalysisUrl(value)) {
            console.log("[DeepLink] FCM 알림 URL은 처리하지 않습니다:", value);
            await Preferences.remove({ key: "PENDING_DEEP_LINK" });
            return;
          }
          
          // 자동으로 처리
          await processDeepLink(value);
        }
      } catch (e) {
        console.error("[DeepLink] 저장된 딥링크 확인 중 오류");
      }
    };

    checkPendingDeepLink();

    // 2. 딥링크 리스너 등록
    const handler = async (event: any) => {
      const { url } = event;
      console.log("[DeepLink] appUrlOpen 이벤트 수신:", url);
      
      // 이미 비활성화되었는지 다시 확인
      if (disabled) {
        console.log("[DeepLink] 컴포넌트가 비활성화되어 URL 처리를 건너뜁니다:", url);
        return;
      }
      
      // FCM 알림 URL이면 건너뛰기
      if (isFcmAnalysisUrl(url)) {
        console.log("[DeepLink] FCM 알림 URL은 처리하지 않습니다:", url);
        return;
      }

      // 딥링크 URL 저장 (앱이 재시작되더라도 처리할 수 있도록)
      await Preferences.set({ key: "PENDING_DEEP_LINK", value: url });

      // 바로 처리
      await processDeepLink(url);
    };

    // FCM 플래그가 설정되어 있지 않을 때만 리스너 등록
    if (!disabled) {
      // 리스너 등록
      const listenerPromise = App.addListener("appUrlOpen", handler);
      
      return () => {
        // 리스너 해제
        listenerPromise.then((listener) => listener.remove());
      };
    }
  }, [router, disabled]);

  // 컴포넌트 반환 (data-deeplink 속성 포함)
  return <div data-deeplink={true} data-disabled={disabled} style={{ display: 'none' }} />;
}
