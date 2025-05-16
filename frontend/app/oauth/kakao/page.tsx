'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Preferences } from '@capacitor/preferences'
import { registerFcmToken } from '@/lib/api/Fcm'

export default function KakaoOAuthCallbackPage() {
  const router = useRouter()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    console.log('웹 리다이렉트 로그인 방식 실행')

    if (!code) {
      console.error('인가 코드가 없습니다.')
      return
    }

    // 환경 변수에서 백엔드 URL 가져오기
    const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || 'https://k12e203.p.ssafy.io/api';

    // code-callback 엔드포인트 호출
    fetch(
      `${backendUrl}/oauth/kakao/code-callback`,
      { 
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ code })
      }
    )
      .then(response => {
        if (!response.ok) {
          throw new Error(`백엔드 응답 오류: ${response.status}`)
        }
        return response.json()
      })
      .then(async data => {
        // authToken이 있는지 확인
        if (data && data.authToken) {
          // 토큰 저장
          await Preferences.set({ key: 'AUTH_TOKEN', value: data.authToken })
          
          // FCM 토큰 등록
          try {
            await registerFcmToken(data.authToken)
          } catch (fcmError) {
            console.error('FCM 토큰 등록 오류:', fcmError)
          }
        } else {
          console.error('응답에 인증 토큰이 없습니다:', data)
        }
        
        // 대시보드로 이동
        router.replace('/dashboard')
      })
      .catch(error => {
        console.error('로그인 처리 중 오류:', error)
      })
  }, [router])

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-gray-900 mb-4"></div>
      <p className="text-lg">로그인 처리 중… 잠시만 기다려 주세요.</p>
    </div>
  )
}