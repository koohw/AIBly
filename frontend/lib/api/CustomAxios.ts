import axios, { AxiosInstance } from 'axios';
import { Preferences } from '@capacitor/preferences';

const API_URL = process.env.NEXT_PUBLIC_API_URL;

/**
 * 앱 전역에서 사용할 Axios 인스턴스
 * Preferences에서 AUTH_TOKEN을 비동기로 읽어와 Authorization 헤더에 추가
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: 저장소에서 토큰을 가져와 설정
apiClient.interceptors.request.use(
  async (config) => {
    try {
      // Capacitor Preferences에서 키로 조회
      const { value: token } = await Preferences.get({ key: 'AUTH_TOKEN' });
      
      // 토큰 상태 로깅
      if (!token) {
        console.warn('⚠️ 인증 토큰이 없습니다. 로그인이 필요할 수 있습니다.');
      } else {
        // 토큰 유효성 간단 확인 (JWT 형식 확인)
        const isValidFormat = token.split('.').length === 3;
        if (!isValidFormat) {
          console.warn('⚠️ 토큰 형식이 유효하지 않습니다:', token.substring(0, 15) + '...');
        } else {
          console.log('✅ 유효한 형식의 토큰이 설정됨:', token.substring(0, 10) + '...');
        }
      }
      
      // 헤더에 토큰 설정
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      
      // 디버깅용 로그
      console.log(`🌐 API 요청: ${config.method?.toUpperCase()} ${config.url}`);
      console.log('요청 헤더:', JSON.stringify(config.headers));
      if (config.params) {
        console.log('요청 파라미터:', config.params);
      }
      
      return config;
    } catch (error) {
      console.error('API 요청 인터셉터 오류:', error);
      // 오류가 발생해도 요청은 계속 진행
      return config;
    }
  },
  (error) => {
    console.error('API 요청 인터셉터 오류:', error);
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 응답 및 오류 로깅
apiClient.interceptors.response.use(
  (response) => {
    console.log(`✅ API 응답 성공: ${response.config.method?.toUpperCase()} ${response.config.url}`);
    console.log('응답 상태:', response.status);
    return response;
  },
  (error) => {
    if (error.response) {
      // 서버가 응답을 반환했지만 2xx 범위가 아닌 경우
      console.error('🚨 API 요청 실패:', error.response.status, error.response.config.url);
      console.error('응답 데이터:', error.response.data);
      console.error('응답 헤더:', error.response.headers);
    } else if (error.request) {
      // 요청이 이루어졌으나 응답을 받지 못한 경우
      console.error('🚨 API 응답 없음:', error.request);
    } else {
      // 요청 설정 중 오류 발생
      console.error('🚨 API 요청 설정 오류:', error.message);
    }
    
    return Promise.reject(error);
  }
);
