import { apiClient } from '../api/CustomAxios';
import type { AxiosError } from 'axios';

/**
 * FCM 토큰 관리를 위한 단일 저장소
 * 메인 페이지와 로그인 페이지 등 여러 곳에서 일관된 방식으로 토큰을 관리하기 위한 유틸리티
 */
class FcmTokenStore {
  private readonly TOKEN_KEY = 'fcm_token';
  private readonly REGISTERED_KEY = 'fcm_token_registered';
  private readonly REGISTER_ATTEMPT_KEY = 'fcm_register_attempt';

  /**
   * 저장된 FCM 토큰 반환
   */
  getToken(): string {
    return localStorage.getItem(this.TOKEN_KEY) || '';
  }

  /**
   * FCM 토큰 저장
   */
  setToken(token: string): void {
    if (!token) return;
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  /**
   * 토큰이 서버에 등록되었는지 여부 반환
   */
  isRegistered(): boolean {
    return localStorage.getItem(this.REGISTERED_KEY) === 'true';
  }

  /**
   * 토큰 등록 상태 설정
   */
  setRegistered(registered: boolean): void {
    localStorage.setItem(this.REGISTERED_KEY, registered ? 'true' : 'false');
  }

  /**
   * 마지막 등록 시도 시간 저장
   */
  setRegisterAttempt(): void {
    localStorage.setItem(this.REGISTER_ATTEMPT_KEY, Date.now().toString());
  }

  /**
   * 최근 등록 시도 후 일정 시간(ms)이 지났는지 확인
   */
  canAttemptRegister(cooldownMs = 5000): boolean {
    const lastAttempt = localStorage.getItem(this.REGISTER_ATTEMPT_KEY);
    if (!lastAttempt) return true;
    
    const elapsed = Date.now() - parseInt(lastAttempt);
    return elapsed > cooldownMs;
  }

  /**
   * 네이티브 브릿지를 통해 FCM 토큰 등록 시도
   */
  async registerTokenViaAndroidBridge(): Promise<boolean> {
    try {
      // 네이티브 브릿지가 있는지 확인
      if (window.androidFcmBridge) {
        console.log('🔥 네이티브 브릿지를 통해 FCM 토큰 등록 시도');
        const result = await window.androidFcmBridge.registerFcmToken();
        
        // 네이티브 응답 처리
        if (result && typeof result === 'string') {
          if (!result.startsWith('error:')) {
            console.log('✅ 네이티브 브릿지 FCM 토큰 등록 성공');
            this.setRegistered(true);
            return true;
          } else {
            console.warn('⚠️ 네이티브 브릿지 FCM 토큰 등록 실패:', result);
          }
        } else {
          console.log('✅ 네이티브 브릿지 FCM 토큰 등록 성공 (비문자열 응답)');
          this.setRegistered(true);
          return true;
        }
      } else {
        console.log('🔍 네이티브 FCM 브릿지 없음, 웹 방식으로 등록 시도');
      }
    } catch (error) {
      console.error('❌ 네이티브 브릿지 FCM 토큰 등록 오류:', error);
    }
    
    return false;
  }

  /**
   * 서버에 직접 FCM 토큰 등록 시도
   */
  async registerTokenToServer(token: string): Promise<boolean> {
    if (!token) {
      console.warn('⚠️ 등록할 FCM 토큰이 없습니다');
      return false;
    }

    try {
      this.setRegisterAttempt();
      console.log('🔄 서버에 FCM 토큰 등록 시도');
      
      // 서버에 토큰 등록
      const response = await apiClient.post('/api/auth/devices/fcm', { token });
      
      if (response.status === 200 || response.status === 201) {
        console.log('✅ FCM 토큰 서버 등록 성공');
        this.setToken(token);
        this.setRegistered(true);
        return true;
      } else {
        console.warn('⚠️ FCM 토큰 서버 등록 실패:', response.status);
      }
    } catch (error) {
      const err = error as AxiosError;
      console.error('❌ FCM 토큰 서버 등록 오류:', err.response?.data || err.message);
    }
    
    return false;
  }

  /**
   * 전체 FCM 토큰 등록 프로세스 실행
   * 브릿지 시도 → 실패 시 서버 직접 등록
   */
  async registerToken(): Promise<boolean> {
    // 이미 등록되었거나 쿨다운 중이면 스킵
    if (this.isRegistered() || !this.canAttemptRegister()) {
      return this.isRegistered();
    }

    // 네이티브 브릿지 시도
    const bridgeSuccess = await this.registerTokenViaAndroidBridge();
    if (bridgeSuccess) return true;

    // 브릿지 실패 시 저장된 토큰이 있으면 서버 직접 등록
    const token = this.getToken();
    if (token) {
      return this.registerTokenToServer(token);
    }

    return false;
  }

  /**
   * 저장소 초기화 (로그아웃 시 호출)
   */
  clear(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REGISTERED_KEY);
    localStorage.removeItem(this.REGISTER_ATTEMPT_KEY);
  }
}

// 싱글톤 인스턴스 생성
const fcmTokenStore = new FcmTokenStore();

// 전역 인터페이스는 Fcm.ts에서 이미 정의되어 있으므로 여기서는 생략

export default fcmTokenStore; 