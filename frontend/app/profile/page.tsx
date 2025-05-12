"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { ArrowLeft, User, Shield, Bell, Moon, Sun, LogOut } from "lucide-react"
import { Switch } from "@/components/ui/switch"
import { useTheme } from "../contexts/theme-context"
import { Preferences } from '@capacitor/preferences'
import WhithdrawButton from "@/components/profile/whithdraw"


export default function ProfilePage() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(true)
  const { theme, setTheme } = useTheme()
  const [notifications, setNotifications] = useState(true)
  const [autoDetect, setAutoDetect] = useState(true)

  // 인증 상태 확인
  useEffect(() => {
    const init = async () => {
      const { value: token } = await Preferences.get({ key: 'AUTH_TOKEN' })
    if (!token) {
      router.push("/login")
    } else {
      setIsLoading(false)

      // Preferences에서 자동 감지 설정 불러오기
      const { value: savedAutoDetect } = await Preferences.get({ key: 'AUTO_DETECT' })
      if (savedAutoDetect !== null) {
        setAutoDetect(savedAutoDetect === "true")
      }

      // Preferences에서 알림 설정 불러오기
      const { value: savedNotifications } = await Preferences.get({ key: 'NOTIFICATIONS' })
      if (savedNotifications !== null) {
        setNotifications(savedNotifications === "true")
      }
    }
  }
    init()
  }, [router])

  const handleBack = () => {
    router.back()
  }

  const handleLogout = () => {
    Preferences.remove({ key: 'AUTH_TOKEN' })
    router.push("/")
  }

  const toggleTheme = () => {
    setTheme(theme === "dark" ? "light" : "dark")
  }

  // 자동 감지 설정 변경 시 localStorage에 저장
  const handleAutoDetectChange = (checked: boolean) => {
    setAutoDetect(checked)
    Preferences.set({ key: 'AUTO_DETECT', value: checked.toString() })
  }

  // 알림 설정 변경 시 localStorage에 저장
  const handleNotificationsChange = (checked: boolean) => {
    setNotifications(checked)
    Preferences.set({ key: 'NOTIFICATIONS', value: checked.toString() })
  }

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="w-8 h-8 border-4 border-appblue border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      {/* 헤더 */}
      <header className="app-header">
        <div className="flex items-center">
          <Button variant="ghost" size="icon" onClick={handleBack} className="mr-2 text-white">
            <ArrowLeft size={20} />
          </Button>
          <h1 className="text-xl font-bold">프로필</h1>
        </div>
      </header>

      {/* 프로필 정보 */}
      <div className="p-6 flex items-center border-b border-border">
        <div className="w-16 h-16 bg-muted rounded-full flex items-center justify-center mr-4">
          <User size={32} className="text-muted-foreground" />
        </div>
        <div>
          <h2 className="text-lg font-bold">사용자</h2>
          <p className="text-sm text-muted-foreground">user@example.com</p>
        </div>
      </div>

      {/* 탭 콘텐츠 */}
      <div className="flex-1">
        <Tabs defaultValue="settings" className="w-full">
          <TabsList className="grid grid-cols-2 w-full bg-muted p-0">
            <TabsTrigger
              value="profile"
              className="py-3 rounded-none border-b-2 border-transparent data-[state=active]:border-appblue data-[state=active]:bg-transparent data-[state=active]:text-foreground"
            >
              프로필
            </TabsTrigger>
            <TabsTrigger
              value="settings"
              className="py-3 rounded-none border-b-2 border-transparent data-[state=active]:border-appblue data-[state=active]:bg-transparent data-[state=active]:text-foreground"
            >
              설정
            </TabsTrigger>
          </TabsList>

          {/* 프로필 탭 */}
          <TabsContent value="profile" className="p-4">
            <div className="space-y-4">
              <div className="app-card p-4">
                <h3 className="font-medium mb-4">개인 정보</h3>
                <div className="space-y-3">
                  <div>
                    <label className="text-sm text-muted-foreground">이름</label>
                    <p className="font-medium">사용자</p>
                  </div>
                  <div>
                    <label className="text-sm text-muted-foreground">이메일</label>
                    <p className="font-medium">user@example.com</p>
                  </div>
                  <div>
                    <label className="text-sm text-muted-foreground">가입일</label>
                    <p className="font-medium">2025년 1월 15일</p>
                  </div>
                </div>
              </div>

              <div className="app-card p-4">
                <h3 className="font-medium mb-4">차량 정보</h3>
                <div className="space-y-3">
                  <div>
                    <label className="text-sm text-muted-foreground">차량 번호</label>
                    <p className="font-medium">12가 3456</p>
                  </div>
                  <div>
                    <label className="text-sm text-muted-foreground">차량 모델</label>
                    <p className="font-medium">현대 아반떼</p>
                  </div>
                </div>
              </div>
            </div>
          </TabsContent>

          {/* 설정 탭 */}
          <TabsContent value="settings" className="p-4">
            <div className="space-y-4">
              <div className="app-card p-4">
                <h3 className="font-medium mb-4">앱 설정</h3>
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <Bell size={20} className="mr-3 text-muted-foreground" />
                      <div>
                        <p className="font-medium">알림 설정</p>
                        <p className="text-sm text-muted-foreground">앱 알림을 받습니다</p>
                      </div>
                    </div>
                    <Switch
                      checked={notifications}
                      onCheckedChange={handleNotificationsChange}
                      className="data-[state=checked]:bg-appblue"
                    />
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <Shield size={20} className="mr-3 text-muted-foreground" />
                      <div>
                        <p className="font-medium">자동 감지</p>
                        <p className="text-sm text-muted-foreground">
                          블랙박스 영상 {autoDetect ? "자동 감지" : "수동 업로드"}
                        </p>
                      </div>
                    </div>
                    <Switch
                      checked={autoDetect}
                      onCheckedChange={handleAutoDetectChange}
                      className="data-[state=checked]:bg-appblue"
                    />
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      {theme === "dark" ? (
                        <Moon size={20} className="mr-3 text-muted-foreground" />
                      ) : (
                        <Sun size={20} className="mr-3 text-muted-foreground" />
                      )}
                      <div>
                        <p className="font-medium">{theme === "dark" ? "다크 모드" : "라이트 모드"}</p>
                        <p className="text-sm text-muted-foreground">
                          {theme === "dark" ? "어두운 테마 사용 중" : "밝은 테마 사용 중"}
                        </p>
                      </div>
                    </div>
                    <Switch
                      checked={theme === "dark"}
                      onCheckedChange={toggleTheme}
                      className="data-[state=checked]:bg-appblue"
                    />
                  </div>
                </div>
              </div>

              <div className="app-card p-4">
                <h3 className="font-medium mb-4">계정</h3>
              </div>
                <Button
                  variant="destructive"
                  className="w-full flex items-center justify-center"
                  onClick={handleLogout}
                >
                  <LogOut size={16} className="mr-2" />
                  로그아웃
                </Button>
                <WhithdrawButton />
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
