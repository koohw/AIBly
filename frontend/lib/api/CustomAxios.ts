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
    // Capacitor Preferences에서 키로 조회
    const { value: token } = await Preferences.get({ key: 'AUTH_TOKEN' });
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
