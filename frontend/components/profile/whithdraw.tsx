import { Preferences } from '@capacitor/preferences'
import { CapacitorKakaoLogin } from '@team-lepisode/capacitor-kakao-login'
import { useRouter } from 'next/navigation'

export default function WithdrawalButton() {
  const router = useRouter()

  const handleWithdrawal = async () => {
    if (!confirm('정말 탈퇴하시겠습니까?')) return

    try {
        // 1) 카카오 unlink
        // 1) Preferences에서 토큰 꺼내기
        const { value: accessToken } = await Preferences.get({ key: "kakao_access_token" });

        if (accessToken) {
        // 2) REST API 직접 호출 예시 (플러그인 unlink 대신)
        try {
            await fetch("https://kapi.kakao.com/v1/user/unlink", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${accessToken}`,
            },
            });
            console.log("unlink via REST OK");
        } catch (e) {
            console.warn("unlink 실패:", e);
        }
        } else {
        console.log("저장된 토큰이 없어 unlink 생략");
        }

      // 2) 우리 서버에 회원 삭제 요청
      const { value: token } = await Preferences.get({ key: 'AUTH_TOKEN' })
      await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/user/delete`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      })
      // 3) 로컬 스토리지·Preferences 정리
      await Preferences.clear()
      // 4) 로그인 페이지로 돌려보내기
      router.replace('/loginlogin')
    } catch (e) {
      console.error('회원 탈퇴 실패', e)
      alert('탈퇴 중 오류가 발생했습니다.')
    }
  }

  return (
    <button
      onClick={handleWithdrawal}
      className="text-red-500 hover:underline"
    >
      회원 탈퇴
    </button>
  )
}
