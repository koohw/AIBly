// lib/api/User.ts

import { Preferences } from '@capacitor/preferences'
import type { AxiosError } from 'axios'
import { apiClient } from './CustomAxios'

/** 백엔드에서 내려주는 유저 정보 타입 */
export interface UserInfo {
  name: string
  email: string
  createdAt: string
}

/**
 * 내 정보 조회
 * - JWT 토큰은 Preferences에서 꺼내와서 Authorization 헤더에 담아 보냄
 * @throws {Error} 404 등 에러 메시지를 담은 Error
 */
export async function getUserMe(): Promise<UserInfo> {
  // 1) 저장된 토큰 꺼내오기
  const { value: token } = await Preferences.get({ key: 'AUTH_TOKEN' })
  if (!token) {
    throw new Error('로그인 정보가 없습니다.')
  }

  try {
    // 2) API 호출
    const res = await apiClient.get<UserInfo>(
      '/api/user/me',
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    )
    console.log('사용자 정보', res.data)
    return res.data
  } catch (e) {
    const err = e as AxiosError<{ status: number; message: string }>
    // 3) 404 등 서버 에러 메시지를 그대로 던져주기
    if (err.response?.data?.message) {
      throw new Error(err.response.data.message)
    }
    throw err
  }
}
