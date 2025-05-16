'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Browser } from '@capacitor/browser';
import { CapacitorKakaoLogin } from '@team-lepisode/capacitor-kakao-login'
import { Preferences } from '@capacitor/preferences'
import { App } from '@capacitor/app'
import { registerFcmToken } from '@/lib/api/Fcm' // FCM 토큰 등록 유틸리티 import

export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()
  const [logs, setLogs] = useState<string[]>([])
  const [showLogs, setShowLogs] = useState(false)

  // 로그 함수 정의
  const log = (message: string) => {
    console.log(message)
  }

  // 백엔드 URL을 가져오는 함수
  const getBackendUrl = () => {
    // 환경 변수에서 백엔드 URL 가져오기
    const envUrl = process.env.NEXT_PUBLIC_BACKEND_URL
    
    // 환경 변수가 없으면 기본 URL 사용
    if (!envUrl) {
      log('[LoginPage] NEXT_PUBLIC_BACKEND_URL 환경 변수가 없습니다. 기본 URL을 사용합니다.')
      return 'https://k12e203.p.ssafy.io/api'
    }
    
    return envUrl
  }

 // 1) SDK 초기화
  useEffect(() => {
    log('[LoginPage] 카카오 SDK 초기화 시작')
    CapacitorKakaoLogin.initialize({
      appKey: process.env.NEXT_PUBLIC_KAKAO_NATIVE_APP_KEY || 'b89f880c8784d7bd0779323ad91191af',  // "네이티브 앱 키"
    })
    .then(() => {
      log('[LoginPage] 카카오 SDK 초기화 성공')
    })
    .catch(e => {
      log(`[LoginPage] SDK init 에러: ${e.message || e}`)
    })
  }, [])

  // 앱 상태 변경 리스너 추가
  useEffect(() => {
    let cleanup: (() => void) | undefined;
    
    const setupListener = async () => {
      try {
        log('[LoginPage] 앱 상태 리스너 설정 시작')
        const listener = await App.addListener('appStateChange', ({ isActive }) => {
          log(`[LoginPage] 앱 상태 변경: ${isActive ? '활성화' : '비활성화'}`);
          if (isActive) {
            // 앱이 다시 활성화될 때 필요한 로직 추가
            log('[LoginPage] 앱이 다시 활성화됨, 로그인 상태 확인');
            
            // 로그인 프로세스를 계속하거나 상태를 확인하는 로직을 여기에 추가할 수 있습니다
            Preferences.get({ key: 'kakao_access_token' }).then(({ value }) => {
              if (value) {
                log(`[LoginPage] 저장된 카카오 토큰 발견: ${value.substring(0, 10)}...`);
              } else {
                log('[LoginPage] 저장된 카카오 토큰 없음');
              }
            });
          }
        });
        
        cleanup = () => {
          listener.remove();
        };
        log('[LoginPage] 앱 상태 리스너 설정 완료')
      } catch (error: any) {
        log(`[LoginPage] 앱 상태 리스너 등록 실패: ${error.message || error}`);
      }
    };
    
    setupListener();
    
    return () => {
      if (cleanup) {
        cleanup();
      }
    };
  }, []);

  const handleKakaoLogin = async () => {
    log('📱📱📱 앱 직접 로그인 방식 실행됨');
    log('[LoginPage] 🔥 handleKakaoLogin 호출됨');
    setIsLoading(true);

    try {
      // 1️⃣ 플러그인으로 카카오 로그인 → accessToken, refreshToken 획득
      log('[LoginPage] CapacitorKakaoLogin.login() 호출 시작');
      const { accessToken, refreshToken } = await CapacitorKakaoLogin.login();
      log(`[LoginPage] 🎉 Kakao accessToken: ${accessToken.substring(0, 10)}...`);

      try {
        await Preferences.set({
          key: "kakao_access_token",
          value: accessToken,
        });
        log('[LoginPage] 🎉 카카오 토큰 저장 성공');
      } catch (storageError: any) {
        log(`[LoginPage] 카카오 토큰 저장 실패: ${storageError.message || storageError}`);
      }

      // 2️⃣ 우리 서비스 백엔드에 POST 요청 (authToken 발급)
      try {
        const backendUrl = getBackendUrl();
        log(`[LoginPage] 백엔드 URL: ${backendUrl}`);
        
        const endpoint = `${backendUrl}/oauth/kakao/callback`;
        log(`[LoginPage] 백엔드 요청 URL: ${endpoint}`);
        
        log('[LoginPage] 백엔드로 accessToken 전송 시작');
        const res = await fetch(endpoint, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            accessToken,
            // refreshToken: refreshToken  // 필요한 경우
          }),
        });
        
        log(`[LoginPage] 백엔드 응답 상태: ${res.status} ${res.statusText}`);
        
        if (!res.ok) {
          const errorText = await res.text().catch(() => '응답 본문을 가져올 수 없음');
          log(`[LoginPage] 백엔드 응답 오류: ${errorText}`);
          throw new Error(`백엔드 에러 ${res.status}: ${errorText}`);
        }
        
        const data = await res.json();
        log(`[LoginPage] 백엔드 응답 데이터: ${JSON.stringify(data)}`);
        
        // authToken 추출
        const authToken = data.authToken || data.token;
        
        if (!authToken) {
          log('[LoginPage] authToken이 응답에 없습니다');
          throw new Error('authToken이 응답에 없습니다');
        }
        
        log(`[LoginPage] 🔑 서비스 JWT(authToken): ${authToken.substring(0, 10)}...`);

        // 3️⃣ 로컬 스토리지 대신 Capacitor Preferences 에 저장
        await Preferences.set({ key: 'AUTH_TOKEN', value: authToken });
        log('[LoginPage] AUTH_TOKEN 저장 완료');

        // [추가] FCM 토큰 등록
        try {
          await registerFcmToken(authToken);
          log('[LoginPage] FCM 토큰 등록 요청 완료');
        } catch (fcmError: any) {
          log(`[LoginPage] FCM 토큰 등록 중 오류 (로그인은 계속 진행됨): ${fcmError.message || fcmError}`);
        }

        // 4️⃣ 대시보드 페이지로 이동
        log('[LoginPage] 대시보드로 이동 시작');
        router.replace('/dashboard');
      } catch (backendError: any) {
        log(`[LoginPage] 백엔드 통신 오류: ${backendError.message || backendError}`);
        throw backendError;
      }
    } catch (e: any) {
      log(`[LoginPage] 로그인 처리 중 에러: ${e.message || e}`);
      alert(`로그인 처리 중 오류가 발생했습니다: ${e.message || '알 수 없는 오류'}`);
    } finally {
      setIsLoading(false);
    }
  };
  
  // 로그 토글 처리
  const toggleLogs = () => {
    setShowLogs(prev => !prev)
  }

  // 로그 지우기
  const clearLogs = () => {
    setLogs([])
  }

  const handleBack = () => router.back()

  return (
    <div className="min-h-screen bg-black text-white flex flex-col">
      {/* 헤더 */}
      <header className="p-4 border-b border-gray-800 flex items-center">
        <Button variant="ghost" size="icon" onClick={handleBack} className="mr-2">
          <ArrowLeft size={20} />
        </Button>
        <h1 className="text-xl font-bold">로그인</h1>
      </header>

      {/* 메인 콘텐츠 */}
      <main className="flex-1 flex flex-col items-center justify-center p-6">
        <div className="w-full max-w-md space-y-8">
          {/* 로고 */}
          <div className="text-center mb-8">
            <div className="inline-block p-4 rounded-full bg-gray-900 mb-4">
              <div className="w-16 h-16 rounded-full bg-red-600 flex items-center justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="32"
                  height="32"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M12 13V2l8 4-8 4" />
                  <path d="M20.55 10.23A9 9 0 1 1 8 4.94" />
                  <path d="M8 10a5 5 0 1 0 8.9 2.02" />
                </svg>
              </div>
            </div>
            <h2 className="text-2xl font-bold">블랙박스 리뷰</h2>
            <p className="text-gray-400 mt-2">로그인하여 모든 기능을 이용하세요</p>
          </div>

          {/* 로그인 버튼 */}
          <div className="space-y-4">
            <button
              onClick={handleKakaoLogin}
              disabled={isLoading}
              className="w-full py-4 flex items-center justify-center space-x-2 text-black font-medium rounded-md bg-[#FEE500] hover:bg-[#FDD835] transition-colors"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <Image src="/kakao-logo.png" alt="Kakao" width={24} height={24} className="mr-2" />
                  카카오톡으로 로그인하기
                </>
              )}
            </button>

            <div className="text-center">
              <p className="text-sm text-gray-500">
                로그인하면 블랙박스 리뷰의{" "}
                <span className="text-gray-400 underline cursor-pointer">서비스 이용약관</span>과{" "}
                <span className="text-gray-400 underline cursor-pointer">개인정보 처리방침</span>에 동의하게 됩니다.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
