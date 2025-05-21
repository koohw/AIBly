// Fcm.ts
import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';

export async function registerFcmToken(authToken: string): Promise<void> {
  console.log('🔴🔴🔴 FCM 토큰 등록 함수 호출됨', authToken.substring(0, 10) + '...');
  
  try {
    // 네이티브 플랫폼 체크
    if (!Capacitor.isNativePlatform()) {
      console.log('🔴🔴🔴 FCM 토큰 플러그인을 사용할 수 없음 (네이티브 플랫폼이 아님)');
      return;
    }

    // 저장된 FCM 토큰 가져오기
    const { value: storedFcmToken } = await Preferences.get({ key: 'fcm_token' });
    
    // 네이티브 앱에서 토큰을 읽을 수 있도록 인증 토큰 저장
    // 이 때 AUTH_TOKEN을 대문자로 정확히 저장하는 것이 중요
    await Preferences.remove({ key: 'AUTH_TOKEN' });
    await new Promise(resolve => setTimeout(resolve, 300));
    await Preferences.set({ key: 'AUTH_TOKEN', value: authToken });

    // 토큰이 저장되었는지 확인
    const { value: savedToken } = await Preferences.get({ key: 'AUTH_TOKEN' });
    console.log('✅ 저장된 인증 토큰 확인:', savedToken ? (savedToken.substring(0, 10) + '...') : '없음');
    
    // 1. FCM 토큰 획득 및 비교 로직 
    let currentFcmToken = null;
    let tokenChanged = false;
    
    // 네이티브 브릿지 메서드 호출 시도 (현재 FCM 토큰 가져오기)
    if (window.androidFcmBridge && typeof window.androidFcmBridge.getFcmToken === 'function') {
      try {
        currentFcmToken = await window.androidFcmBridge.getFcmToken();
        console.log('✅ 현재 FCM 토큰 획득:', currentFcmToken ? (currentFcmToken.substring(0, 10) + '...') : '없음');
        
        // 토큰 변경 여부 확인
        if (currentFcmToken && storedFcmToken && currentFcmToken !== storedFcmToken) {
          console.log('🔄 FCM 토큰이 변경되었습니다. 갱신이 필요합니다.');
          tokenChanged = true;
        } else if (!storedFcmToken && currentFcmToken) {
          console.log('🔄 저장된 FCM 토큰이 없습니다. 새로 등록합니다.');
          tokenChanged = true;
        }
      } catch (error) {
        console.error('❌ FCM 토큰 획득 오류:', error);
      }
    }

    // FCM 토큰이 이미 등록되었는지 확인
    const { value: fcmRegistered } = await Preferences.get({ key: 'fcm_token_registered' });
    if (fcmRegistered === 'true' && !tokenChanged) {
      console.log('✅ FCM 토큰이 이미 등록되어 있고 변경되지 않았습니다.');
      return;
    }

    // 2. 네이티브 FCM 토큰 등록 호출
    if (window.androidFcmBridge) {
      console.log('🔄 네이티브 FCM 브릿지 발견, 토큰 등록 호출 중...');
      
      try {
        // 안드로이드 브릿지에서는 registerFcmToken() 메서드만 구현되어 있음
        const result = await window.androidFcmBridge.registerFcmToken();
        console.log('✅ 네이티브 FCM 토큰 등록 결과:', result);
        
        // 성공 시 토큰 등록 상태 및 현재 토큰 저장
        await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
        if (currentFcmToken) {
          await Preferences.set({ key: 'fcm_token', value: currentFcmToken });
        }
        console.log('✅ FCM 토큰 등록 상태 저장됨');
        return;
      } catch (bridgeError) {
        console.error('❌ 네이티브 브릿지 호출 오류:', bridgeError);
        throw bridgeError;
      }
    }
      
    // MainActivity를 통한 메서드 호출 시도
    if (window.MainActivity && typeof window.MainActivity.registerFcmToken === 'function') {
      console.log('🔄 MainActivity 브릿지 발견, FCM 토큰 등록 중...');
      
      try {
        const result = await window.MainActivity.registerFcmToken();
        console.log('✅ MainActivity FCM 토큰 등록 결과:', result);
        
        // 성공 표시 저장
        await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
        console.log('✅ FCM 토큰 등록 상태 저장됨');
        return;
      } catch (mainActivityError) {
        console.error('❌ MainActivity 브릿지 호출 오류:', mainActivityError);
        throw mainActivityError;
      }
    }
      
    // 백엔드 직접 호출 방식 - 마지막 방법
    console.log('🔄 직접 FCM 토큰 획득 시도 중...');
    let fcmToken = null;
      
    // Firebase Messaging 직접 호출 시도
    if (window.firebase && window.firebase.messaging) {
      try {
        const messaging = window.firebase.messaging();
        fcmToken = await messaging.getToken();
        console.log('✅ Firebase SDK로 FCM 토큰 획득:', fcmToken.substring(0, 10) + '...');
          
        // 토큰 변경 여부 확인
        if (storedFcmToken && fcmToken !== storedFcmToken) {
          console.log('🔄 FCM 토큰이 변경되었습니다. 갱신이 필요합니다.');
          tokenChanged = true;
        }
      } catch (firebaseError) {
        console.error('❌ Firebase SDK 토큰 획득 실패:', firebaseError);
      }
    }
      
    // FCM 토큰을 획득했다면 직접 백엔드에 등록
    if (fcmToken) {
      console.log('🔄 백엔드에 FCM 토큰 등록 요청 중...');
      const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || 'https://k12e203.p.ssafy.io/api';
      const response = await fetch(`${backendUrl}/api/fcm/token`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({ fcmToken: fcmToken })
      });
        
      if (response.ok) {
        console.log('✅✅✅ FCM 토큰 백엔드 등록 성공:', response.status);
          
        // 성공 상태 저장
        await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
        console.log('✅ FCM 토큰 등록 상태 저장됨');
          
        // FCM 토큰도 저장
        await Preferences.set({ key: 'fcm_token', value: fcmToken });
        return;
      } else {
        console.error('❌ FCM 토큰 백엔드 등록 실패:', response.status);
        throw new Error(`FCM 토큰 등록 실패: ${response.status}`);
      }
    }
      
    throw new Error('FCM 토큰을 획득할 수 있는 방법이 없습니다.');
    
  } catch (error) {
    console.error('❌❌❌ FCM 토큰 등록 프로세스 실패:', error);
    
    if (error instanceof Error) {
      console.error('에러 이름:', error.name);
      console.error('에러 메시지:', error.message);
    }
    
    // 오류를 상위로 전파
    throw error;
  }
}

// FCM 토큰 변경 감지 함수
export async function checkFcmTokenChanges(): Promise<boolean> {
  try {
    if (!Capacitor.isNativePlatform()) {
      return false;
    }
    
    // 저장된 FCM 토큰 가져오기
    const { value: storedFcmToken } = await Preferences.get({ key: 'fcm_token' });
    
    // 현재 FCM 토큰 가져오기
    let currentFcmToken = null;
    
    if (window.androidFcmBridge && typeof window.androidFcmBridge.getFcmToken === 'function') {
      try {
        currentFcmToken = await window.androidFcmBridge.getFcmToken();
      } catch (error) {
        console.error('❌ FCM 토큰 획득 오류:', error);
      }
    }
    
    // 토큰 변경 여부 확인
    if (currentFcmToken && storedFcmToken && currentFcmToken !== storedFcmToken) {
      console.log('🔄 FCM 토큰이 변경되었습니다:', 
        storedFcmToken.substring(0, 10) + '... ➡️ ' + currentFcmToken.substring(0, 10) + '...');
      return true;
    }
    
    return false;
  } catch (error) {
    console.error('토큰 변경 감지 오류:', error);
    return false;
  }
}

// 타입 정의
declare global {
  interface Window {
    androidFcmBridge?: {
      registerFcmToken(): Promise<any>;
      getFcmToken?(): Promise<string>;
    };
    MainActivity?: {
      registerFcmToken(): Promise<any>;
    };
    firebase?: {
      messaging: () => {
        getToken(): Promise<string>;
      };
    };
  }
}