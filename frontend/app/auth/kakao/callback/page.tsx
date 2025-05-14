"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function KakaoCallback() {
  const router = useRouter();

  useEffect(() => {
    // 1) 이 안에서만 window를 읽도록 하고
    const code = new URLSearchParams(window.location.search).get("code");
    if (!code) {
      console.error("인가 코드가 없습니다.");
      return;
    }

    // 2) fetch도 바로 이 useEffect 내부에서 처리
    fetch(`https://k12e203.p.ssafy.io/api/oauth/kakao/callback?code=${code}`)
      .then((res) => res.json())
      .then((data) => {
        localStorage.setItem("auth_token", data.token);
        router.replace("/dashboard");
      })
      .catch((err) => {
        console.error("카카오 콜백 처리 중 에러:", err);
      });
  }, [router]);

  return <p>로그인 처리 중… 잠시만 기다려 주세요.</p>;
}
