export interface AnalysisHistory {
  id: string
  title: string
  date: string
  thumbnail: string
  faultA?: string
  faultB?: string
  tags: string[]
  status: "completed" | "processing" | "failed"
}
