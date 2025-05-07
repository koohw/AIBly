"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Upload, Camera, Clock, FileText, AlertCircle, User, ChevronRight } from "lucide-react"
import LoginRequiredModal from "../components/login-required-modal"
import { useTheme } from "../contexts/theme-context"

export default function Dashboard() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(true)
  const [isGuest, setIsGuest] = useState(false)
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [hasAnalysis, setHasAnalysis] = useState(false) // 분석 결과가 있는지 여부
  const [autoDetectEnabled, setAutoDetectEnabled] = useState(true) // 자동 감지 활성화 여부
  const { theme } = useTheme()

  // 인증 상태 확인
  useEffect(() => {
    const token = localStorage.getItem("auth_token")
    const guestToken = localStorage.getItem("guest_token")

    if (!token && !guestToken) {
      router.push("/")
    } else {
      setIsLoading(false)
      setIsGuest(!token && !!guestToken)

      // 로그인 사용자는 예시 분석 결과 표시
      if (token) {
        setHasAnalysis(true)
      }

      // localStorage에서 자동 감지 설정 불러오기
      const savedAutoDetect = localStorage.getItem("auto_detect")
      if (savedAutoDetect !== null) {
        setAutoDetectEnabled(savedAutoDetect === "true")
      }
    }
  }, [router])

  const handleLogout = () => {
    localStorage.removeItem("auth_token")
    localStorage.removeItem("guest_token")
    router.push("/")
  }

  const handleLogin = () => {
    // 로그인 페이지로 이동
    router.push("/login")
  }

  const handleProfileClick = () => {
    if (isGuest) {
      router.push("/login")
    } else {
      // 로그인된 사용자는 프로필 페이지로 이동
      router.push("/profile")
    }
  }

  const handleUpload = () => {
    if (isGuest) {
      // 게스트 사용자는 로그인 필요 모달 표시
      setShowLoginModal(true)
    } else {
      // 로그인 사용자는 업로드 페이지로 이동
      router.push("/upload")
    }
  }

  const handleViewAnalysis = () => {
    router.push("/analysis/1") // 예시 분석 결과 페이지로 이동
  }

  const handleAutoDetectSettings = () => {
    router.push("/profile")
  }

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="w-8 h-8 border-4 border-appblue border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground pb-16">
      {/* 헤더 */}
      <header className="app-header">
        <h1 className="text-xl font-bold">블랙박스 리뷰</h1>
        {isGuest ? (
          <Button variant="outline" size="sm" onClick={handleLogin} className="bg-white text-appblue border-none">
            로그인
          </Button>
        ) : (
          <Button variant="ghost" size="sm" onClick={handleLogout} className="text-white hover:bg-white/20">
            로그아웃
          </Button>
        )}
      </header>

      {/* 게스트 모드 알림 */}
      {isGuest && (
        <div className="mx-4 mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700/50 rounded-md flex items-start">
          <AlertCircle className="text-yellow-500 mr-2 flex-shrink-0 mt-0.5" size={16} />
          <div className="text-sm">
            <p className="text-yellow-700 dark:text-yellow-500 font-medium">게스트 모드로 접속 중입니다</p>
            <p className="text-yellow-600/70 dark:text-yellow-500/70 text-xs">모든 기능을 사용하려면 로그인하세요</p>
          </div>
        </div>
      )}

      {/* 메인 콘텐츠 */}
      <main className="app-container">
        {/* 자동 감지 상태 표시 */}
        <section className="mb-6 app-card p-4">
          <div className="flex items-center">
            {autoDetectEnabled ? (
              <div className="w-3 h-3 bg-green-500 rounded-full mr-2 relative">
                <span className="absolute w-3 h-3 bg-green-500 rounded-full animate-ping opacity-75"></span>
              </div>
            ) : (
              <div className="w-3 h-3 bg-gray-400 rounded-full mr-2"></div>
            )}
            <div className="flex-1">
              <h3 className="font-medium">자동 감지 {autoDetectEnabled ? "활성화됨" : "비활성화됨"}</h3>
              <p className="text-xs text-muted-foreground">
                {autoDetectEnabled
                  ? "블랙박스 영상이 감지되면 자동으로 분석합니다"
                  : "블랙박스 영상을 수동으로 업로드해야 합니다"}
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="text-xs text-appblue hover:text-appblue-dark"
              onClick={handleAutoDetectSettings}
            >
              설정
            </Button>
          </div>
        </section>

        <section className="mb-8">
          <h2 className="app-section-title">영상 분석하기</h2>
          <div className="grid grid-cols-2 gap-4">
            <Button
              variant="outline"
              className="h-32 flex flex-col items-center justify-center space-y-2 border-border bg-card hover:border-appblue hover:bg-appblue/5"
              onClick={handleUpload}
            >
              <div className="w-12 h-12 rounded-full bg-appblue flex items-center justify-center">
                <Upload size={24} className="text-white" />
              </div>
              <span>영상 업로드</span>
            </Button>
            <Button
              variant="outline"
              className="h-32 flex flex-col items-center justify-center space-y-2 border-border bg-card hover:border-appblue hover:bg-appblue/5"
              onClick={handleUpload}
            >
              <div className="w-12 h-12 rounded-full bg-appblue flex items-center justify-center">
                <Camera size={24} className="text-white" />
              </div>
              <span>카메라 촬영</span>
            </Button>
          </div>
        </section>

        <section>
          <div className="flex justify-between items-center mb-4">
            <h2 className="app-section-title">최근 분석</h2>
            <Button variant="link" className="text-appblue p-0 hover:text-appblue-dark">
              모두 보기
            </Button>
          </div>

          {hasAnalysis ? (
            <div className="app-card overflow-hidden">
              <div className="p-4 border-b border-border flex items-center cursor-pointer" onClick={handleViewAnalysis}>
                <div className="w-16 h-16 bg-muted rounded-md mr-3 flex-shrink-0 overflow-hidden">
                  <img
                    src="/car-accident-aftermath.png"
                    alt="사고 영상 썸네일"
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="flex-1">
                  <h3 className="font-medium">교차로 신호위반 사고</h3>
                  <p className="text-xs text-muted-foreground">2025년 4월 15일 오후 2:30</p>
                  <div className="flex mt-1">
                    <span className="text-xs bg-appblue/20 text-appblue px-2 py-0.5 rounded mr-1">과실비율 30:70</span>
                    <span className="text-xs bg-muted text-muted-foreground px-2 py-0.5 rounded">신호위반</span>
                  </div>
                </div>
                <ChevronRight size={20} className="text-muted-foreground" />
              </div>
            </div>
          ) : (
            <div className="app-card p-6 text-center">
              <div className="w-16 h-16 rounded-full bg-appblue mx-auto mb-4 flex items-center justify-center">
                <Clock className="text-white" size={32} />
              </div>
              <h3 className="text-lg font-medium mb-2">분석 내역이 없습니다</h3>
              <p className="text-muted-foreground mb-4">블랙박스 영상을 업로드하여 AI 분석을 시작해보세요.</p>
              <Button className="app-blue-button" onClick={handleUpload}>
                <Upload className="mr-2 h-4 w-4" /> 영상 업로드하기
              </Button>
            </div>
          )}
        </section>
      </main>

      {/* 하단 네비게이션 */}
      <nav className="fixed bottom-0 left-0 right-0 bg-card border-t border-border">
        <div className="flex justify-around">
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleUpload}>
            <Upload size={20} className="text-appblue" />
            <span className="text-xs mt-1">업로드</span>
          </Button>
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3">
            <Clock size={20} className="text-muted-foreground" />
            <span className="text-xs mt-1">분석내역</span>
          </Button>
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleAutoDetectSettings}>
            <div className="relative">
              {autoDetectEnabled ? (
                <div className="absolute -top-1 -right-1 w-2 h-2 bg-green-500 rounded-full"></div>
              ) : null}
              <FileText size={20} className={autoDetectEnabled ? "text-appblue" : "text-muted-foreground"} />
            </div>
            <span className="text-xs mt-1">자동감지</span>
          </Button>
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleProfileClick}>
            {isGuest ? (
              <>
                <User size={20} className="text-muted-foreground" />
                <span className="text-xs mt-1">로그인</span>
              </>
            ) : (
              <>
                <User size={20} className="text-appblue" />
                <span className="text-xs mt-1">프로필</span>
              </>
            )}
          </Button>
        </div>
      </nav>

      {/* 로그인 필요 모달 */}
      <LoginRequiredModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />
    </div>
  )
}
