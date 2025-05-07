// lib/kakao.ts
import { registerPlugin } from '@capacitor/core'
import type { KakaoLoginPlugin } from 'capacitor-kakao-login-plugin'

// 'capacitor-kakao-login-plugin' 은 네이티브 쪽 pluginId 와 반드시 일치해야 합니다.
// (보통 plugin.xml 혹은 build.gradle 의 id 속성 확인)
export const KakaoLogin = registerPlugin<KakaoLoginPlugin>(
  'capacitor-kakao-login-plugin',
  {
    web: async () => {
      const m = await import("capacitor-kakao-login-plugin/dist/esm/web")
      return new m.KakaoLoginWeb()
    }
  }
)
