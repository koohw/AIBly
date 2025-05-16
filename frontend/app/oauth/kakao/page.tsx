'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Preferences } from '@capacitor/preferences'
import { registerFcmToken } from '@/lib/api/Fcm' // FCM 토큰 등록 함수 import 추가

export default function KakaoOAuthCallbackPage() {
  const router = useRouter()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    console.log('🌐🌐🌐 웹 리다이렉트 로그인 방식 실행됨');


    if (!code) {
      console.error('인가 코드가 없습니다.')
      return
    }

    console.log('🔵 OAuth 콜백 페이지 - 인가 코드 획득:', code.substring(0, 10) + '...')

    // 1) 백엔드로 code 보내서 토큰 받기
    fetch(
      `${process.env.NEXT_PUBLIC_BACKEND_URL}/oauth/kakao/callback?code=${code}`,
      { 
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    )
      .then(response => {
        if (!response.ok) {
          throw new Error(`백엔드 응답 오류: ${response.status}`)
        }
        console.log('🔵 OAuth 콜백 - 백엔드 응답 성공')
        return response.json()
      })
      .then(async data => {
        console.log('🔵 OAuth 콜백 - 응답 데이터 수신:', data)
        
        // authToken이 있는지 확인
        if (data && data.authToken) {
          console.log('🔵 OAuth 콜백 - 인증 토큰 수신:', data.authToken.substring(0, 10) + '...')
          
          // 토큰 저장
          await Preferences.set({ key: 'AUTH_TOKEN', value: data.authToken })
          console.log('🔵 OAuth 콜백 - 인증 토큰 저장 완료')
          
          // FCM 토큰 등록 추가
          try {
            console.log('🔵 OAuth 콜백 - FCM 토큰 등록 시도')
            await registerFcmToken(data.authToken)
            console.log('🔵 OAuth 콜백 - FCM 토큰 등록 완료')
          } catch (fcmError) {
            console.error('🔵 OAuth 콜백 - FCM 토큰 등록 오류:', fcmError)
          }
        } else {
          console.error('🔵 OAuth 콜백 - 응답에 인증 토큰이, 없습니다:', data)
        }
        
        // 대시보드로 이동
        router.replace('/dashboard')
      })
      .catch(error => {
        console.error('🔵 OAuth 콜백 - 처리 중 오류:', error)
      })
  }, [router])

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-gray-900 mb-4"></div>
      <p className="text-lg">로그인 처리 중… 잠시만 기다려 주세요.</p>
    </div>
  )
}