"use client"

import { Button } from "@/components/ui/button"
import { FileX, Upload } from "lucide-react"

interface EmptyHistoryProps {
  onUpload: () => void
}

export default function EmptyHistory({ onUpload }: EmptyHistoryProps) {
  return (
    <div className="app-card p-6 text-center">
      <div className="w-16 h-16 rounded-full bg-muted mx-auto mb-4 flex items-center justify-center">
        <FileX className="text-muted-foreground" size={32} />
      </div>
      <h3 className="text-lg font-medium mb-2">분석 내역이 없습니다</h3>
      <p className="text-muted-foreground mb-4">블랙박스 영상을 업로드하여 AI 분석을 시작해보세요.</p>
      <Button className="app-blue-button" onClick={onUpload}>
        <Upload className="mr-2 h-4 w-4" /> 영상 업로드하기
      </Button>
    </div>
  )
}
