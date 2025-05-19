"use client"

import React, { useState, useRef, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Upload, Play, ImageIcon, CheckCircle } from "lucide-react"
import { Progress } from "@/components/ui/progress"
import { FilePicker } from '@capawesome/capacitor-file-picker'
import { getPresignedUrl, PresignedUrlResponse, notifyManualUpload, pollVideoStatus } from "@/lib/api/Video"
import { getReportDetail } from "@/lib/api/Report"
import type { AxiosError } from "axios"

interface VideoSelectProps {
  selectedFile: File | null
  preview: string | null
  onFileChange: (file: File, previewUrl: string) => void
  onClearSelection: () => void
}

// Toast 컴포넌트 정의
interface ToastProps {
  message: string;
  isVisible: boolean;
  type?: 'success' | 'error' | 'info';
}

function Toast({ message, isVisible, type = 'success' }: ToastProps) {
  if (!isVisible) return null;

  const bgColor = type === 'success' ? 'bg-appblue' : 
                 type === 'error' ? 'bg-red-500' : 'bg-blue-500';
  
  return (
    <div className={`fixed bottom-20 left-0 right-0 mx-auto w-[90%] max-w-md p-4 rounded-lg shadow-lg z-50 flex items-center justify-between ${bgColor} text-white animate-slideUp`}>
      <div className="flex items-center">
        {type === 'success' && <CheckCircle className="mr-2" size={20} />}
        <p className="font-medium text-sm">{message}</p>
      </div>
    </div>
  );
}

export default function VideoSelect({
  selectedFile,
  preview,
  onFileChange,
  onClearSelection,
}: VideoSelectProps) {
  const router = useRouter()
  const [isUploading, setIsUploading] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [isUploadComplete, setIsUploadComplete] = useState(false)
  const [videoId, setVideoId] = useState<number | null>(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [analyzeProgress, setAnalyzeProgress] = useState(0)
  const [thumbnail, setThumbnail] = useState<string | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)
  
  // Toast 상태
  const [toastMessage, setToastMessage] = useState('')
  const [isToastVisible, setIsToastVisible] = useState(false)
  const [toastType, setToastType] = useState<'success' | 'error' | 'info'>('success')

  // Toast 표시 함수
  const showToast = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
    setToastMessage(message)
    setToastType(type)
    setIsToastVisible(true)
    
    // 3초 후 자동으로 닫기
    setTimeout(() => {
      setIsToastVisible(false)
    }, 3000)
  }

  useEffect(() => {
  console.log('▶️ video-select got preview prop:', preview)
}, [preview])

  // Generate poster thumbnail
  useEffect(() => {
    if (!preview) {
      setThumbnail(null)
      return
    }
    const videoEl = document.createElement('video')
    videoEl.src = preview
    videoEl.crossOrigin = 'anonymous'
    videoEl.preload = 'metadata'
    videoEl.muted = true
    videoEl.playsInline = true
    const handleLoaded = () => {
      const canvas = document.createElement('canvas')
      canvas.width = videoEl.videoWidth
      canvas.height = videoEl.videoHeight
      const ctx = canvas.getContext('2d')
      if (ctx) ctx.drawImage(videoEl, 0, 0, canvas.width, canvas.height)
      setThumbnail(canvas.toDataURL('image/png'))
    }
    videoEl.addEventListener('loadedmetadata', handleLoaded)
    return () => videoEl.removeEventListener('loadedmetadata', handleLoaded)
  }, [preview])

  // Upload file to S3
  const uploadToS3 = (
    presignedUrl: string,
    file: File,
    onProgress: (percent: number) => void
  ): Promise<void> => {
    return new Promise((resolve, reject) => {
      console.log(`🕒 S3 업로드 시작: ${file.name}`)
      const xhr = new XMLHttpRequest()
      xhr.open("PUT", presignedUrl)
      xhr.setRequestHeader("Content-Type", file.type)
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          const percent = Math.round((event.loaded / event.total) * 100)
          console.log(`⬆️ 업로드 진행: ${percent}%`)
          onProgress(percent)
        }
      }
      xhr.onload = () => {
        if (xhr.status === 200) {
          console.log(`✅ S3 업로드 성공: ${file.name} (200)`)
          resolve()
        } else {
          console.error(`❌ S3 업로드 실패: ${file.name} (status ${xhr.status})`)
          reject(new Error(`S3 업로드 실패: ${xhr.statusText}`))
        }
      }
      xhr.onerror = () => {
        console.error(`❌ S3 업로드 에러: ${file.name}`)
        reject(new Error("S3 업로드 에러"))
      }
      xhr.send(file)
    })
  }

  // 분석 시작하기
  const handleAnalyze = async () => {
    if (!selectedFile) return
    setIsUploading(true)
    try {
      // 1) Presigned URL 요청
      console.log('🎯 Presigned URL 요청 중...')
      const { presignedUrl, s3Key }: PresignedUrlResponse = await getPresignedUrl({
        fileName: selectedFile.name,
        contentType: selectedFile.type,
      })
      console.log(`✅ Presigned URL 발급 성공: key=${s3Key}`)
      console.log(`🔗 Upload URL: ${presignedUrl}`)

      // 2) S3 업로드
      await uploadToS3(presignedUrl, selectedFile, setUploadProgress)
      setIsUploading(false)

       // 3) DB 수동 업로드 알림
      console.log('📫 DB 수동 업로드 알림 요청 중...')
      const { videoId: uploadedVideoId  } = await notifyManualUpload({
        fileName: selectedFile.name,
        s3Key,
        contentType: selectedFile.type,
        size: selectedFile.size,
      })
      console.log(`✅ DB 알림 완료, 분석 준비 완료: videoId=${uploadedVideoId}`)
      
      // 업로드 완료 상태로 변경
      setVideoId(uploadedVideoId)
      setIsUploadComplete(true)
      
      // 기존 폴링 코드 주석 처리 - 더 이상 필요없음
      /*
      setIsAnalyzing(true)
      console.log('🔄 상태 폴링 시작: videoId=', videoId )
      const pollInterval = setInterval(async () => {
        try {
          const { status, reportId } = await pollVideoStatus(String(videoId))
          console.log(`📊 폴링 상태: ${status}`)
          if (status === 'COMPLETED') {
            clearInterval(pollInterval)
            console.log(`🏁 분석 완료: reportId=${reportId}`)
             // 보고서 상세 조회
            const report = await getReportDetail(reportId)
            console.log('📄 보고서 상세:', report)

            setIsAnalyzing(false)
            router.push(`/analysis?id=${reportId}`)
          }
        } catch (e) {
          console.error('폴링 오류:', e)
          clearInterval(pollInterval)
          setIsAnalyzing(false)
        }
      }, 3000)
      */
    } catch (error) {
      const err = error as AxiosError
      console.error('🚨 처리 중 오류:', err.response?.data || err.message)
      setIsUploading(false)
      setIsAnalyzing(false)
      showToast('영상 업로드 중 오류가 발생했습니다. 다시 시도해주세요.', 'error')
    }
  }

  // 비디오 재생/일시정지
  const handlePlayPause = () => {
    if (!videoRef.current) return
    isPlaying ? videoRef.current.pause() : videoRef.current.play()
    setIsPlaying(!isPlaying)
  }

  // 갤러리에서 선택
 // VideoSelect 컴포넌트 내부
// VideoSelect 컴포넌트 내부
const openGalleryPicker = async () => {
  try {
    const res = await FilePicker.pickFiles({
      types: ['video/*'],
      readData: true  // Base64 로 읽기
    })
    if (!res.files.length) return

    const info = res.files[0] as any
    const { data: base64, mimeType, name } = info

    // 1) 업로드용 File 생성
    const byteChars = atob(base64)
    const byteNums = new Uint8Array(byteChars.length)
    for (let i = 0; i < byteChars.length; i++) {
      byteNums[i] = byteChars.charCodeAt(i)
    }
    const blob = new Blob([byteNums], { type: mimeType })
    const file = new File([blob], name, { type: mimeType })

    // 2) 미리보기용 Blob URL 생성
    const previewUrl = URL.createObjectURL(blob)

    // 부모 콜백에 파일 + Blob URL 전달
    onFileChange(file, previewUrl)
  } catch (e) {
    console.error('Gallery picker error:', e)
  }
}





  // 대시보드로 이동하는 함수
  const handleGoDashboard = () => {
    router.push('/dashboard')
  }

  // 새 영상 선택으로 돌아가기
  const handleNewUpload = () => {
    // 상태 초기화
    onClearSelection();
    setIsUploadComplete(false);
    setVideoId(null);
    setUploadProgress(0);
    setAnalyzeProgress(0);
    setThumbnail(null);
    setIsPlaying(false);
  }

  return (
    <main className="app-container flex-1 flex flex-col">
      {isAnalyzing ? (
        <div className="flex-1 flex flex-col items-center justify-center">
          <h2 className="text-xl font-bold mb-2">AI 분석 중...</h2>

          {/* 비디오 플레이어 추가 */}
          <div className="w-full max-w-md aspect-video bg-black rounded-lg mb-4 overflow-hidden relative">
            <video src={preview || undefined} className="w-full h-full object-contain" autoPlay muted loop />
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-16 h-16 rounded-full bg-black/50 flex items-center justify-center">
                <div className="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin"></div>
              </div>
            </div>
          </div>
          
          <Progress value={analyzeProgress} className="w-full max-w-md h-2 mb-2 bg-muted" />
          <p>{analyzeProgress}%</p>
        </div>
      ) : isUploading ? (
        <div className="flex-1 flex flex-col items-center justify-center">
          <h2 className="text-xl font-bold mb-2">업로드 중...</h2>
          <Progress value={uploadProgress} className="w-full max-w-md h-2 mb-2 bg-muted" />
          <p>{uploadProgress}%</p>
        </div>
      ) : isUploadComplete ? (
        <div className="flex-1 flex flex-col items-center justify-center">
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-6 mb-6 max-w-md w-full">
            <div className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-full bg-green-500 mb-4 flex items-center justify-center">
                <CheckCircle className="text-white" size={40} />
              </div>
              <h2 className="text-2xl font-bold mb-2">업로드 완료!</h2>
              <p className="text-center text-muted-foreground mb-4">
                영상 업로드가 완료되었습니다.
              </p>
              <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-md border border-blue-200 dark:border-blue-800 mb-6 w-full">
                <h3 className="font-medium text-appblue dark:text-blue-400 mb-2">AI 분석 진행 중</h3>
                <p className="text-sm text-muted-foreground">
                  AI가 영상을 분석하는 동안 다른 작업을 진행하셔도 됩니다. 분석이 완료되면 알림을 통해 알려드립니다.
                </p>
              </div>
            </div>
          </div>
          <Button className="w-full max-w-md py-5 app-blue-button mb-3 text-base font-medium" onClick={handleGoDashboard}>
            대시보드로 이동
          </Button>
          <Button variant="outline" onClick={handleNewUpload} className="w-full max-w-md py-5 text-base">
            새 영상 업로드
          </Button>
        </div>
      ) : preview ? (
        <>
          <div className="relative rounded-lg overflow-hidden bg-black mb-4 aspect-video">
            <video
              ref={videoRef}
              src={preview}
              poster={thumbnail || undefined}
              className="w-full h-full object-contain"
              preload="metadata"
              onEnded={() => setIsPlaying(false)}
            />
            <div
              className="absolute inset-0 flex items-center justify-center cursor-pointer"
              onClick={handlePlayPause}
            >
              {!isPlaying && <Play size={48} className="text-white" />}
            </div>
          </div>
          <div className="p-4 mb-4 bg-card rounded-lg">
            <h3 className="font-medium mb-1">파일 정보</h3>
            <p>파일명: {selectedFile?.name}</p>
            <p>크기: {((selectedFile?.size ?? 0) / (1024 * 1024)).toFixed(2)} MB</p>
          </div>
          <Button className="w-full py-4 app-blue-button" onClick={handleAnalyze}>
            <Upload className="mr-2" /> 분석 시작하기
          </Button>
          <Button variant="ghost" onClick={onClearSelection} className="mt-2">
            선택 취소
          </Button>
        </>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center">
          <h2 className="text-xl font-bold mb-2">블랙박스 영상 선택</h2>
          <Button
            variant="outline"
            className="h-32 w-full max-w-md flex flex-col items-center justify-center space-y-2"
            onClick={openGalleryPicker}
          >
            <div className="w-12 h-12 rounded-full bg-appblue flex items-center justify-center">
              <ImageIcon size={24} className="text-white" />
            </div>
            <span>갤러리에서 선택</span>
          </Button>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            지원 형식: MP4, AVI, MOV (최대 500MB)
          </p>
        </div>
      )}
      {isToastVisible && (
        <Toast message={toastMessage} isVisible={isToastVisible} type={toastType} />
      )}
    </main>
  )
}
