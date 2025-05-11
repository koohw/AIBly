'use client'

import React, { Suspense, useEffect } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import ClientReport from '@/components/analysis/ClientReport'

export default function AnalysisPage() {
  return (
    <Suspense fallback={<div className="p-4 text-center">보고서를 불러오는 중...</div>}>
      <AnalysisClient />
    </Suspense>
  )
}

function AnalysisClient() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const reportId = searchParams.get('id')

  useEffect(() => {
    if (!reportId) {
      router.replace('/')
    }
  }, [reportId, router])

  if (!reportId) {
    // reportId가 없을 땐 Suspense fallback이 있으니 별도 UI 없음
    return null
  }

  return <ClientReport id={reportId} />
}