'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function KakaoOAuthCallbackPage() {
  const router = useRouter()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')

    if (!code) {
      console.error('인가 코드가 없습니다.')
      return
    }

    // 1) 백엔드로 code 보내서 토큰 받고…
    fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/oauth/kakao/callback?code=${code}`,
      { credentials: 'include' }
    )
      .then(() => router.replace('/dashboard'))
      .catch(console.error)
  }, [router])

  return <p>로그인 처리 중… 잠시만 기다려 주세요.</p>
}
