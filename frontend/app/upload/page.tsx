"use client"

import type React from "react"

import { useState, useRef, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Upload, X, Play } from "lucide-react"
import { Progress } from "@/components/ui/progress"

export default function UploadPage() {
  const router = useRouter()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [analyzeProgress, setAnalyzeProgress] = useState(0)
  const videoRef = useRef<HTMLVideoElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 인증 상태 확인
  useEffect(() => {
    const token = localStorage.getItem("auth_token")
    const guestToken = localStorage.getItem("guest_token")

    if (!token && !guestToken) {
      router.push("/")
    } else if (!token && guestToken) {
      // 게스트 사용자는 대시보드로 리다이렉트
      router.push("/dashboard")
    } else {
      setIsLoading(false)
    }
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

  const handlePlayPause = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause()
      } else {
        videoRef.current.play()
      }
      setIsPlaying(!isPlaying)
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
    fileInputRef.current?.click()
  }

  const handleClearSelection = () => {
    setSelectedFile(null)
    setPreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
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

      {/* 메인 콘텐츠 */}
      <main className="app-container flex-1 flex flex-col">
        {/* 파일 입력 (숨김) */}
        <input type="file" accept="video/*" onChange={handleFileChange} ref={fileInputRef} className="hidden" />

        {isAnalyzing ? (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="w-full max-w-md">
              <div className="mb-4 text-center">
                <h2 className="text-xl font-bold mb-2">AI 분석 중...</h2>
                <p className="text-muted-foreground text-sm">영상을 분석하여 사고 상황을 파악하고 있습니다.</p>
              </div>

              <Progress value={analyzeProgress} className="h-2 mb-2 bg-muted" />
              <p className="text-right text-sm text-muted-foreground">{analyzeProgress}%</p>

              <div className="mt-8 text-center text-sm text-muted-foreground">
                <p>AI가 영상을 분석하여 과실 비율을 산정하고 있습니다.</p>
                <p>영상 길이와 복잡도에 따라 분석 시간이 달라질 수 있습니다.</p>
              </div>
            </div>
          </div>
        ) : isUploading ? (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="w-full max-w-md">
              <div className="mb-4 text-center">
                <h2 className="text-xl font-bold mb-2">업로드 중...</h2>
                <p className="text-muted-foreground text-sm">영상을 서버에 업로드하고 있습니다.</p>
              </div>

              <Progress value={uploadProgress} className="h-2 mb-2 bg-muted" />
              <p className="text-right text-sm text-muted-foreground">{uploadProgress}%</p>

              <div className="mt-8 text-center text-sm text-muted-foreground">
                <p>업로드가 완료되면 자동으로 분석이 시작됩니다.</p>
                <p>영상 길이에 따라 분석 시간이 달라질 수 있습니다.</p>
              </div>
            </div>
          </div>
        ) : preview ? (
          <div className="flex-1 flex flex-col">
            {/* 비디오 미리보기 */}
            <div className="relative rounded-lg overflow-hidden bg-black mb-4 aspect-video">
              <video
                ref={videoRef}
                src={preview}
                className="w-full h-full object-contain"
                onEnded={() => setIsPlaying(false)}
              />

              <div
                className="absolute inset-0 flex items-center justify-center cursor-pointer"
                onClick={handlePlayPause}
              >
                {!isPlaying && (
                  <div className="bg-black/50 rounded-full p-4">
                    <Play size={24} className="text-white" />
                  </div>
                )}
              </div>
            </div>

            {/* 파일 정보 */}
            <div className="app-card p-4 mb-4">
              <h3 className="font-medium mb-1">파일 정보</h3>
              <p className="text-sm text-muted-foreground">파일명: {selectedFile?.name}</p>
              <p className="text-sm text-muted-foreground">
                크기: {(selectedFile?.size ? selectedFile.size / (1024 * 1024) : 0).toFixed(2)} MB
              </p>
            </div>

            {/* 업로드 버튼 */}
            <div className="mt-auto">
              <Button className="w-full py-6 app-blue-button" onClick={handleUpload}>
                <Upload className="mr-2 h-4 w-4" /> 분석 시작하기
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div
              className="border-2 border-dashed border-border rounded-lg p-8 w-full max-w-md flex flex-col items-center justify-center cursor-pointer hover:border-appblue transition-colors"
              onClick={handleSelectFile}
            >
              <div className="bg-muted rounded-full p-4 mb-4">
                <Upload size={32} className="text-muted-foreground" />
              </div>
              <h2 className="text-xl font-bold mb-2">블랙박스 영상 선택</h2>
              <p className="text-muted-foreground text-center mb-4">갤러리에서 분석할 블랙박스 영상을 선택해주세요</p>
              <Button variant="outline" className="border-border">
                갤러리에서 선택
              </Button>
            </div>

            <div className="mt-8 text-center text-sm text-muted-foreground">
              <p>지원 형식: MP4, AVI, MOV</p>
              <p>최대 파일 크기: 500MB</p>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
