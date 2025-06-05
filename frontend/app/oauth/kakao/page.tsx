"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Preferences } from "@capacitor/preferences";
import { registerFcmToken } from "@/lib/api/Fcm"; // FCM 토큰 등록 함수 import 추가
import { Capacitor } from "@capacitor/core";

// 콜백 페이지 수정
export default function KakaoOAuthCallbackPage() {
  const router = useRouter();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    console.log("🌐🌐🌐 웹 리다이렉트 로그인 방식 실행됨");
    console.log(
      "🔑 인가 코드 획득:",
      code ? `${code.substring(0, 10)}...` : "없음"
    );

    const processLogin = async () => {
      if (!code) {
        console.error("❌ 인가 코드가 없습니다.");
        return;
      }

      try {
        // 환경 변수에서 백엔드 URL 가져오기
        const backendUrl =
          process.env.NEXT_PUBLIC_BACKEND_URL ||
          "https://k12e203.p.ssafy.io/api";
        console.log("🔌 백엔드 URL:", backendUrl);

        // code-callback 엔드포인트 호출
        console.log("🔄 백엔드 인증 요청 시작...");
        const response = await fetch(
          `${backendUrl}/oauth/kakao/code-callback`,
          {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({ code }),
          }
        );

        if (!response.ok) {
          const errorText = await response.text();
          console.error(`❌ 백엔드 응답 오류: ${response.status}`, errorText);
          throw new Error(`백엔드 응답 오류: ${response.status}`);
        }

        const data = await response.json();
        console.log(
          "✅ 백엔드 응답 성공:",
          data ? "데이터 수신" : "데이터 없음"
        );

        if (data && data.authToken) {
          // Capacitor Preferences에 저장 (기존 코드)
          await Preferences.set({ key: "AUTH_TOKEN", value: data.authToken });
          console.log("💾 인증 토큰 저장 완료");

          // 네이티브에 특수 커스텀 이벤트 발생 - 기존 FcmTokenPlugin을 활용
          try {
            // 사용자 정의 이벤트를 사용하여 FcmTokenPlugin을 호출하고 authToken 전달
            if (Capacitor.isNativePlatform()) {
              console.log("🔄 네이티브 플랫폼에 토큰 정보 전달 시도");

              // FcmTokenPlugin 사용 (이미 등록된 플러그인)
              // @ts-ignore - Capacitor 플러그인 타입 오류 무시
              const fcmPlugin = Capacitor.Plugins.FcmToken;
              if (
                fcmPlugin &&
                typeof fcmPlugin.registerFcmToken === "function"
              ) {
                await fcmPlugin.registerFcmToken({ authToken: data.authToken });
                console.log("✅ FCM 토큰 등록 요청 완료");
              } else {
                console.log("⚠️ FcmToken 플러그인을 찾을 수 없음");
              }
            }
          } catch (e) {
            console.error("❌ 네이티브 인증 토큰 처리 중 오류:", e);
          }

          // FCM 토큰 등록 (딜레이 추가)
          try {
            console.log("⏳ FCM 등록 전 딜레이 시작 (2초)...");
            // 2초 딜레이 후 FCM 토큰 등록 시도
            setTimeout(async () => {
              try {
                console.log("🔄 FCM 토큰 등록 시작...");
                await registerFcmToken(data.authToken);
                console.log("✅ FCM 등록 요청 완료");
              } catch (fcmDelayedError) {
                console.error("❌ FCM 토큰 지연 등록 오류:", fcmDelayedError);
              }
            }, 2000);
          } catch (fcmError) {
            console.error("❌ FCM 토큰 등록 오류:", fcmError);
          }
        } else {
          console.error("❌ 응답에 인증 토큰이 없습니다:", data);
        }

        // 대시보드로 이동
        console.log("🔄 대시보드로 이동 중...");
        router.replace("/dashboard");
      } catch (error) {
        console.error("❌ 로그인 처리 중 오류:", error);
      }
    };

    processLogin();
  }, [router]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-gray-900 mb-4"></div>
      <p className="text-lg">로그인 처리 중… 잠시만 기다려 주세요.</p>
    </div>
  );
}
