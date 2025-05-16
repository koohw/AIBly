import { Capacitor } from '@capacitor/core';
import { registerPlugin } from '@capacitor/core';

// FCM 토큰 플러그인 인터페이스
interface FcmTokenPlugin {
  registerFcmToken(options: { authToken: string }): Promise<{ success: boolean, message: string }>;
}

// FCM 토큰 플러그인 등록
const FcmToken = registerPlugin<FcmTokenPlugin>('FcmToken');

// FCM 토큰 등록 함수
// lib/api/Fcm.ts
export async function registerFcmToken(authToken: string): Promise<void> {
  console.log('🔴🔴🔴 FCM 토큰 등록 함수 호출됨', authToken.substring(0, 10) + '...');
  
  try {
    // Capacitor 플러그인 존재 확인
    if (!Capacitor.isPluginAvailable('FcmToken')) {
      console.error('🔴🔴🔴 FCM 토큰 플러그인을 사용할 수 없음');
      return;
    }
    
    console.log('🔴🔴🔴 FCM 토큰 플러그인 사용 가능, 메소드 호출 시도');
    const result = await FcmToken.registerFcmToken({ authToken });
    console.log('🔴🔴🔴 FCM 토큰 등록 결과:', result);
  } catch (error) {
    console.error('🔴🔴🔴 FCM 토큰 등록 심각한 오류:', error);
  }
}