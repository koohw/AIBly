'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Browser } from '@capacitor/browser';
import { CapacitorKakaoLogin } from '@team-lepisode/capacitor-kakao-login'
import { Preferences } from '@capacitor/preferences'





export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()

 // 1) SDK 초기화
  useEffect(() => {
    CapacitorKakaoLogin.initialize({
      appKey: process.env.NEXT_PUBLIC_KAKAO_NATIVE_APP_KEY!,  // “네이티브 앱 키”
    }).catch(e => console.error('SDK init 에러', e))
  }, [])

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
      `${process.env.NEXT_PUBLIC_API_URL}/oauth/kakao/callback`,
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
      <header className="p-4 border-b border-gray-800 flex items-center">
        <Button variant="ghost" size="icon" onClick={handleBack} className="mr-2">
          <ArrowLeft size={20} />
        </Button>
        <h1 className="text-xl font-bold">로그인test</h1>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center p-6">
        <div className="w-full max-w-md space-y-8">
          {/* 로고 & 설명 생략 */}

          <div className="space-y-4">
            <button
              onClick={handleKakaoLogin}
              disabled={isLoading}
              className="w-full py-4 flex items-center justify-center space-x-2 text-black font-medium rounded-md bg-[#FEE500] hover:bg-[#FDD835] transition-colors"
            >
              {isLoading
                ? <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
                : (
                  <>
                    <Image src="/kakao-logo.png" alt="Kakao" width={24} height={24} />
                    카카오톡으로 로그인하기
                  </>
                )
              }
            </button>
            {/* 약관 문구 등 생략 */}
          </div>
        </div>
      </main>
    </div>
  )
}
