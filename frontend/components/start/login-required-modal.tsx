"use client"

import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Lock } from "lucide-react"

interface LoginRequiredModalProps {
  isOpen: boolean
  onClose: () => void
}

export default function LoginRequiredModal({ isOpen, onClose }: LoginRequiredModalProps) {
  const router = useRouter()

  const handleLogin = () => {
    onClose()
    router.push("/login")
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="bg-card border-border text-foreground sm:max-w-[425px]">
        <DialogHeader className="flex flex-col items-center text-center">
          <div className="bg-appblue p-3 rounded-full mb-4">
            <Lock className="h-6 w-6 text-white" />
          </div>
          <DialogTitle className="text-xl">로그인이 필요한 서비스입니다</DialogTitle>
          <DialogDescription className="text-muted-foreground mt-2">
            영상 업로드 및 분석 기능을 이용하려면 로그인이 필요합니다.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="flex flex-col sm:flex-row gap-2 mt-4">
          <Button variant="outline" className="sm:flex-1 border-border text-foreground" onClick={onClose}>
            취소
          </Button>
          <Button className="sm:flex-1 app-blue-button" onClick={handleLogin}>
            로그인하기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
