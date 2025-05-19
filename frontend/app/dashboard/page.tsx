"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Upload, Camera, Clock, FileText, AlertCircle, User, ChevronRight, Bell } from "lucide-react"
import LoginRequiredModal from "@/components/start/login-required-modal"
import { useTheme } from "../contexts/theme-context"
import { Preferences } from '@capacitor/preferences'
import { getRecentReports, ReportListItem } from "@/lib/api/Report"

export default function Dashboard() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(true)
  const [isGuest, setIsGuest] = useState(false)
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [hasAnalysis, setHasAnalysis] = useState(false) // 분석 결과가 있는지 여부
  const [autoDetectEnabled, setAutoDetectEnabled] = useState(true) // 자동 감지 활성화 여부
  const [newReports, setNewReports] = useState<ReportListItem[]>([]) // 새로운 보고서 목록
  const [recentReports, setRecentReports] = useState<ReportListItem[]>([]) // 최근 보고서 목록
  const [showNewReportAlert, setShowNewReportAlert] = useState(false) // 새 보고서 알림 표시 여부
  const { theme } = useTheme()

  // 인증 상태 확인
  useEffect(() => {
    async function checkAuth() {
      setIsLoading(true)
      setIsLoading(true)

      // 1) Preferences에서 JWT 꺼내기
      const { value: authToken } = await Preferences.get({ key: 'AUTH_TOKEN' })

      if (authToken) {
        // 로그인된 사용자
        setIsGuest(false)
        
        // FCM 토큰 콘솔에 출력 (디버그용)
        if (process.env.NODE_ENV === 'development') {
          try {
            const { value: fcmToken } = await Preferences.get({ key: 'fcm_token' });
            console.log('🔑 저장된 FCM 토큰:', fcmToken);
            
            // FCM 새 보고서 ID 목록 확인
            const { value: newReportIdsStr } = await Preferences.get({ key: 'NEW_REPORT_IDS' });
            console.log('📋 저장된 새 보고서 ID 목록:', newReportIdsStr);
          } catch (e) {
            console.error('FCM 토큰 확인 실패:', e);
          }
        }
        
        // 안드로이드 브릿지에서 새 보고서 목록 동기화
        try {
          if (typeof window !== 'undefined' && (window as any).androidFcmBridge) {
            // 안드로이드 네이티브에서 새 보고서 ID 목록 가져오기
            const newReportIdsJson = (window as any).androidFcmBridge.getNewReportIds();
            console.log('안드로이드에서 가져온 새 보고서 ID 목록:', newReportIdsJson);
            
            // Capacitor Preferences에 저장
            if (newReportIdsJson && newReportIdsJson !== '[]') {
              await Preferences.set({ key: 'NEW_REPORT_IDS', value: newReportIdsJson });
            }
          }
        } catch (error) {
          console.error('네이티브 브릿지에서 새 보고서 ID 가져오기 실패:', error);
        }
        
        try {
          // 보고서 목록 가져오기
          console.log('보고서 목록 조회 시작');
          const reportsData = await getRecentReports(5);
          console.log('API 응답 데이터 형식:', typeof reportsData, Array.isArray(reportsData));
          console.log('원본 API 응답:', JSON.stringify(reportsData).substring(0, 200) + '...');
          
          // API 응답이 배열일 경우와 객체일 경우 모두 처리
          let reportsList: ReportListItem[] = [];
          
          if (Array.isArray(reportsData)) {
            reportsList = reportsData;
          } else if (reportsData && typeof reportsData === 'object') {
            // reports 속성이 있는지 확인
            if ('reports' in reportsData && Array.isArray(reportsData.reports)) {
              reportsList = reportsData.reports;
            }
          }
          
          console.log('처리된 보고서 목록 갯수:', reportsList.length);
          console.log('처리된 보고서 목록 내용:', JSON.stringify(reportsList.slice(0, 2)));
          
          if (reportsList.length > 0) {
            console.log('최근 보고서 목록 처리 완료, 상태 업데이트');
            setHasAnalysis(true);
            setRecentReports(reportsList);
            
            // 테스트용 코드: reportsList의 각 항목이 올바른 구조를 갖고 있는지 확인
            reportsList.forEach((report, index) => {
              console.log(`보고서 ${index + 1}:`, 
                `ID=${report.reportId}`, 
                `제목=${report.title}`, 
                `생성일=${report.createdAt}`,
                `타입=${report.accidentType}`);
            });
            
            // 로컬 스토리지에서 새 보고서 ID 목록 가져오기
            const { value: newReportIdsStr } = await Preferences.get({ key: 'NEW_REPORT_IDS' });
            console.log('새 보고서 ID 문자열:', newReportIdsStr);
            const newReportIds = newReportIdsStr ? JSON.parse(newReportIdsStr) : [];
            console.log('파싱된 새 보고서 ID 목록:', newReportIds);
            
            if (newReportIds.length > 0) {
              // 새 보고서 ID가 있는 보고서만 필터링
              const newReportsList = reportsList.filter(report => 
                newReportIds.includes(report.reportId.toString())
              );
              console.log('새 보고서로 필터링된 목록:', newReportsList.length);
              setNewReports(newReportsList);
              setShowNewReportAlert(newReportsList.length > 0);
            }
          } else {
            console.log('보고서 목록이 비어있음');
            setHasAnalysis(false);
            
            // 토큰 확인 및 로깅 (디버깅용)
            const { value: authToken } = await Preferences.get({ key: 'AUTH_TOKEN' });
            console.log('현재 저장된 토큰(일부):', authToken ? authToken.substring(0, 15) + '...' : '토큰 없음');
            
            // 게스트 모드인지 확인
            const { value: guestToken } = await Preferences.get({ key: 'guest_token' });
            console.log('게스트 토큰 존재:', !!guestToken);
          }
        } catch (error) {
          console.error('보고서 목록 조회 실패:', error);
          
          // 에러 발생 시에도 UI 상태 업데이트
          setHasAnalysis(false);
          
          // 페이지가 로드되었음을 표시
          setIsLoading(false);
          
          // 오류 알림 표시 (선택적)
          alert('보고서 목록을 불러오는 중 오류가 발생했습니다.');
          
          // 개발 모드에서만 콘솔에 자세한 오류 출력
          if (process.env.NODE_ENV === 'development') {
            console.error('상세 오류 정보:', error);
          }
        }
      } else {
        // 게스트 모드
        setIsGuest(true)
      }

      // 2) 자동 감지 설정도 Preferences에서 꺼내기
      const { value: autoDetect } = await Preferences.get({ key: 'AUTO_DETECT' })
      if (autoDetect !== null) {
        setAutoDetectEnabled(autoDetect === 'true')
      }

      setIsLoading(false)
    }
    checkAuth()
  }, [router])

  const handleLogout = () => {
    Preferences.remove({ key: "AUTH_TOKEN" })
    localStorage.removeItem("auth_token")
    router.push("/")
  }

  const handleLogin = () => {
    router.push("/login")
  }

  const handleProfileClick = () => {
    if (isGuest) {
      router.push("/login")
    } else {
      router.push("/profile")
    }
  }

  const handleUpload = () => {
    if (isGuest) {
      setShowLoginModal(true)
    } else {
      router.push("/upload")
    }
  }

  const handleViewAnalysis = (reportId?: number) => {
    // 특정 보고서 ID가 있으면 해당 보고서로, 없으면 기본값 사용
    const id = reportId || 1;
    router.push(`/analysis?id=${id}`);
  }
  
  const handleViewNewReport = async (reportId: number) => {
    try {
      // 1. 현재 저장된 새 보고서 ID 목록 가져오기
      const { value: newReportIdsStr } = await Preferences.get({ key: 'NEW_REPORT_IDS' });
      const newReportIds = newReportIdsStr ? JSON.parse(newReportIdsStr) : [];
      
      // 2. 열람한 보고서 ID 제거
      const updatedIds = newReportIds.filter((id: string) => id !== reportId.toString());
      
      // 3. 업데이트된 목록 저장
      const updatedIdsJson = JSON.stringify(updatedIds);
      await Preferences.set({ key: 'NEW_REPORT_IDS', value: updatedIdsJson });
      
      // 4. 안드로이드 네이티브에도 업데이트 반영
      if (typeof window !== 'undefined' && (window as any).androidFcmBridge) {
        try {
          (window as any).androidFcmBridge.updateNewReportIds(updatedIdsJson);
        } catch (error) {
          console.error('안드로이드 새 보고서 ID 목록 업데이트 실패:', error);
        }
      }
      
      // 5. 상세 페이지로 이동
      handleViewAnalysis(reportId);
    } catch (error) {
      console.error('보고서 열람 처리 중 오류:', error);
      handleViewAnalysis(reportId);
    }
  }

  const handleAutoDetectSettings = () => {
    router.push("/profile")
  }
  
  const handleDismissAlert = () => {
    setShowNewReportAlert(false);
  }

  const handleHistory = () => {
    router.push("/history")
  }

  // 과실비율 표시를 위한 헬퍼 함수
  const renderFaultRatio = (faultRatio?: string) => {
    if (!faultRatio) return null;
    
    return (
      <span className="text-xs bg-appblue/20 text-appblue px-2 py-0.5 rounded mr-1">
        과실비율 {faultRatio}
      </span>
    );
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
        <h1 className="text-xl font-bold">블랙박스 리뷰</h1>
        {isGuest ? (
          <Button variant="outline" size="sm" onClick={handleLogin} className="bg-white text-appblue border-none">
            로그인
          </Button>
        ) : (
          <Button variant="ghost" size="sm" onClick={handleLogout} className="text-white hover:bg-white/20">
            로그아웃
          </Button>
        )}
      </header>

      {/* 게스트 모드 알림 */}
      {isGuest && (
        <div className="mx-4 mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700/50 rounded-md flex items-start">
          <AlertCircle className="text-yellow-500 mr-2 flex-shrink-0 mt-0.5" size={16} />
          <div className="text-sm">
            <p className="text-yellow-700 dark:text-yellow-500 font-medium">게스트 모드로 접속 중입니다</p>
            <p className="text-yellow-600/70 dark:text-yellow-500/70 text-xs">모든 기능을 사용하려면 로그인하세요</p>
          </div>
        </div>
      )}

      {/* 새로운 보고서 알림 */}
      {showNewReportAlert && newReports.length > 0 && (
        <div className="mx-4 mt-4 p-3 bg-appblue/10 dark:bg-appblue/20 border border-appblue/30 rounded-md">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center">
              <Bell className="text-appblue mr-2 flex-shrink-0" size={18} />
              <div>
                <h3 className="font-medium text-appblue">새로운 보고서가 생성되었습니다</h3>
                <p className="text-sm text-gray-600 dark:text-gray-300">
                  {newReports.length}개의 새 보고서가 준비되었습니다
                </p>
              </div>
            </div>
            <Button variant="ghost" size="sm" className="text-gray-500 p-1 h-8 w-8" onClick={handleDismissAlert}>
              <span className="sr-only">닫기</span>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
            </Button>
          </div>
          
          {newReports.map((report) => (
            <div 
              key={report.reportId}
              className="bg-white dark:bg-gray-800 rounded-md p-3 mb-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              onClick={() => handleViewNewReport(report.reportId)}
            >
              <div className="flex items-center">
                <div className="mr-3 flex-shrink-0 w-2 h-2 bg-appblue rounded-full"></div>
                <div className="flex-1">
                  <p className="font-medium text-sm">{report.title}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{report.accidentType}</p>
                </div>
                <Button variant="outline" size="sm" className="text-xs px-3 py-1 h-7 border-appblue text-appblue hover:bg-appblue/10">
                  보고서 보기
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 메인 콘텐츠 */}
      <main className="app-container">
        {/* 자동 감지 상태 표시 */}
        <section className="mb-6 app-card p-4">
          <div className="flex items-center">
            {autoDetectEnabled ? (
              <div className="w-3 h-3 bg-green-500 rounded-full mr-2 relative">
                <span className="absolute w-3 h-3 bg-green-500 rounded-full animate-ping opacity-75"></span>
              </div>
            ) : (
              <div className="w-3 h-3 bg-gray-400 rounded-full mr-2"></div>
            )}
            <div className="flex-1">
              <h3 className="font-medium">자동 감지 {autoDetectEnabled ? "활성화됨" : "비활성화됨"}</h3>
              <p className="text-xs text-muted-foreground">
                {autoDetectEnabled
                  ? "블랙박스 영상이 감지되면 자동으로 분석합니다"
                  : "블랙박스 영상을 수동으로 업로드해야 합니다"}
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="text-xs text-appblue hover:text-appblue-dark"
              onClick={handleAutoDetectSettings}
            >
              설정
            </Button>
          </div>
        </section>

        <section className="mb-8">
          <h2 className="app-section-title">영상 분석하기</h2>
          <div className="grid grid-cols-2 gap-4">
            <Button
              variant="outline"
              className="h-32 flex flex-col items-center justify-center space-y-2 border-border bg-card hover:border-appblue hover:bg-appblue/5"
              onClick={handleUpload}
            >
              <div className="w-12 h-12 rounded-full bg-appblue flex items-center justify-center">
                <Upload size={24} className="text-white" />
              </div>
              <span>영상 업로드</span>
            </Button>
            <Button
              variant="outline"
              className="h-32 flex flex-col items-center justify-center space-y-2 border-border bg-card hover:border-appblue hover:bg-appblue/5"
              onClick={handleUpload}
            >
              <div className="w-12 h-12 rounded-full bg-appblue flex items-center justify-center">
                <Camera size={24} className="text-white" />
              </div>
              <span>카메라 촬영</span>
            </Button>
          </div>
        </section>

        <section>
          <div className="flex justify-between items-center mb-4">
            <h2 className="app-section-title">최근 분석</h2>
            <Button variant="link" className="text-appblue p-0 hover:text-appblue-dark" onClick={handleHistory}>
              모두 보기
            </Button>
          </div>

          {hasAnalysis && recentReports.length > 0 ? (
            <div className="app-card overflow-hidden">
              {recentReports.map((report) => (
                <div 
                  key={report.reportId} 
                  className="p-4 border-b border-border flex items-center cursor-pointer" 
                  onClick={() => handleViewAnalysis(report.reportId)}
                >
                  <div className="w-16 h-16 bg-muted rounded-md mr-3 flex-shrink-0 overflow-hidden">
                    {report.thumbnailUrl ? (
                      <img
                        src={report.thumbnailUrl}
                        alt={`${report.title} 썸네일`}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full bg-appblue/20 flex items-center justify-center">
                        <FileText size={24} className="text-appblue" />
                      </div>
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center">
                      <h3 className="font-medium">{report.title || '제목 없음'}</h3>
                      {report.isNew && (
                        <span className="ml-2 px-2 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 text-xs rounded-full">NEW</span>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {new Date(report.createdAt).toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </p>
                    <div className="flex mt-1">
                      <span className="text-xs bg-appblue/20 text-appblue px-2 py-0.5 rounded mr-1">
                        {report.accidentType || '일반 사고'}
                      </span>
                      {report.faultRatio && renderFaultRatio(report.faultRatio)}
                    </div>
                  </div>
                  <ChevronRight size={20} className="text-muted-foreground" />
                </div>
              ))}
            </div>
          ) : (
            <div className="app-card p-6 text-center">
              <div className="w-16 h-16 rounded-full bg-appblue mx-auto mb-4 flex items-center justify-center">
                <Clock className="text-white" size={32} />
              </div>
              <h3 className="text-lg font-medium mb-2">분석 내역이 없습니다</h3>
              <p className="text-muted-foreground mb-4">블랙박스 영상을 업로드하여 AI 분석을 시작해보세요.</p>
              <Button className="app-blue-button" onClick={handleUpload}>
                <Upload className="mr-2 h-4 w-4" /> 영상 업로드하기
              </Button>
            </div>
          )}
        </section>
      </main>

      {/* 하단 네비게이션 */}
      <nav className="fixed bottom-0 left-0 right-0 bg-card border-t border-border">
        <div className="flex justify-around">
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleUpload}>
            <Upload size={20} className="text-appblue" />
            <span className="text-xs mt-1">업로드</span>
          </Button>
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleHistory}>
            <Clock size={20} className="text-muted-foreground" />
            <span className="text-xs mt-1">분석내역</span>
          </Button>
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleAutoDetectSettings}>
            <div className="relative">
              {autoDetectEnabled ? (
                <div className="absolute -top-1 -right-1 w-2 h-2 bg-green-500 rounded-full"></div>
              ) : null}
              <FileText size={20} className={autoDetectEnabled ? "text-appblue" : "text-muted-foreground"} />
            </div>
            <span className="text-xs mt-1">자동감지</span>
          </Button>
          <Button variant="ghost" className="flex-1 flex flex-col items-center py-3" onClick={handleProfileClick}>
            {isGuest ? (
              <>
                <User size={20} className="text-muted-foreground" />
                <span className="text-xs mt-1">로그인</span>
              </>
            ) : (
              <>
                <User size={20} className="text-appblue" />
                <span className="text-xs mt-1">프로필</span>
              </>
            )}
          </Button>
        </div>
      </nav>

      {/* 로그인 필요 모달 */}
      <LoginRequiredModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />
    </div>
  )
}
