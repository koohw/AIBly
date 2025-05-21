"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Preferences } from "@capacitor/preferences"
import { Button } from "@/components/ui/button"
import { Upload, Camera, Clock, AlertCircle,  ChevronRight } from "lucide-react"
import AuthScreen from "@/components/start/auth-screen"
import LoginRequiredModal from "@/components/start/login-required-modal"
import { useTheme } from "./contexts/theme-context"
import { registerFcmToken } from '@/lib/api/Fcm'; // FCM 토큰 등록 함수 import
import  RobotBgRemove  from "@/public/image/removebgrobot.png"
import  Uploade  from "@/public/image/upload.png"
import  FileText  from "@/public/image/filetext.png"
import  User  from "@/public/image/user.png"
import Accident from "@/public/image/accident.jpg"
import { getUserMe } from "@/lib/api/User"



export default function Home() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(true)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isGuest, setIsGuest] = useState(false)
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [hasAnalysis, setHasAnalysis] = useState(false) // 분석 결과가 있는지 여부
  const [autoDetectEnabled, setAutoDetectEnabled] = useState(true) // 자동 감지 활성화 여부
  const { theme } = useTheme()
  const [userName, setUserName] = useState("사용자")

  // 인증 상태 확인
  useEffect(() => {
    async function checkAuth() {
      setIsLoading(true)

      // 1) Preferences에서 JWT 꺼내기
      const { value: authToken } = await Preferences.get({ key: "AUTH_TOKEN" })

      if (authToken) {
        // 로그인된 사용자
        setIsLoggedIn(true)
        setIsGuest(false)
        setHasAnalysis(true)

      // 사용자 이름 가져오기 (실제로는 API 호출이 필요할 수 있음)
        try {
          const user = await getUserMe()
          setUserName(user.name)
          console.log('[Home] 유저 정보:', user)
        } catch (err) {
          console.error('[Home] getUserMe 실패:', (err as Error).message)
        }

      // FCM 토큰 등록 상태 확인
      const { value: fcmRegistered } = await Preferences.get({ key: "fcm_token_registered" });
            
      // FCM 토큰이 등록되지 않았다면 등록 시도
      if (fcmRegistered !== "true") {
        try {
          console.log("[Home] FCM 토큰이 등록되지 않았습니다. 등록을 시도합니다.");
          // 타임아웃 추가하여 비동기로 처리
          setTimeout(async () => {
            try {
              await registerFcmToken(authToken);
              await Preferences.set({ key: "fcm_token_registered", value: "true" });
              console.log("[Home] FCM 토큰 등록 완료");
            } catch (delayedError) {
              console.error("[Home] 지연된 FCM 토큰 등록 실패:", delayedError);
            }
          }, 2000);
        } catch (error) {
          console.error("[Home] FCM 토큰 등록 실패:", error);
        }
      } else {
        console.log("[Home] FCM 토큰이 이미 등록되어 있습니다.");
      }

        // (선택) 유효성 재검증이 필요하면 여기에 API 호출…
        // await fetch(`${API_URL}/user/me`, { headers: { Authorization: `Bearer ${authToken}` } })
      } else {
        // 게스트 모드 또는 비로그인 상태 확인
        const { value: guestToken } = await Preferences.get({ key: "guest_token" })
        if (guestToken) {
          setIsLoggedIn(true) // 게스트도 로그인된 것으로 처리
          setIsGuest(true)
        } else {
          setIsLoggedIn(false)
        }
      }



        // 2) 자동 감지 설정도 Preferences에서 꺼내기
        const { value: autoDetect } = await Preferences.get({ key: "AUTO_DETECT" })
        if (autoDetect !== null) {
          setAutoDetectEnabled(autoDetect === "true")
        }

        setIsLoading(false)
      }
      checkAuth()
  }, [])

  const handleLogout = () => {
    Preferences.remove({ key: "AUTH_TOKEN" })
    localStorage.removeItem("auth_token")
    setIsLoggedIn(false)
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
    // /analysis?id=1 형태로 push
    router.push(`/analysis?id=1`)
    // 나중에 API에서 받아온 동적 ID로 바꿀땐 아래와 같이 쓰면 됨
    //router.push(`/analysis?id=${reportId}`)
  }

  const handleAutoDetectSettings = () => {
    router.push("/profile")
  }

  const handleHistory = () => {
    router.push("/history")
  }

  // 로딩 중이면 로딩 화면 표시
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="w-8 h-8 border-4 border-appblue border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  // 로그인되지 않은 상태면 AuthScreen 표시
  if (!isLoggedIn) {
    return <AuthScreen />
  }

  // 로그인된 상태면 대시보드 표시 (기존 dashboard/page.tsx의 내용)
  return (
    <div className="min-h-screen bg-appnavy text-white">
      {/* 헤더 */}
      <header className="p-6 flex justify-between items-center">
        <h1 className="text-xl font-bold">AI 블리</h1>
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

      {/* 게스트 모드 알림
      {isGuest && (
        <div className="mx-4 mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700/50 rounded-md flex items-start">
          <AlertCircle className="text-yellow-500 mr-2 flex-shrink-0 mt-0.5" size={16} />
          <div className="text-sm">
            <p className="text-yellow-700 dark:text-yellow-500 font-medium">게스트 모드로 접속 중입니다</p>
            <p className="text-yellow-600/70 dark:text-yellow-500/70 text-xs">모든 기능을 사용하려면 로그인하세요</p>
          </div>
        </div>
      )} */}

      {/* 메인 콘텐츠 */}
      <main className="px-6 pb-6 relative">
        {/* 배경 분할 */}
        <div className="absolute inset-0 z-0">
          <div className="h-[63%] bg-appnavy"></div>
          <div className="h-[37%] bg-gray-100"></div>
        </div>

        {/* 기존 콘텐츠를 z-index로 배경 위에 표시 */}
        <div className="relative z-10">
          {/* 환영 메시지 및 로봇 이미지 */}
          <div className="flex flex-col md:flex-row items-center justify-between mb-8">
            <div className="mb-6 md:mb-0">
              <h1 className="text-3xl font-bold mb-2">{userName}님의</h1>
              <h2 className="text-3xl font-bold mb-4">블랙박스 리뷰 입니다</h2>

              <div className="bg-appblue rounded-full px-6 py-3 inline-block">
                <div className="flex items-center">
                <div className="w-3 h-3 bg-green-500 rounded-full mr-2 relative">
                  <span className="absolute w-3 h-3 bg-green-500 rounded-full animate-ping opacity-75"></span>
                </div>
                <p className="font-medium">사고영상을 자동 감지중 입니다.</p>
                </div>
              </div>
            </div>

            <div className="w-48 h-48 relative">
              <div className="flex items-center justify-center h-full animate-pulse-slow">
                <img
                  src={RobotBgRemove.src}
                  alt="AI 로봇"
                  width={200}
                  height={200}
                  className="object-contain"
                />
              </div>
            </div>
          </div>

          {/* 3개 메뉴 버튼 */}
          <div className="grid grid-cols-3 gap-4 mb-8">
            <Button
              variant="outline"
              className="flex flex-col items-center justify-center py-4 rounded-xl bg-appblue hover:bg-gray-100 text-black h-28 border-2 border-appblue shadow-md"
              onClick={handleUpload}
            >
              <img
                src={Uploade.src}
                alt="upload"
                width={18}
                height={18}
                className="object-contain mb-5"/>
              <h4 className="text-[15px]">업로드</h4>
            </Button>

            <Button
              variant="outline"
              className="flex flex-col items-center justify-center py-4 rounded-xl bg-appblue hover:bg-gray-100 text-black h-28 border-2 border-appblue shadow-md"
              onClick={handleHistory}
            >
              <img
              src={FileText.src}
              alt="file"
              width={18}
              height={18}
              className="object-contain mb-5"/>
              <h4 className="text-[15px]">분석내역</h4>
            </Button>

            <Button
              variant="outline"
              className="flex flex-col items-center justify-center py-4 rounded-xl bg-appblue hover:bg-gray-100 text-black h-28 border-2 border-appblue shadow-md"
              onClick={handleProfileClick}
            >
              <img
              src={User.src}
              alt="user"
              width={18}
              height={18}
              className="object-contain mb-5"/>
              <h4 className="text-[15px]">프로필</h4>
            </Button>
          </div>

          {/* 자동 감지 상태 표시 */}
          {/* <section className="mb-6 bg-gray-50 backdrop-blur-sm rounded-xl p-4 shadow-sm">
            <div className="flex items-center">
              {autoDetectEnabled ? (
                <div className="w-3 h-3 bg-green-500 rounded-full mr-2 relative">
                  <span className="absolute w-3 h-3 bg-green-500 rounded-full animate-ping opacity-75"></span>
                </div>
              ) : (
                <div className="w-3 h-3 bg-gray-400 rounded-full mr-2"></div>
              )}
              <div className="flex-1">
                <h3 className="font-medium text-gray-800">자동 감지 {autoDetectEnabled ? "활성화됨" : "비활성화됨"}</h3>
                <p className="text-xs text-gray-600">
                  {autoDetectEnabled
                    ? "블랙박스 영상이 감지되면 자동으로 분석합니다"
                    : "블랙박스 영상을 수동으로 업로드해야 합니다"}
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="text-xs text-appblue hover:text-appblue-dark"
                onClick={handleProfileClick}
              >
                설정
              </Button>
            </div>
          </section> */}

          {/* 최근 분석 */}
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold text-gray-800">최근 분석</h2>
              <Button variant="link" className="text-appblue p-0 hover:text-appblue-dark" onClick={handleHistory}>
                모두 보기
              </Button>
            </div>

            {hasAnalysis ? (
              <div className="bg-gray-50 shadow-sm backdrop-blur-sm rounded-xl overflow-hidden">
                <div
                  className="p-4 border-b border-gray-200 flex items-center cursor-pointer"
                  onClick={handleViewAnalysis}
                >
                  <div className="w-16 h-16 bg-muted rounded-md mr-3 flex-shrink-0 overflow-hidden">
                    <img
                      src={Accident.src}
                      alt="사고 영상 썸네일"
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="flex-1">
                    <h3 className="font-medium text-gray-800">교차로 신호위반 사고</h3>
                    <p className="text-xs text-gray-600">2025년 4월 15일 오후 2:30</p>
                    <div className="flex mt-1">
                      <span className="text-xs bg-appblue/20 text-appblue px-2 py-0.5 rounded mr-1">
                        과실비율 30:70
                      </span>
                      <span className="text-xs bg-gray-200 text-gray-700 px-2 py-0.5 rounded">신호위반</span>
                    </div>
                  </div>
                  <ChevronRight size={20} className="text-gray-500" />
                </div>
              </div>
            ) : (
              <div className="bg-gray-50 shadow-sm backdrop-blur-sm rounded-xl p-6 text-center">
                <div className="w-16 h-16 rounded-full bg-appblue mx-auto mb-4 flex items-center justify-center">
                  <Clock className="text-white" size={32} />
                </div>
                <h3 className="text-lg font-medium mb-2 text-gray-800">분석 내역이 없습니다</h3>
                <p className="text-gray-600 mb-4">블랙박스 영상을 업로드하여 AI 분석을 시작해보세요.</p>
                <Button className="bg-appblue hover:bg-appblue-dark text-white" onClick={handleUpload}>
                  <Upload className="mr-2 h-4 w-4" /> 영상 업로드하기
                </Button>
              </div>
            )}
          </section>
        </div>
      </main>

      {/* 로그인 필요 모달 */}
      <LoginRequiredModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />
    </div>
  )
}