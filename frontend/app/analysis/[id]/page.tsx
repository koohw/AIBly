// app/analysis/[id]/page.tsx
import ClientReport from '@/components/analysis/ClientReport'

// .next/types/... 에 있던 SegmentParams를 직접 선언
type SegmentParams = { id: string }


export default async function AnalysisReportPage({
  params,
}: {
  params: Promise<SegmentParams>
}) {
  const { id } = await params
  return <ClientReport id={id} />
}
