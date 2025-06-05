// **'use client' 제거**

import React from 'react';
import './globals.css';
import type { Metadata } from 'next';
import { ThemeProvider } from './contexts/theme-context';
import DeepLink from './deeplink'; // 클라이언트 컴포넌트

export const metadata: Metadata = {
  title: '블랙박스 리뷰 - AI 교통사고 과실 판단 시스템',
  description:
    '차량 블랙박스 영상을 AI로 분석하여 교통사고 과실 비율을 자동으로 산정해주는 서비스',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        <script
          src="https://developers.kakao.com/sdk/js/kakao.min.js"
          async
        />
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover, height=device-height"
        />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="mobile-web-app-capable" content="yes" />
      </head>
      <body>
        {/* ① 클라이언트 전용 딥링크 리스너 */}
        <DeepLink />
        {/* ② 나머지 페이지 렌더링 */}
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}
