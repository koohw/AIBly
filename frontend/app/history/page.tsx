"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  ArrowLeft,
  Search,
  Upload,
  Clock,
  FileText,
  User,
  Filter,
  X,
} from "lucide-react";
import { Preferences } from "@capacitor/preferences";
import { useTheme } from "../contexts/theme-context";
import HistoryList from "@/components/history/history-list";
import EmptyHistory from "@/components/history/empty-history";
import type { AnalysisHistory } from "@/types/analysis";
import { getReportList, ReportListResponse } from "@/lib/api/Report";

export default function HistoryPage() {
  const router = useRouter();
  const { theme } = useTheme();
  const [isLoading, setIsLoading] = useState(true);
  const [isGuest, setIsGuest] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [showFilter, setShowFilter] = useState(false);
  const [selectedFilter, setSelectedFilter] = useState<string | null>(null);
  const [historyItems, setHistoryItems] = useState<ReportListResponse[]>([]);
  const [filteredItems, setFilteredItems] = useState<ReportListResponse[]>([]);
  const [autoDetectEnabled, setAutoDetectEnabled] = useState(true);

  useEffect(() => {
    async function init() {
      setIsLoading(true);

      // 1) 인증 상태 확인
      const { value: authToken } = await Preferences.get({ key: "AUTH_TOKEN" });
      const { value: guestToken } = await Preferences.get({
        key: "guest_token",
      });
      if (!authToken && !guestToken) {
        router.push("/");
        return;
      }
      setIsGuest(!authToken && !!guestToken);

      // 2) 자동 감지 설정 확인
      const { value: autoDetect } = await Preferences.get({
        key: "AUTO_DETECT",
      });
      if (autoDetect !== null) {
        setAutoDetectEnabled(autoDetect === "true");
      }

      // 3) 보고서 목록 API 호출
      if (authToken) {
        try {
          const list = await getReportList();
           setHistoryItems(list);
           setFilteredItems(list);
          // ReportListResponse -> AnalysisHistory 매핑
        } catch (e) {
          console.error("보고서 목록 로드 실패", e);
          setHistoryItems([]);
          setFilteredItems([]);
        }
      }

      setIsLoading(false);
    }

    init();
  }, [router]);

  // 검색·필터링
  // useEffect(() => {
  //   let results = [...historyItems];
  //   if (searchQuery) {
  //     const q = searchQuery.toLowerCase();
  //     results = results.filter(
  //       (it) =>
  //         it.title.toLowerCase().includes(q) ||
  //         it.tags.some((t) => t.toLowerCase().includes(q))
  //     );
  //   }
  //   if (selectedFilter) {
  //     results = results.filter((it) => it.status === selectedFilter);
  //   }
  //   setFilteredItems(results);
  // }, [searchQuery, selectedFilter, historyItems]);

  const handleBack = () => router.back();
  const handleItemClick = (id: number) => router.push(`/analysis?id=${id}`);
  const handleUpload = () => router.push("/upload");
  const handleProfileClick = () =>
    isGuest ? router.push("/login") : router.push("/profile");
  const handleAutoDetectSettings = () => router.push("/profile");
  const handleDashboard = () => router.push("/dashboard");
  const toggleSearch = () => {
    setShowSearch((v) => !v);
    if (showSearch) setSearchQuery("");
  };
  const toggleFilter = () => {
    setShowFilter((v) => !v);
    if (showFilter) setSelectedFilter(null);
  };
  const applyFilter = (filter: string | null) => {
    setSelectedFilter(filter);
    setShowFilter(false);
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="w-8 h-8 border-4 border-appblue border-t-transparent rounded-full animate-spin" />
      </div>
    );
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
            <Search
              className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground"
              size={16}
            />
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
              className={
                selectedFilter === "completed" ? "bg-appblue text-white" : ""
              }
            >
              분석 완료
            </Button>
            <Button
              variant={selectedFilter === "processing" ? "default" : "outline"}
              size="sm"
              onClick={() => applyFilter("processing")}
              className={
                selectedFilter === "processing" ? "bg-appblue text-white" : ""
              }
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
      {/* 네비게이션 */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-border h-20 z-50">
        <div className="flex justify-around items-center h-full">
          <Button
            variant="ghost"
            className="flex flex-col items-center justify-center gap-0.5 h-full py-2"
            onClick={handleUpload}
          >
            <Upload size={18} className="text-muted-foreground" />
            <span className="text-[10px]">업로드</span>
          </Button>

          <Button
            variant="ghost"
            className="flex flex-col items-center justify-center gap-0.5 h-full py-2"
            onClick={handleDashboard}
          >
            <Clock size={18} className="text-muted-foreground" />
            <span className="text-[10px]">대시보드</span>
          </Button>

          <Button
            variant="ghost"
            className="flex flex-col items-center justify-center gap-0.5 h-full py-2"
          >
            <FileText size={18} className="text-appblue" />
            <span className="text-[10px] text-appblue">분석내역</span>
          </Button>

          <Button
            variant="ghost"
            className="flex flex-col items-center justify-center gap-0.5 h-full py-2"
            onClick={handleProfileClick}
          >
            <User size={18} className="text-muted-foreground" />
            <span className="text-[10px]">프로필</span>
          </Button>
        </div>
      </nav>
    </div>
  );
}
