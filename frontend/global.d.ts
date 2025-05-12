// global.d.ts
export {}      // 이 파일을 모듈로 만듭니다

declare global {
  interface Window {
    Kakao: {
      init: (jsKey: string) => void
      isInitialized: () => boolean
      Auth: {
        authorize: (options: { redirectUri: string }) => void
      }
      // 필요하다면 추가 메서드도 선언해 주세요
    }
  }
}
