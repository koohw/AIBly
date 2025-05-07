"use client"

import type React from "react"

import { useState, useRef } from "react"
import { motion } from "framer-motion"
import Image from "next/image"
import { useRouter } from "next/navigation"
import { ChevronLeft, ChevronRight } from "lucide-react"

export default function AuthScreen() {
  const [currentSlide, setCurrentSlide] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [touchStart, setTouchStart] = useState(0)
  const [touchEnd, setTouchEnd] = useState(0)
  const sliderRef = useRef<HTMLDivElement>(null)
  const router = useRouter()

  const slides = [
    {
      title: "설치만 하세요, 나머지는 자동으로",
      description: "앱을 설치해 두면 사고 발생 시 블랙박스 영상을 자동으로 감지하고 분석 보고서를 생성합니다.",
      images: [
        "/car-dashboard-view.png",
        "/app-in-hand.png",
        "/data-analysis-laptop.png",
        "/digital-accident-report.png",
      ],
    },
    {
      title: "실시간 분석, 즉각적인 결과",
      description: "블랙박스 영상을 업로드하면 AI가 즉시 분석하여 결과를 제공합니다.",
      images: [
        "/ai-video-analysis.png",
        "/intersection-incident.png",
        "/troubleshooting-flowchart.png",
        "/stamped-contract.png",
      ],
    },
    {
      title: "법적 근거와 함께 제공되는 결과",
      description: "모든 분석 결과는 대한민국 교통법규에 기반한 법적 근거와 함께 제공됩니다.",
      images: [
        "/balanced-justice.png",
        "/korean-traffic-law-book-cover.png",
        "/accident-analysis-pie-charts.png",
        "/business-deal-clinch.png",
      ],
    },
  ]

  // 터치 이벤트 핸들러
  const handleTouchStart = (e: React.TouchEvent) => {
    setTouchStart(e.targetTouches[0].clientX)
  }

  const handleTouchMove = (e: React.TouchEvent) => {
    setTouchEnd(e.targetTouches[0].clientX)
  }

  const handleTouchEnd = () => {
    if (touchStart - touchEnd > 75) {
      // 왼쪽으로 스와이프
      handleNextSlide()
    }

    if (touchStart - touchEnd < -75) {
      // 오른쪽으로 스와이프
      handlePrevSlide()
    }
  }

  const handleNextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % slides.length)
  }

  const handlePrevSlide = () => {
    setCurrentSlide((prev) => (prev === 0 ? slides.length - 1 : prev - 1))
  }

  // 다음에 하기 버튼 - 대시보드로 이동
  const handleSkip = () => {
    // 비로그인 상태로 대시보드 접근을 위해 임시 토큰 저장
    localStorage.setItem("guest_token", "temporary_access")
    router.push("/dashboard")
  }

  const handleKakaoLogin = async () => {
    setIsLoading(true)
    try {
      // 실제 구현에서는 카카오 SDK를 사용하여 로그인 처리
      // 여기서는 예시로 setTimeout을 사용하여 로딩 상태 표시
      await new Promise((resolve) => setTimeout(resolve, 1500))

      // 로그인 성공 시 로컬 스토리지에 토큰 저장 (실제로는 서버에서 받은 토큰 저장)
      localStorage.setItem("auth_token", "kakao_token_example")

      // 메인 페이지로 이동
      router.push("/dashboard")
    } catch (error) {
      console.error("카카오 로그인 실패:", error)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-black text-white">
      {/* 슬라이드 섹션 */}
      <div
        className="flex-1 flex flex-col items-center justify-center p-6 relative"
        ref={sliderRef}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        <div className="w-full max-w-md">
          {/* 이미지 그리드 */}
          <div className="grid grid-cols-2 gap-4 mb-8">
            {slides[currentSlide].images.map((image, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="relative border border-gray-700 rounded-lg overflow-hidden"
              >
                <img
                  src={image || "/placeholder.svg"}
                  alt={`Feature illustration ${index + 1}`}
                  className="w-full h-auto"
                />
              </motion.div>
            ))}
          </div>

          {/* 텍스트 콘텐츠 */}
          <motion.div
            key={currentSlide}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-8"
          >
            <h2 className="text-2xl font-bold mb-2">{slides[currentSlide].title}</h2>
            <p className="text-gray-400">{slides[currentSlide].description}</p>
          </motion.div>

          {/* 슬라이드 인디케이터 및 네비게이션 */}
          <div className="flex items-center justify-center mb-8">
            <button onClick={handlePrevSlide} className="p-2 text-gray-400 hover:text-white" aria-label="이전 슬라이드">
              <ChevronLeft size={20} />
            </button>

            <div className="flex space-x-2 mx-4">
              {slides.map((_, index) => (
                <button
                  key={index}
                  onClick={() => setCurrentSlide(index)}
                  className={`w-2 h-2 rounded-full transition-colors ${
                    currentSlide === index ? "bg-red-600" : "bg-gray-600"
                  }`}
                  aria-label={`슬라이드 ${index + 1}`}
                />
              ))}
            </div>

            <button onClick={handleNextSlide} className="p-2 text-gray-400 hover:text-white" aria-label="다음 슬라이드">
              <ChevronRight size={20} />
            </button>
          </div>
        </div>
      </div>

      {/* 로그인/회원가입 버튼 */}
      <div className="p-6 space-y-4">
        <button
          onClick={handleKakaoLogin}
          disabled={isLoading}
          className="w-full py-4 flex items-center justify-center space-x-2 text-black font-medium rounded-md bg-[#FEE500] hover:bg-[#FDD835] transition-colors"
        >
          {isLoading ? (
            <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
          ) : (
            <>
              <Image src="/kakao-logo.png" alt="Kakao" width={24} height={24} className="mr-2" />
              카카오톡으로 로그인하기
            </>
          )}
        </button>

        <div className="relative flex items-center justify-center">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-700"></div>
          </div>
          <div className="relative px-4 text-sm text-gray-400 bg-black">또는</div>
        </div>

        <button
          onClick={handleSkip}
          className="w-full py-4 text-gray-300 font-medium rounded-md border border-gray-700 hover:bg-gray-900 transition-colors"
        >
          다음에 하기
        </button>

        <p className="text-xs text-center text-gray-500 mt-6">
          로그인하면 블랙박스 리뷰의 <span className="text-gray-400">서비스 이용약관</span>과{" "}
          <span className="text-gray-400">개인정보 처리방침</span>에 동의하게 됩니다.
        </p>
      </div>
    </div>
  )
}
