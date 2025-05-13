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

export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()

 // 1) SDK 초기화
  useEffect(() => {
    CapacitorKakaoLogin.initialize({
      appKey: process.env.NEXT_PUBLIC_KAKAO_NATIVE_APP_KEY!,  // "네이티브 앱 키"
    }).catch(e => console.error('SDK init 에러', e))
  }, [])

  // 앱 상태 변경 리스너 추가
  useEffect(() => {
    let cleanup: (() => void) | undefined;
    
    const setupListener = async () => {
      try {
        const listener = await App.addListener('appStateChange', ({ isActive }) => {
          console.log('[LoginPage] 앱 상태 변경:', isActive ? '활성화' : '비활성화');
          if (isActive) {
            // 앱이 다시 활성화될 때 필요한 로직 추가
            console.log('[LoginPage] 앱이 다시 활성화됨, 로그인 상태 확인');
            
            // 로그인 프로세스를 계속하거나 상태를 확인하는 로직을 여기에 추가할 수 있습니다
            Preferences.get({ key: 'kakao_access_token' }).then(({ value }) => {
              if (value) {
                console.log('[LoginPage] 저장된 카카오 토큰 발견:', value);
              }
            });
          }
        });
        
        cleanup = () => {
          listener.remove();
        };
      } catch (error) {
        console.error('[LoginPage] 앱 상태 리스너 등록 실패:', error);
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
    console.log('[LoginPage] 🔥 handleKakaoLogin 호출됨');
    setIsLoading(true);

      try {
    // 1️⃣ 플러그인으로 카카오 로그인 → accessToken, refreshToken 획득
    const { accessToken, refreshToken } = await CapacitorKakaoLogin.login();
    console.log('[LoginPage] 🎉 Kakao accessToken:', accessToken);

    await Preferences.set({
      key: "kakao_access_token",
      value: accessToken,
    });
    console.log('[LoginPage] 🎉 저장성공', accessToken);

    // 2️⃣ 우리 서비스 백엔드에 POST 요청 (authToken 발급)
    const res = await fetch(
      `${process.env.NEXT_PUBLIC_BACKEND_URL}/oauth/kakao/callback`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          accessToken,
          // refreshToken: refreshToken  // 필요한 경우
        }),
      }
    );
    if (!res.ok) {
      throw new Error(`백엔드 에러 ${res.status}`);
    }
    const { authToken } = await res.json();
    console.log('[LoginPage] 🔑 서비스 JWT(authToken):', authToken);

    // 3️⃣ 로컬 스토리지 대신 Capacitor Preferences 에 저장
    await Preferences.set({ key: 'AUTH_TOKEN', value: authToken });

    // 4️⃣ 대시보드 페이지로 이동
    router.replace('/dashboard');
  } catch (e) {
    console.error('[LoginPage] 로그인 처리 중 에러', e);
    // TODO: 사용자에게 오류 UI 띄우기
  } finally {
    setIsLoading(false);
  }
};
  

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
