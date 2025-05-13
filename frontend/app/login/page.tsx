"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import Image from "next/image"
import { ArrowLeft } from "lucide-react"
import { Button } from "@/components/ui/button"
// pages/login.tsx 혹은 해당 파일
// import KakaoLogin from "capacitor-kakao-login-plugin"
// - import KakaoLogin from "capacitor-kakao-login-plugin"
// import { initForWeb, goLogin } from "capacitor-kakao-login-plugin"
// import  { KakaoLogin } from "@/lib/kakao"
// import { initForWeb, goLogin } from "capacitor-kakao-login-plugin"
// import { registerPlugin } from '@capacitor/core'

export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()

//   useEffect(() => {
//      // .env.local 에 NEXT_PUBLIC_KAKAO_JS_KEY=your_web_app_key 를 설정해야 합니다.
//      KakaoLogin.initForWeb(process.env.NEXT_PUBLIC_KAKAO_JS_KEY!)
//      .catch((err) => console.error("Kakao SDK init 실패:", err))
//  }, [])
  

  // const handleKakaoLogin = async () => {
  //   setIsLoading(true)
  //   try {
  //     // 앱이 설치돼 있으면 카톡 앱, 없으면 웹뷰 창으로 로그인
  //     const result = await KakaoLogin.goLogin()

  //     // 받은 AccessToken을 백엔드에 전달
  //     const res = await fetch("/api/auth/kakao/mobile", {
  //       method: "POST",
  //       headers: { "Content-Type": "application/json" },
  //       body: JSON.stringify({ accessToken: result.accessToken }),
  //     })
  //     if (!res.ok) throw new Error("서버 로그인 실패")

  //     // 백엔드에서 내려준 JWT 등을 저장
  //     const { token } = await res.json()
  //     localStorage.setItem("auth_token", token)
  //     localStorage.removeItem("guest_token") // 게스트 토큰 제거

  //      // 로그인 후 대시보드로 이동
  //      router.push("/dashboard")
  //     } catch (error) {
  //       console.error("카카오 로그인 실패:", error)
  //       // TODO: 사용자에게 실패 알림 UI 추가
  //     } finally {
  //       setIsLoading(false)
  //     }
  //   }

  // const handleKakaoLogin = async () => {
  //   setIsLoading(true)
  //   try {
  //     // 1) 카톡 앱이 있으면 앱, 없으면 웹뷰로 로그인
  //     const result = await goLogin()
  //     const kakaoToken = result.accessToken

  //     // 2) 백엔드 로그인 엔드포인트 주소를 정확히: 
  //     //    - 개발 서버: http://localhost:8001
  //     //    - 그리고 우리가 만든 컨트롤러가 /api/auth/kakao/login 인지 확인
  //     const res = await fetch("http://localhost:8001/api/auth/kakao/login", {
  //       method: "POST",
  //       headers: { "Content-Type": "application/json" },
  //       body: JSON.stringify({ accessToken: kakaoToken }),
  //     })
  //     if (!res.ok) throw new Error("서버 로그인 실패")

  //     // 3) 백엔드에서 내려준 JWT 토큰 꺼내기
  //     const { token /*, expiresIn */ } = await res.json()

  //     // ⭐️ 웹뷰만 쓴다면 localStorage 도 가능하지만,
  //     //    모바일 앱이라면 Capacitor Storage 권장
  //     localStorage.setItem("auth_token", token)
  //     // await Storage.set({ key: "auth_token", value: token })

  //     // 4) 로그인 후 화면 이동
  //     router.push("/dashboard")
  //   } catch (error) {
  //     console.error("카카오 로그인 실패:", error)
  //     // TODO: 사용자에게 실패 알림 띄우기
  //   } finally {
  //     setIsLoading(false)
  //   }
  // }

  const handleKakaoLogin = () => {
    setIsLoading(true)
    // 프론트는 단순히 이 엔드포인트로 이동만 시켜주면 됩니다.
    window.location.href = "https://k12e203.p.ssafy.io/api/auth/kakao"
  }

  const handleBack = () => {
    router.back()
  }

  return (
    <div className="min-h-screen bg-black text-white flex flex-col">
      {/* 헤더 */}
      <header className="p-4 border-b border-gray-800 flex items-center">
        <Button variant="ghost" size="icon" onClick={handleBack} className="mr-2">
          <ArrowLeft size={20} />
        </Button>
        <h1 className="text-xl font-bold">로그인</h1>
      </header>

      {/* 메인 콘텐츠 */}
      <main className="flex-1 flex flex-col items-center justify-center p-6">
        <div className="w-full max-w-md space-y-8">
          {/* 로고 */}
          <div className="text-center mb-8">
            <div className="inline-block p-4 rounded-full bg-gray-900 mb-4">
              <div className="w-16 h-16 rounded-full bg-red-600 flex items-center justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="32"
                  height="32"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M12 13V2l8 4-8 4" />
                  <path d="M20.55 10.23A9 9 0 1 1 8 4.94" />
                  <path d="M8 10a5 5 0 1 0 8.9 2.02" />
                </svg>
              </div>
            </div>
            <h2 className="text-2xl font-bold">블랙박스 리뷰</h2>
            <p className="text-gray-400 mt-2">로그인하여 모든 기능을 이용하세요</p>
          </div>

          {/* 로그인 버튼 */}
          <div className="space-y-4">
            <button
              onClick={handleKakaoLogin}
              disabled={isLoading}
              className="w-full py-4 flex items-center justify-center space-x-2 text-black font-medium rounded-md bg-[#FEE500] hover:bg-[#FDD835] transition-colors"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <Image src="/kakao-logo.png" alt="Kakao" width={24} height={24} className="mr-2" />
                  카카오톡으로 로그인하기
                </>
              )}
            </button>

            <div className="text-center">
              <p className="text-sm text-gray-500">
                로그인하면 블랙박스 리뷰의{" "}
                <span className="text-gray-400 underline cursor-pointer">서비스 이용약관</span>과{" "}
                <span className="text-gray-400 underline cursor-pointer">개인정보 처리방침</span>에 동의하게 됩니다.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
