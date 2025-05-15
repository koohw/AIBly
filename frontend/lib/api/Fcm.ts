import { Capacitor } from '@capacitor/core';
import { registerPlugin } from '@capacitor/core';

// FCM 토큰 플러그인 인터페이스
interface FcmTokenPlugin {
  registerFcmToken(options: { authToken: string }): Promise<{ success: boolean, message: string }>;
}

// FCM 토큰 플러그인 등록
const FcmToken = registerPlugin<FcmTokenPlugin>('FcmToken');

// FCM 토큰 등록 함수
export async function registerFcmToken(authToken: string): Promise<void> {
  try {
    console.log('[FCM] 토큰 등록 시도', authToken.substring(0, 10) + '...');
    
    // Capacitor 플러그인 호출
    const result = await FcmToken.registerFcmToken({ authToken });
    console.log('[FCM] 토큰 등록 결과:', result);
  } catch (error) {
    console.error('[FCM] 토큰 등록 오류:', error);
  }
}