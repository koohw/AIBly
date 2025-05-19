"use client"

import React, { useState, useRef, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Play, Pause, Calendar, Clock } from "lucide-react"
import { useTheme } from "@/app/contexts/theme-context"
import { getReportDetail, ReportDetailResponse } from "@/lib/api/Report"
import { FormattedText } from "@/components/analysis/FormattedText"

export default function ClientReport({ id }: { id: string }) {
  const router = useRouter()
  const { theme } = useTheme()
  // 로딩/에러/데이터 상태
  const [report, setReport] = useState<ReportDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const videoRef = useRef<HTMLVideoElement>(null)
  const videoTotalDuration = 30

    // id가 바뀔 때마다 상세 조회
  useEffect(() => {
    if (!id) return
    setLoading(true)
    getReportDetail(id)
      .then((data) => {
        setReport(data)
      })
      .catch((err) => {
        console.error("보고서 상세 조회 실패:", (err as any).response?.data ?? err.message)
        setError("보고서를 불러오는 중 오류가 발생했습니다.")
      })
      .finally(() => setLoading(false))
  }, [id])

  const handleBack = () => router.back()
  const handlePlayPause = () => {
    if (!videoRef.current) return
    isPlaying ? videoRef.current.pause() : videoRef.current.play()
    setIsPlaying(!isPlaying)
  }
  const handleTimeUpdate = () => {
    if (videoRef.current) setCurrentTime(videoRef.current.currentTime)
  }
  const formatTime = (t: number) => {
    const m = String(Math.floor(t / 60)).padStart(2, "0")
    const s = String(Math.floor(t % 60)).padStart(2, "0")
    return `${m}:${s}`
  }
  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const t = Number(e.target.value)
    setCurrentTime(t)
    if (videoRef.current) videoRef.current.currentTime = t
  }

  // 로딩 및 에러 처리
  if (loading) {
    return <div className="p-4 text-center">보고서를 불러오는 중...</div>
  }
  if (error || !report) {
    return <div className="p-4 text-center text-red-500">{error}</div>
  }

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
          {/* 헤더 */}
          <header className="app-header">
            <div className="flex items-center">
              <Button variant="ghost" size="icon" onClick={handleBack} className="mr-2 text-white">
                <ArrowLeft size={20} />
              </Button>
              <h1 className="text-xl font-bold">AI 사고 분석 보고서</h1>
            </div>
            <p className="text-sm mt-1 text-white/80">
              분석 완료: {new Date(report.createdAt).toLocaleString("ko-KR", {
                year: "numeric", month: "numeric", day: "numeric",
                hour: "2-digit", minute: "2-digit"
              })}
            </p>
          </header>
    
          {/* 사고 일시 */}
          <div className="border border-border m-4 p-4 rounded-md bg-card">
            <div className="flex items-center text-muted-foreground">
              <Calendar className="mr-2" size={18} />
              <Clock className="mr-2" size={18} />
              <span>2025년 4월 15일 오후 2:30</span>
              <span>{report.createdAt}</span>
            </div>
          </div>
    
          {/* 과실 비율 판단 */}
          <div className="mx-4 mb-4">
            <h2 className="text-sm font-medium text-muted-foreground mb-2 flex items-center">
              <span className="inline-block w-4 h-4 bg-appblue rounded-full mr-2"></span>
              과실 비율 판단
            </h2>
            <div className="app-card p-4 flex items-center border-2 border-border">
              <div className="w-1/2 text-center">
                <div className="text-4xl font-bold text-appblue">{report.faultA}</div>
                <div className="text-sm font-medium text-foreground">사용자</div>
              </div>
              <div className="h-12 border-r-2 border-border"></div>
              <div className="w-1/2 text-center">
                <div className="text-4xl font-bold text-red-500">{report.faultB}</div>
                <div className="text-sm font-medium text-foreground">상대방</div>
              </div>
            </div>
          </div>
    
          {/* 비디오 플레이어 */}
          <div className="mx-4 mb-4 bg-black rounded-md overflow-hidden relative">
            <video
              ref={videoRef}
              src="/car-accident-dashcam.png"
              className="w-full h-auto"
              onTimeUpdate={handleTimeUpdate}
              onEnded={() => setIsPlaying(false)}
            />
            <div className="absolute inset-0 flex items-center justify-center">
              <button
                onClick={handlePlayPause}
                className="bg-appblue rounded-full w-12 h-12 flex items-center justify-center"
              >
                {isPlaying ? <Pause size={24} className="text-white" /> : <Play size={24} className="text-white" />}
              </button>
            </div>
          </div>
    
          {/* 비디오 컨트롤 */}
          <div className="mx-4 mb-4 flex items-center">
            <button onClick={handlePlayPause} className="mr-2 text-muted-foreground">
              {isPlaying ? <Pause size={20} /> : <Play size={20} />}
            </button>
            <input
              type="range"
              min="0"
              max={videoTotalDuration}
              value={currentTime}
              onChange={handleSeek}
              className="flex-1 mx-2 accent-appblue bg-muted h-1 rounded-full"
            />
            <span className="text-xs text-muted-foreground">
              {formatTime(currentTime)} / {formatTime(videoTotalDuration)}
            </span>
          </div>
    
          {/* 탭 콘텐츠 */}
          <div className="flex-1 mx-4 mb-4">
            <Tabs defaultValue="fault" className="w-full">
              <TabsList className="grid grid-cols-4 w-full bg-muted p-0">
                <TabsTrigger
                  value="fault"
                  className="py-3 rounded-none border-b-2 border-transparent data-[state=active]:border-appblue data-[state=active]:bg-transparent data-[state=active]:text-foreground"
                >
                  과실비율
                </TabsTrigger>
                <TabsTrigger
                  value="explanation"
                  className="py-3 rounded-none border-b-2 border-transparent data-[state=active]:border-appblue data-[state=active]:bg-transparent data-[state=active]:text-foreground"
                >
                  해설
                </TabsTrigger>
                <TabsTrigger
                  value="regulations"
                  className="py-3 rounded-none border-b-2 border-transparent data-[state=active]:border-appblue data-[state=active]:bg-transparent data-[state=active]:text-foreground"
                >
                  법규
                </TabsTrigger>
                <TabsTrigger
                  value="precedents"
                  className="py-3 rounded-none border-b-2 border-transparent data-[state=active]:border-appblue data-[state=active]:bg-transparent data-[state=active]:text-foreground"
                >
                  판례
                </TabsTrigger>
              </TabsList>
    
              {/* 과실비율 탭 */}
              <TabsContent value="fault" className="mt-4">
                <div className="space-y-4">
                  <div className="bg-appblue text-white p-3 rounded-md flex justify-between items-center">
                    <span>A 차량</span>
                    <span className="font-bold">{report.faultA}</span>
                  </div>
                  <div className="bg-muted text-foreground p-3 rounded-md flex justify-between items-center">
                    <span>B 차량</span>
                    <span className="font-bold">{report.faultB}</span>
                  </div>
    
                  <div className="mt-6">
                    <h3 className="text-sm font-medium mb-2 text-foreground">참조 판례 / 법률 근거</h3>
                    <div className="space-y-2">
                      <div className="flex items-start p-3 border border-border rounded-md bg-card">
                        <input type="checkbox" className="mt-1 mr-2 accent-appblue" checked readOnly />
                        <span className="text-foreground">서울지방법원 2019-xxxxxxx (유사 판결)</span>
                      </div>
                      <div className="flex items-start p-3 border border-border rounded-md bg-card">
                        <input type="checkbox" className="mt-1 mr-2 accent-appblue" checked readOnly />
                        <span className="text-foreground">도로교통법 제15조: 차량차선 규정</span>
                      </div>
                    </div>
                  </div>
                </div>
              </TabsContent>
    
              {/* 해설 탭 */}
              <TabsContent value="explanation" className="mt-4">
                <div className="space-y-4">
                  <div className="app-card p-4">
                    <h3 className="font-medium mb-2 text-foreground">주요 사고 상황</h3>
                    <ul className="space-y-2 text-sm">
                      <li className="pb-2 border-b border-border">
                        <span className="text-appblue font-medium">14:30:05</span> 상대방 차량 신호 위반
                      </li>
                      <li className="pb-2 border-b border-border">
                        <span className="text-appblue font-medium">14:30:08</span> 사용자 차량 회피 시도
                      </li>
                      <li>
                        <span className="text-appblue font-medium">14:30:10</span> 충돌 발생
                      </li>
                    </ul>
                  </div>
    
                  <div className="app-card p-4">
                    <h3 className="font-medium mb-3 text-foreground">사고 분석 결과</h3>
                    <div className="grid grid-cols-3 gap-2 mb-4">
                      <div className="bg-muted p-3 rounded text-center text-sm text-foreground">사고 모형파악</div>
                      <div className="bg-muted p-3 rounded text-center text-sm text-foreground">AI 분석 요약 결과</div>
                      <div className="bg-muted p-3 rounded text-center text-sm text-foreground">자료</div>
                    </div>
                    <ul className="list-disc pl-5 text-sm space-y-1 text-foreground">
                      <li>사고 발생 위치: 교차로 중앙</li>
                      <li>분석 결과 추가: 신호 위반 확인됨</li>
                    </ul>
                  </div>
                </div>
              </TabsContent>
    
              {/* 법규 탭 */}
              <TabsContent value="regulations" className="mt-4">
                <div className="space-y-4">
                  <div className="app-card p-4">
                    <h3 className="font-medium mb-2 text-foreground">관련 법규</h3>
                    <div className="text-sm">
                      <p className="font-medium mb-1 text-foreground">도로교통법 제16조</p>
                      <p className="text-muted-foreground mb-3">[차로 진입 차선 변경]</p>
                      <p className="text-muted-foreground">
                        <FormattedText text={report.laws} />
                      </p>
                    </div>
                  </div>
    
                  <div className="app-card p-4">
                    <h3 className="font-medium mb-2 text-foreground">과실 비율 산정 기준</h3>
                    <ul className="text-sm space-y-2 text-muted-foreground">
                      <li className="flex justify-between">
                        <span>• 기본 과실 비율: 30:70 (사용자:상대방)</span>
                      </li>
                      <li className="flex justify-between">
                        <span>• 신호 위반: +10%p (상대방)</span>
                      </li>
                      <li className="flex justify-between">
                        <span>• 회피 과실 비율: 30:70 (사용자:상대방)</span>
                      </li>
                    </ul>
                  </div>
                </div>
              </TabsContent>
    
              {/* 판례 탭 */}
              <TabsContent value="precedents" className="mt-4">
                <div className="space-y-4">
                  <div className="app-card p-4">
                    <h3 className="font-medium mb-2 text-foreground">유사 판례 사례</h3>
                    <div className="text-sm">
                      <p className="font-medium text-foreground">서울중앙지방법원 2019-xxxxxxx</p>
                      <p className="text-muted-foreground mb-2">판결일: 2019년 5월 12일</p>
                      <p className="text-muted-foreground">
                        <FormattedText text={report.precedents} />
                      </p>
                    </div>
                  </div>
                </div>
              </TabsContent>
            </Tabs>
          </div>
    
          {/* 하단 버튼 */}
          <div className="p-4 border-t border-border bg-card">
            <Button className="w-full app-blue-button py-6">보고서 저장 / 공유</Button>
          </div>
        </div>
  )
}