// frontend/app/api/auth/kakao/route.ts

import { NextResponse } from "next/server";
import axios from "axios";

// POST /api/auth/kakao
export async function POST(request: Request) {
  // 1) 요청 바디에서 accessToken 꺼내기
  const { accessToken } = await request.json().catch(() => ({}));
  if (!accessToken) {
    return NextResponse.json(
      { ok: false, error: "accessToken이 필요합니다." },
      { status: 400 }
    );
  }

  try {
    // 2) Kakao 서버에 프로필 정보 요청
    const profileRes = await axios.get("https://kapi.kakao.com/v2/user/me", {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    const { id: kakaoId, kakao_account } = profileRes.data;
    const email = kakao_account?.email;
    const profileImage = kakao_account?.profile?.profile_image_url;

    // 3) (여기서) DB 조회/가입 로직을 추가하세요.
    //    예: const user = await db.user.upsert({ where: { kakaoId }, create: { kakaoId, email, profileImage }, update: {} });

    // 4) 자체 발급 토큰(JWT 등)
    //    실제로는 signJwt 같은 함수를 호출하세요.
    const token = "예시용_내부_JWT_토큰";

    // 5) 성공 응답
    return NextResponse.json({ ok: true, token }, { status: 200 });
  } catch (err: any) {
    console.error("Kakao 로그인 에러:", err.response?.data || err.message);
    return NextResponse.json(
      { ok: false, error: "서버 내부 에러" },
      { status: 500 }
    );
  }
}
