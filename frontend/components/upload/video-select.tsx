"use client"

import React, { useState, useRef, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Upload, Play, ImageIcon } from "lucide-react"
import { Progress } from "@/components/ui/progress"
import { FilePicker } from '@capawesome/capacitor-file-picker'

interface VideoSelectProps {
  selectedFile: File | null
  preview: string | null
  isUploading: boolean
  isAnalyzing: boolean
  uploadProgress: number
  analyzeProgress: number
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  onClearSelection: () => void
  onUpload: () => void
}

export default function VideoSelect({
  selectedFile,
  preview,
  isUploading,
  isAnalyzing,
  uploadProgress,
  analyzeProgress,
  onFileChange,
  onClearSelection,
  onUpload,
}: VideoSelectProps) {
  const [isPlaying, setIsPlaying] = useState(false)
  const [thumbnail, setThumbnail] = useState<string | null>(null)
  const videoRef = useRef<HTMLVideoElement>(null)

  // Generate poster thumbnail from first video frame
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
    return () => {
      videoEl.removeEventListener('loadedmetadata', handleLoaded)
    }
  }, [preview])

  const handlePlayPause = () => {
    if (!videoRef.current) return
    if (isPlaying) videoRef.current.pause()
    else videoRef.current.play()
    setIsPlaying(!isPlaying)
  }

  const openGalleryPicker = async () => {
    try {
      const result = await FilePicker.pickFiles({ types: ['video/*'], readData: true })
      if (result.files.length > 0) {
        const fileInfo: any = result.files[0]
        const base64 = fileInfo.data as string
        const mime = fileInfo.mimeType as string
        const byteChars = atob(base64)
        const byteNums = new Uint8Array(byteChars.length)
        for (let i = 0; i < byteChars.length; i++) byteNums[i] = byteChars.charCodeAt(i)
        const blob = new Blob([byteNums], { type: mime })
        const fileName = fileInfo.name || `video.${mime.split('/')[1]}`
        const file = new File([blob], fileName, { type: mime })
        onFileChange({ target: { files: [file] } } as any)
      }
    } catch (error) {
      console.error('Gallery picker error:', error)
    }
  }

  return (
    <main className="app-container flex-1 flex flex-col">
      {isAnalyzing ? (
        <div className="flex-1 flex flex-col items-center justify-center">
          <h2 className="text-xl font-bold mb-2">AI 분석 중...</h2>
          <Progress value={analyzeProgress} className="w-full max-w-md h-2 mb-2 bg-muted" />
          <p>{analyzeProgress}%</p>
        </div>
      ) : isUploading ? (
        <div className="flex-1 flex flex-col items-center justify-center">
          <h2 className="text-xl font-bold mb-2">업로드 중...</h2>
          <Progress value={uploadProgress} className="w-full max-w-md h-2 mb-2 bg-muted" />
          <p>{uploadProgress}%</p>
        </div>
      ) : preview ? (
        <div className="flex-1 flex flex-col">
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
          <Button className="w-full py-4 app-blue-button" onClick={onUpload}>
            <Upload className="mr-2" /> 분석 시작하기
          </Button>
        </div>
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
    </main>
  )
}
