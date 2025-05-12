'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { App } from '@capacitor/app'
// ↓ 변경된 부분: @capacitor/storage → @capacitor/preferences
import { Preferences } from '@capacitor/preferences'

export default function DeepLink() {
  const router = useRouter()

  useEffect(() => {
     const handler = async (event: any) => {
      const { url } = event
      console.log('[DeepLink] appUrlOpen:', url)

      const scheme = process.env.NEXT_PUBLIC_APP_SCHEME
      if (!url.startsWith(`${scheme}://oauth/kakao`)) {
        return
      }

      let code: string | null = null
      try {
        const parsed = new URL(url)
        code = parsed.searchParams.get('code')
      } catch (e) {
        console.error('[DeepLink] URL 파싱 실패', e)
        return
      }

      if (!code) {
        console.error('[DeepLink] authorization code 가 없습니다')
        return
      }

      try {
        // 1) 인가 코드 교환
        const api = process.env.NEXT_PUBLIC_API_URL
        const res = await fetch(`${api}/oauth/kakao/callback`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ code }),
        })
        if (!res.ok) throw new Error(`HTTP ${res.status}`)

        // 2) JSON { token: "..." } 형태로 응답받았다 치고
        const { token } = await res.json()

        // 3) Preferences 를 이용해 네이티브 저장소에 토큰 저장
        await Preferences.set({ key: 'AUTH_TOKEN', value: token })

        // 4) 대시보드로 이동
        router.replace('/dashboard')
      } catch (e) {
        console.error('[DeepLink] 로그인 완료 후 처리 실패', e)
      }
    }

    // 리스너 등록
    const listenerPromise = App.addListener('appUrlOpen', handler)

    return () => {
      // 리스너 해제
      listenerPromise.then((listener) => listener.remove())
    }
  }, [router])

  return null
}
