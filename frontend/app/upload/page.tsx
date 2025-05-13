"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { ArrowLeft, X } from "lucide-react"
import VideoSelect from "@/components/upload/video-select"
import { Preferences } from "@capacitor/preferences"

export default function UploadPage() {
  const router = useRouter()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [analyzeProgress, setAnalyzeProgress] = useState(0)

  // 인증 상태 확인 - Capacitor Preferences 사용
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const { value: token } = await Preferences.get({ key: "AUTH_TOKEN" })
        const { value: guestToken } = await Preferences.get({ key: "guest_token" })

        if (!token && !guestToken) {
          router.push("/")
        } else if (!token && guestToken) {
          // 게스트 사용자는 대시보드로 리다이렉트
          router.push("/dashboard")
        } else {
          setIsLoading(false)
        }
      } catch (error) {
        console.error("인증 상태 확인 중 오류 발생:", error)
        router.push("/")
      }
    }

    checkAuth()
  }, [router])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file && file.type.startsWith("video/")) {
      setSelectedFile(file)
      const objectUrl = URL.createObjectURL(file)
      setPreview(objectUrl)

      // 이전 미리보기 URL 정리
      return () => URL.revokeObjectURL(objectUrl)
    }
  }

  const handleUpload = async () => {
    if (!selectedFile) return

    setIsUploading(true)

    // 업로드 진행 상태를 시뮬레이션
    const interval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval)
          return 100
        }
        return prev + 5
      })
    }, 200)

    // 실제 구현에서는 여기서 파일 업로드 API 호출
    try {
      // 업로드 완료를 시뮬레이션 (3초 후)
      await new Promise((resolve) => setTimeout(resolve, 3000))

      // 업로드 완료 후 분석 시작
      clearInterval(interval)
      setUploadProgress(100)
      setIsUploading(false)
      setIsAnalyzing(true)

      // 분석 진행 상태를 시뮬레이션
      const analyzeInterval = setInterval(() => {
        setAnalyzeProgress((prev) => {
          if (prev >= 100) {
            clearInterval(analyzeInterval)
            return 100
          }
          return prev + 2
        })
      }, 300)

      // 분석 완료를 시뮬레이션 (5초 후)
      await new Promise((resolve) => setTimeout(resolve, 5000))
      clearInterval(analyzeInterval)
      setAnalyzeProgress(100)

      // 분석 결과 페이지로 이동
      setTimeout(() => {
        router.push("/analysis/1") // 실제로는 서버에서 받은 분석 ID를 사용
      }, 500)
    } catch (error) {
      console.error("업로드 실패:", error)
      setIsUploading(false)
      setIsAnalyzing(false)
    }
  }

  const handleBack = () => {
    router.back()
  }

  const handleSelectFile = () => {
    // 이 함수는 VideoSelect 컴포넌트 내부에서 처리됩니다
  }

  const handleClearSelection = () => {
    setSelectedFile(null)
    setPreview(null)
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
          <h1 className="text-xl font-bold">영상 업로드</h1>
        </div>

        {selectedFile && !isUploading && !isAnalyzing && (
          <Button variant="ghost" size="icon" onClick={handleClearSelection} className="text-white">
            <X size={20} />
          </Button>
        )}
      </header>

      {/* 비디오 선택 컴포넌트 */}
      <VideoSelect
        selectedFile={selectedFile}
        preview={preview}
        isUploading={isUploading}
        isAnalyzing={isAnalyzing}
        uploadProgress={uploadProgress}
        analyzeProgress={analyzeProgress}
        onFileChange={handleFileChange}
        onClearSelection={handleClearSelection}
        onUpload={handleUpload}
        // onSelectFile={handleSelectFile}
      />
    </div>
  )
}
