"use client"

import { useState, useEffect } from "react"
import LoadingAnimation from "./components/loading-animation"
import AuthScreen from "./components/auth-screen"
import { useRouter } from "next/navigation"

export default function Home() {
  const [isLoading, setIsLoading] = useState(true)
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  const router = useRouter()

  // 로딩 화면 표시 (실제로는 인증 상태 확인 등의 작업을 수행)
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoading(false)
    }, 3000) // 3초 후 로딩 화면 종료

    return () => clearTimeout(timer)
  }, [])

  // 로그인 상태 확인 (실제로는 토큰 검증 등의 작업을 수행)
  useEffect(() => {
    // 로딩이 끝난 후에만 확인
    if (!isLoading) {
      // 예시: 로컬 스토리지에서 토큰 확인
      const token = localStorage.getItem("auth_token")
      if (token) {
        setIsLoggedIn(true)
        // 로그인된 상태면 대시보드로 리다이렉트
        router.push("/dashboard")
      }
    }
  }, [isLoading, router])

  if (isLoading) {
    return <LoadingAnimation />
  }

  if (!isLoggedIn) {
    return <AuthScreen />
  }

  // 로그인된 상태의 메인 화면 (추후 구현)
  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-6 bg-black text-white">
      <h1 className="text-2xl font-bold mb-4">블랙박스 리뷰</h1>
      <p>로그인 성공! 메인 화면 구현 예정</p>
    </div>
  )
}
