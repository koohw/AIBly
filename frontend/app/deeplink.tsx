'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { App } from '@capacitor/app'
import { Preferences } from '@capacitor/preferences'

export default function DeepLink() {
  const router = useRouter()

  // 백엔드 API URL을 가져오는 함수
  const getBackendUrl = () => {
    // 환경 변수에서 백엔드 URL 가져오기
    const envUrl = process.env.NEXT_PUBLIC_BACKEND_URL
    
    // 환경 변수가 없으면 기본 URL 사용
    if (!envUrl) {
      console.log('[DeepLink] NEXT_PUBLIC_BACKEND_URL 환경 변수가 없습니다. 기본 URL을 사용합니다.')
      return 'https://k12e203.p.ssafy.io/api'
    }
    
    return envUrl
  }

  // 인가 코드로 카카오 액세스 토큰 얻기
  const getKakaoAccessToken = async (code: string) => {
    console.log('[DeepLink] 인가 코드로 카카오 액세스 토큰 요청 시작')
    
    try {
      const api = getBackendUrl()
      const endpoint = `${api}/oauth/kakao/code-callback`
      
      try {
        const res = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ code }),
        })
        
        if (!res.ok) {
          const errorText = await res.text().catch(() => '응답 본문을 가져올 수 없음')
          console.error('[DeepLink] 백엔드 응답 오류:', errorText)
          throw new Error(`HTTP ${res.status}: ${errorText}`)
        }
        
        const responseData = await res.json()
        return responseData
      } catch (error: any) {
        console.error('[DeepLink] fetch 요청 실패:', error.message)
        throw error
      }
    } catch (error: any) {
      console.error('[DeepLink] 카카오 액세스 토큰 요청 실패:', error.message)
      throw error
    }
  }

  // 딥링크 처리 함수
  const processDeepLink = async (url: string) => {
    try {
      console.log('[DeepLink] 처리 중:', url)

      // 인가 코드 파싱 부분
      let code: string | null = null
      try {
        const parsed = new URL(url)
        code = parsed.searchParams.get('code')
      } catch (e) {
        console.error('[DeepLink] URL 파싱 실패')
        return
      }

      if (!code) {
        console.log('[DeepLink] authorization code가 없습니다')
        return
      }

      console.log('[DeepLink] 인가 코드로 토큰 교환 시작')
      
      // 인가 코드로 액세스 토큰 요청 및 백엔드에 전송
      const data = await getKakaoAccessToken(code)
      
      // token 또는 authToken 필드 확인
      const authToken = data.authToken || data.token
      
      if (!authToken) {
        console.error('[DeepLink] 응답에 토큰이 없습니다:', data)
        throw new Error('응답에 유효한 토큰이 없습니다')
      }
      
      // Preferences 를 이용해 네이티브 저장소에 토큰 저장
      await Preferences.set({ key: 'AUTH_TOKEN', value: authToken })
      console.log('[DeepLink] 토큰 저장 완료')

      // 딥링크 처리 완료 후 저장된 URL 제거
      await Preferences.remove({ key: 'PENDING_DEEP_LINK' })
      console.log('[DeepLink] 딥링크 처리 완료')
      
      // 대시보드 페이지로 이동
      router.replace('/dashboard')
    } catch (e: any) {
      console.error('[DeepLink] 로그인 완료 후 처리 실패:', e.message || e)
    }
  }

  useEffect(() => {
    // 1. 먼저 저장된 딥링크가 있는지 확인하고 자동 처리
    const checkPendingDeepLink = async () => {
      try {
        const { value } = await Preferences.get({ key: 'PENDING_DEEP_LINK' })
        if (value) {
          console.log('[DeepLink] 저장된 딥링크 발견:', value)
          // 자동으로 처리
          await processDeepLink(value)
        }
      } catch (e) {
        console.error('[DeepLink] 저장된 딥링크 확인 중 오류')
      }
    }
    
    checkPendingDeepLink()
    
    // 2. 딥링크 리스너 등록
    const handler = async (event: any) => {
      const { url } = event
      console.log('[DeepLink] appUrlOpen 이벤트 수신:', url)
      
      // 딥링크 URL 저장 (앱이 재시작되더라도 처리할 수 있도록)
      await Preferences.set({ key: 'PENDING_DEEP_LINK', value: url })
      
      // 바로 처리
      await processDeepLink(url)
    }

    // 리스너 등록
    const listenerPromise = App.addListener('appUrlOpen', handler)

    return () => {
      // 리스너 해제
      listenerPromise.then((listener) => listener.remove())
    }
  }, [router])

  // 화면에 아무것도 렌더링하지 않음
  return null
}
