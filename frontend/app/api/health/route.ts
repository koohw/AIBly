import { NextResponse } from "next/server";

// 정적 내보내기를 위한 설정 추가
// export const dynamic = 'force-static';

export async function GET() {
  return NextResponse.json(
    { 
      status: "healthy",
      timestamp: new Date().toISOString()
    },
    { status: 200 }
  );
} 