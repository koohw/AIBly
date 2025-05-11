// capacitor-kakao-login-plugin.d.ts
declare module "capacitor-kakao-login-plugin" {
    /** 웹뷰(하이브리드)에서 한 번만 호출 */
    export function initForWeb(appKey: string): Promise<void>;
    /** 카카오톡 로그인 (앱/웹뷰) */
    export function goLogin(): Promise<{ accessToken: string }>;
    /** 로그아웃 */
    export function goLogout(): Promise<void>;
    /** 사용자 정보 조회 */
    export function getUserInfo(): Promise<any>;
    /** 카카오톡 채널 메시지 보내기 */
    export function talkInChannel(options: any): Promise<any>;
    // 필요한 함수가 더 있으면 여기에 선언 추가
  }
  