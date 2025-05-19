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

    // FCM 토큰이 이미 등록되었는지 확인
    const registeredValue = await Preferences.get({ key: 'fcm_token_registered' });
    if (registeredValue.value === 'true') {
      console.log('✅ FCM 토큰이 이미 등록되어 있습니다');
      return;
    }

    // 실제 FCM 토큰 등록 구현
    try {
      // 백엔드 URL 설정
      const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || 'https://k12e203.p.ssafy.io/api';
      console.log('🔄 FCM 토큰 등록 시작 - 백엔드 URL:', backendUrl);
      
      // 1. 인증 토큰 저장 - 네이티브 코드에서 사용할 수 있도록
      await Preferences.set({ key: 'AUTH_TOKEN', value: authToken });
      
      // 2. 네이티브 FcmTokenManager 호출
      if (window.androidFcmBridge) {
        console.log('🔄 네이티브 FCM 브릿지 발견, 토큰 등록 호출 중...');
        const result = await window.androidFcmBridge.registerFcmToken();
        console.log('✅ 네이티브 FCM 토큰 등록 결과:', result);
        
        // 성공 표시 저장
        await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
        console.log('✅ FCM 토큰 등록 상태 저장됨');
        return;
      }
      
      // 3. 안드로이드용 백업 방식 - MainActivity의 static 메서드를 호출
      if (window.MainActivity && window.MainActivity.registerFcmToken) {
        console.log('🔄 MainActivity 브릿지 발견, FCM 토큰 등록 중...');
        const result = await window.MainActivity.registerFcmToken();
        console.log('✅ MainActivity FCM 토큰 등록 결과:', result);
        
        // 성공 표시 저장
        await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
        console.log('✅ FCM 토큰 등록 상태 저장됨');
        return;
      }
      
      // 4. 백엔드 직접 호출 방식 - 마지막 방법
      console.log('🔄 직접 FCM 토큰 획득 시도 중...');
      let fcmToken = null;
      
      // Firebase Messaging 직접 호출 시도
      if (window.firebase && window.firebase.messaging) {
        try {
          const messaging = window.firebase.messaging();
          fcmToken = await messaging.getToken();
          console.log('✅ Firebase SDK로 FCM 토큰 획득:', fcmToken.substring(0, 10) + '...');
        } catch (firebaseError) {
          console.error('❌ Firebase SDK 토큰 획득 실패:', firebaseError);
        }
      }
      
      // FCM 토큰을 획득했다면 직접 백엔드에 등록
      if (fcmToken) {
        console.log('🔄 백엔드에 FCM 토큰 등록 요청 중...');
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
    } catch (directError) {
      console.error('❌ FCM 토큰 등록 오류:', directError);
      
      // 오류를 상위로 전파
      throw directError;
    }
    
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

// 타입 정의
declare global {
  interface Window {
    androidFcmBridge?: {
      registerFcmToken(): Promise<any>;
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