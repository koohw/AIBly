import type React from "react"
import "./globals.css"
import type { Metadata } from "next"
import { ThemeProvider } from "./contexts/theme-context"

export const metadata: Metadata = {
  title: "블랙박스 리뷰 - AI 교통사고 과실 판단 시스템",
  description: "차량 블랙박스 영상을 AI로 분석하여 교통사고 과실 비율을 자동으로 산정해주는 서비스",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />
      </head>
      <body>
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  )
}
