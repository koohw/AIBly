"use client"

import React, { useState, useRef, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Play, Pause, Calendar, Clock, Download, Share, AlertCircle } from "lucide-react"
import { useTheme } from "@/app/contexts/theme-context"
import { FormattedTexts } from "@/components/analysis/FormattedText"
import { downloadReportPdf, getReportDetail, ReportDetailResponse } from "@/lib/api/Report"
import { Capacitor } from "@capacitor/core"
import { Browser } from "@capacitor/browser"

// 타임라인 항목 타입
interface Timeline {
  time: string;
  description: string;
}

// 과실 비율 계산 항목 타입
interface FaultCalculation {
  title: string;
  ratio: string;
}

// 확장 데이터 타입 (UI를 위한 추가 필드)
interface ReportExtendedData {
  timeline?: Timeline[];
  lawReference?: string[];
  caseReference?: string[];
  faultCalculation?: FaultCalculation[];
  videoUrl?: string;
  accidentDate?: string;
  accidentLocation?: string;
}

// 텍스트 포맷팅 컴포넌트
const FormattedText = ({ text }: { text: string }) => {
  // 문자열을 줄바꿈 기준으로 분리
  return (
    <>
      {text?.split('\n').map((line, i) => (
        <React.Fragment key={i}>
          {line}
          {i < text.split('\n').length - 1 && <br />}
        </React.Fragment>
      ))}
    </>
  );
};

export default function ClientReport({ id }: { id: string }) {
  const router = useRouter()
  const { theme } = useTheme()
  // 로딩/에러/데이터 상태
  const [report, setReport] = useState<ReportDetailResponse | null>(null)
  const [extendedData, setExtendedData] = useState<ReportExtendedData>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const videoRef = useRef<HTMLVideoElement>(null)
  const videoTotalDuration = 30
  const [downloading, setDownloading] = useState(false)

  // 보고서 데이터 가져오기
  useEffect(() => {
    async function fetchReportData() {
      try {
        setLoading(true);
        const data = await getReportDetail(id);
        setReport(data);
        
        // 확장 데이터 생성 (실제 API에 필드가 추가되면 이 부분은 수정 필요)
        setExtendedData({
          timeline: [
            { time: "14:30:05", description: "상대방 차량 신호 위반" },
            { time: "14:30:08", description: "사용자 차량 회피 시도" },
            { time: "14:30:10", description: "충돌 발생" }
          ],
          lawReference: ["도로교통법 제15조: 차량차선 규정"],
          caseReference: ["서울지방법원 2019-xxxxxxx (유사 판결)"],
          faultCalculation: [
            { title: "기본 과실 비율: 30:70 (사용자:상대방)", ratio: "30:70" },
            { title: "신호 위반: +10%p (상대방)", ratio: "+10%" },
            { title: "회피 과실 비율: 30:70 (사용자:상대방)", ratio: "30:70" }
          ],
          accidentDate: "2025년 4월 15일 오후 2:30",
          accidentLocation: "교차로 중앙",
          videoUrl: "/car-accident-dashcam.png"
        });
        
        setError(null);
      } catch (err) {
        setError('보고서를 불러오는 중 오류가 발생했습니다.');
        console.error('보고서 로딩 오류:', err);
      } finally {
        setLoading(false);
      }
    }
    
    fetchReportData();
  }, [id]);

  // 과실 비율을 문자열로 변환
  const formatFaultRatio = (value: number | undefined) => {
    if (value === undefined) return "-";
    return value + "%";
  };

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

  const handleDownload = async () => {
    setDownloading(true)
    try {
      const pdfUrl = await downloadReportPdf(id)
      if (pdfUrl) {
        // 모바일 앱 환경에서는 Browser API를 사용
        if (Capacitor.isNativePlatform()) {
          await Browser.open({ url: pdfUrl })
        } else {
          // 웹에서는 새 창에서 열기
          window.open(pdfUrl, '_blank')
        }
      }
    } catch (error) {
      console.error("Error downloading report:", error)
      alert('보고서 다운로드 중 오류가 발생했습니다.')
    } finally {
      setDownloading(false)
    }
  }

  // 로딩 중 표시
  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-appblue border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  // 에러 표시
  if (error) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4">
        <div className="text-red-500 mb-4">
          <AlertCircle size={48} />
        </div>
        <h2 className="text-xl font-bold mb-2">오류가 발생했습니다</h2>
        <p className="text-muted-foreground mb-4">{error}</p>
        <Button onClick={handleBack}>돌아가기</Button>
      </div>
    )
  }

  // 타임라인 데이터
  const timelineData = extendedData.timeline || [];

  // 참조 자료
  const lawReferenceData = extendedData.lawReference || [];
  const caseReferenceData = extendedData.caseReference || [];

  // 과실 계산 내역
  const faultCalculationData = extendedData.faultCalculation || [];

  // 사고 발생 일시
  const formattedAccidentDate = extendedData.accidentDate || "정보 없음";

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col pb-safe">
      {/* 헤더 */}
      <header className="app-header">
        <div className="flex items-center">
          <Button variant="ghost" size="icon" onClick={handleBack} className="mr-2 text-white">
            <ArrowLeft size={20} />
          </Button>
          <h1 className="text-xl font-bold">AI 사고 분석 보고서</h1>
        </div>
        <p className="text-sm mt-1 text-white/80">
          분석 완료: {report?.createdAt && new Date(report.createdAt).toLocaleString("ko-KR", {
            year: "numeric", month: "numeric", day: "numeric",
            hour: "2-digit", minute: "2-digit"
          })}
        </p>
      </header>

      {/* 주요 콘텐츠 영역 - 스크롤 가능 */}
      <div className="flex-1 overflow-y-auto pb-24">
        {/* 사고 일시 */}
        <div className="border border-border m-4 p-4 rounded-md bg-card">
          <div className="flex items-center text-muted-foreground">
            <Calendar className="mr-2" size={18} />
            <Clock className="mr-2" size={18} />
            <span>{formattedAccidentDate}</span>
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
              <div className="text-4xl font-bold text-appblue">{formatFaultRatio(report?.faultA)}</div>
              <div className="text-sm font-medium text-foreground">사용자</div>
            </div>
            <div className="h-12 border-r-2 border-border"></div>
            <div className="w-1/2 text-center">
              <div className="text-4xl font-bold text-red-500">{formatFaultRatio(report?.faultB)}</div>
              <div className="text-sm font-medium text-foreground">상대방</div>
            </div>
          </div>
        </div>

        {/* 비디오 플레이어 */}
        <div className="mx-4 mb-4 bg-black rounded-md overflow-hidden relative">
          <video
            ref={videoRef}
            src={extendedData.videoUrl || "/car-accident-dashcam.png"}
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
        <div className="mx-4 mb-4">
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
                  <span className="font-bold">{formatFaultRatio(report?.faultA)}</span>
                </div>
                <div className="bg-muted text-foreground p-3 rounded-md flex justify-between items-center">
                  <span>B 차량</span>
                  <span className="font-bold">{formatFaultRatio(report?.faultB)}</span>
                </div>

                <div className="mt-6">
                  <h3 className="text-sm font-medium mb-2 text-foreground">참조 판례 / 법률 근거</h3>
                  <div className="space-y-2">
                    {caseReferenceData.map((item, index) => (
                      <div key={`case-${index}`} className="flex items-start p-3 border border-border rounded-md bg-card">
                        <input type="checkbox" className="mt-1 mr-2 accent-appblue" checked readOnly />
                        <span className="text-foreground">{item}</span>
                      </div>
                    ))}
                    {lawReferenceData.map((item, index) => (
                      <div key={`law-${index}`} className="flex items-start p-3 border border-border rounded-md bg-card">
                        <input type="checkbox" className="mt-1 mr-2 accent-appblue" checked readOnly />
                        <span className="text-foreground">{item}</span>
                      </div>
                    ))}
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
                    {timelineData.map((item, index) => (
                      <li key={`timeline-${index}`} className={index < timelineData.length - 1 ? "pb-2 border-b border-border" : ""}>
                        <span className="text-appblue font-medium">{item.time}</span> {item.description}
                      </li>
                    ))}
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
                    <li>사고 발생 위치: {extendedData.accidentLocation || "정보 없음"}</li>
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
                      <FormattedText text={report?.laws || ''} />
                    </p>
                  </div>
                </div>

                <div className="app-card p-4">
                  <h3 className="font-medium mb-2 text-foreground">과실 비율 산정 기준</h3>
                  <ul className="text-sm space-y-2 text-muted-foreground">
                    {faultCalculationData.map((item, index) => (
                      <li key={`fault-calc-${index}`} className="flex justify-between">
                        <span>• {item.title}</span>
                      </li>
                    ))}
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
                      <FormattedText text={report?.precedents || ''} />
                    </p>
                  </div>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </div>

      {/* 하단 버튼 - 네이티브 코드에서 처리됨 */}
      <div className="fixed bottom-0 left-0 right-0 p-4 border-t border-border bg-card">
        <Button className="w-full app-blue-button py-6" onClick={handleDownload} disabled={downloading}>
          {downloading ? "다운로드 중..." : "보고서 저장 / 공유"}
        </Button>
      </div>
    </div>
  )
}