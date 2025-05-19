"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  ArrowLeft,
  Search,
  Upload,
  Clock,
  FileText,
  User,
  Filter,
  X,
} from "lucide-react"
import { Preferences } from "@capacitor/preferences"
import { useTheme } from "../contexts/theme-context"
import HistoryList from "@/components/history/history-list"
import EmptyHistory from "@/components/history/empty-history"
import type { AnalysisHistory } from "@/types/analysis"
import { getReportList, ReportListResponse } from "@/lib/api/Report"

export default function HistoryPage() {
  const router = useRouter()
  const { theme } = useTheme()
  const [isLoading, setIsLoading] = useState(true)
  const [isGuest, setIsGuest] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [showSearch, setShowSearch] = useState(false)
  const [showFilter, setShowFilter] = useState(false)
  const [selectedFilter, setSelectedFilter] = useState<string | null>(null)
  const [historyItems, setHistoryItems] = useState<AnalysisHistory[]>([])
  const [filteredItems, setFilteredItems] = useState<AnalysisHistory[]>([])
  const [autoDetectEnabled, setAutoDetectEnabled] = useState(true)

  useEffect(() => {
    async function init() {
      setIsLoading(true)

      // 1) 인증 상태 확인
      const { value: authToken } = await Preferences.get({ key: "AUTH_TOKEN" })
      const { value: guestToken } = await Preferences.get({
        key: "guest_token",
      })
      if (!authToken && !guestToken) {
        router.push("/")
        return
      }
      setIsGuest(!authToken && !!guestToken)

      // 2) 자동 감지 설정 확인
      const { value: autoDetect } = await Preferences.get({
        key: "AUTO_DETECT",
      })
      if (autoDetect !== null) {
        setAutoDetectEnabled(autoDetect === "true")
      }

      // 3) 보고서 목록 API 호출
      if (authToken) {
        try {
          const list: ReportListResponse[] = await getReportList()
          // ReportListResponse -> AnalysisHistory 매핑
          const mapped: AnalysisHistory[] = list.map((r) => ({
            id: r.id.toString(),
            title: r.title,
            date: new Date(r.createdAt).toLocaleString("ko-KR", {
              year: "numeric",
              month: "numeric",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            }),
            thumbnail: "/placeholder.svg",      // 썸네일 URL이 있으면 교체
            faultRatio: undefined,              // API에 없으면 비워두거나 따로 호출
            tags: [r.accidentCode],
            status: "completed",                // API에 status가 있으면 그에 맞게
          }))
          setHistoryItems(mapped)
          setFilteredItems(mapped)
        } catch (e) {
          console.error("보고서 목록 로드 실패", e)
          setHistoryItems([])
          setFilteredItems([])
        }
      }

      setIsLoading(false)
    }

    init()
  }, [router])

  // 검색·필터링
  useEffect(() => {
    let results = [...historyItems]
    if (searchQuery) {
      const q = searchQuery.toLowerCase()
      results = results.filter(
        (it) =>
          it.title.toLowerCase().includes(q) ||
          it.tags.some((t) => t.toLowerCase().includes(q))
      )
    }
    if (selectedFilter) {
      results = results.filter((it) => it.status === selectedFilter)
    }
    setFilteredItems(results)
  }, [searchQuery, selectedFilter, historyItems])

  const handleBack = () => router.back()
  const handleItemClick = (id: string) => router.push(`/analysis?id=${id}`)
  const handleUpload = () => router.push("/upload")
  const handleProfileClick = () =>
    isGuest ? router.push("/login") : router.push("/profile")
  const handleAutoDetectSettings = () => router.push("/profile")
  const handleDashboard = () => router.push("/dashboard")
  const toggleSearch = () => {
    setShowSearch((v) => !v)
    if (showSearch) setSearchQuery("")
  }
  const toggleFilter = () => {
    setShowFilter((v) => !v)
    if (showFilter) setSelectedFilter(null)
  }
  const applyFilter = (filter: string | null) => {
    setSelectedFilter(filter)
    setShowFilter(false)
  }

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="w-8 h-8 border-4 border-appblue border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground pb-16">
      {/* 헤더 */}
      <header className="app-header">
        <div className="flex items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleBack}
            className="mr-2 text-white"
          >
            <ArrowLeft size={20} />
          </Button>
          <h1 className="text-xl font-bold">분석 내역</h1>
        </div>
        <div className="flex items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleSearch}
            className="text-white"
          >
            <Search size={20} />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleFilter}
            className="text-white"
          >
            <Filter size={20} />
          </Button>
        </div>
      </header>

      {/* 검색 */}
      {showSearch && (
        <div className="p-4 bg-card border-b border-border">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" size={16} />
            <Input
              type="text"
              placeholder="제목 또는 태그로 검색"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 pr-9 bg-background"
            />
            {searchQuery && (
              <Button
                variant="ghost"
                size="icon"
                className="absolute right-1 top-1/2 transform -translate-y-1/2 h-7 w-7"
                onClick={() => setSearchQuery("")}
              >
                <X size={16} />
              </Button>
            )}
          </div>
        </div>
      )}

      {/* 필터 */}
      {showFilter && (
        <div className="p-4 bg-card border-b border-border">
          <div className="flex flex-wrap gap-2">
            <Button
              variant={selectedFilter === null ? "default" : "outline"}
              size="sm"
              onClick={() => applyFilter(null)}
              className={selectedFilter === null ? "bg-appblue text-white" : ""}
            >
              전체
            </Button>
            <Button
              variant={selectedFilter === "completed" ? "default" : "outline"}
              size="sm"
              onClick={() => applyFilter("completed")}
              className={selectedFilter === "completed" ? "bg-appblue text-white" : ""}
            >
              분석 완료
            </Button>
            <Button
              variant={selectedFilter === "processing" ? "default" : "outline"}
              size="sm"
              onClick={() => applyFilter("processing")}
              className={selectedFilter === "processing" ? "bg-appblue text-white" : ""}
            >
              분석 중
            </Button>
          </div>
        </div>
      )}

      {/* 리스트 */}
      <main className="app-container">
        {filteredItems.length > 0 ? (
          <HistoryList items={filteredItems} onItemClick={handleItemClick} />
        ) : (
          <EmptyHistory onUpload={handleUpload} />
        )}
      </main>

      {/* 네비게이션 */}
      <nav className="fixed bottom-0 left-0 right-0 bg-card border-t border-border">
        <div className="flex justify-around">
          <Button
            variant="ghost"
            className="flex-1 flex flex-col items-center py-3"
            onClick={handleUpload}
          >
            <Upload size={20} className="text-muted-foreground" />
            <span className="text-xs mt-1">업로드</span>
          </Button>
          <Button
            variant="ghost"
            className="flex-1 flex flex-col items-center py-3"
            onClick={handleDashboard}
          >
            <Clock size={20} className="text-muted-foreground" />
            <span className="text-xs mt-1">대시보드</span>
          </Button>
          <Button
            variant="ghost"
            className="flex-1 flex flex-col items-center py-3 text-appblue"
          >
            <FileText size={20} className="text-appblue" />
            <span className="text-xs mt-1">분석내역</span>
          </Button>
          <Button
            variant="ghost"
            className="flex-1 flex flex-col items-center py-3"
            onClick={handleProfileClick}
          >
            <User size={20} className="text-muted-foreground" />
            <span className="text-xs mt-1">프로필</span>
          </Button>
        </div>
      </nav>
    </div>
  )
}
