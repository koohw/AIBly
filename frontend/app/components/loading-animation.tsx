"use client"

import { useEffect, useRef } from "react"
import { motion } from "framer-motion"

export default function LoadingAnimation() {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext("2d")
    if (!ctx) return

    // 캔버스 크기 설정
    const setCanvasSize = () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
    }

    setCanvasSize()
    window.addEventListener("resize", setCanvasSize)

    // 카메라 렌즈 애니메이션
    let scale = 0
    let opacity = 0

    const drawCamera = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)

      // 배경 - 다크 모드에서는 검은색, 라이트 모드에서는 흰색
      const isDarkMode = document.documentElement.classList.contains("dark")
      ctx.fillStyle = isDarkMode ? "#000000" : "#f8f9fa"
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      // 카메라 바디
      const centerX = canvas.width / 2
      const centerY = canvas.height / 2

      ctx.save()
      ctx.globalAlpha = opacity

      // 카메라 바디
      ctx.fillStyle = isDarkMode ? "#333" : "#e9ecef"
      ctx.fillRect(centerX - 40, centerY - 30, 80, 60)

      // 카메라 렌즈
      ctx.beginPath()
      ctx.arc(centerX, centerY, 20 + scale * 15, 0, Math.PI * 2)
      ctx.fillStyle = isDarkMode ? "#111" : "#dee2e6"
      ctx.fill()

      // 렌즈 내부 원
      ctx.beginPath()
      ctx.arc(centerX, centerY, 15 + scale * 10, 0, Math.PI * 2)
      ctx.fillStyle = isDarkMode ? "#222" : "#ced4da"
      ctx.fill()

      // 가장 안쪽 렌즈 - 파란색으로 변경
      ctx.beginPath()
      ctx.arc(centerX, centerY, 10 + scale * 5, 0, Math.PI * 2)
      ctx.fillStyle = "#4285F4" // 구글 블루 색상
      ctx.fill()

      // 렌즈 반사광
      ctx.beginPath()
      ctx.arc(centerX - 5, centerY - 5, 3, 0, Math.PI * 2)
      ctx.fillStyle = "rgba(255, 255, 255, 0.7)"
      ctx.fill()

      ctx.restore()
    }

    let animationId: number

    const animate = () => {
      if (scale < 1) {
        scale += 0.01
      }

      if (opacity < 1) {
        opacity += 0.02
      }

      drawCamera()

      if (scale < 1 || opacity < 1) {
        animationId = requestAnimationFrame(animate)
      }
    }

    animate()

    return () => {
      window.removeEventListener("resize", setCanvasSize)
      cancelAnimationFrame(animationId)
    }
  }, [])

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-background">
      <canvas ref={canvasRef} className="w-full h-full" />
      <motion.div
        className="absolute bottom-10 text-foreground text-xl font-bold"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.5, duration: 1 }}
      >
        블랙박스 리뷰
      </motion.div>
    </div>
  )
}
