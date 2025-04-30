from fastapi import APIRouter
import pkg_resources
import sys
import platform

# 슬래시로 끝나는 prefix는 사용할 수 없음
# 빈 문자열이나 슬래시로 끝나지 않는 경로를 사용해야 함
router = APIRouter(prefix="")  # 또는 router = APIRouter()

@router.get("/test")
async def test_libraries():
    # 설치된 라이브러리 목록 가져오기
    installed_packages = sorted([f"{pkg.key}=={pkg.version}" 
                              for pkg in pkg_resources.working_set])
    
    # 시스템 정보
    system_info = {
        "python_version": sys.version,
        "platform": platform.platform(),
        "python_path": sys.executable
    }
    
    return {
        "status": "200 OK",
        "message": "라이브러리 테스트 완료",
        "installed_packages": installed_packages,
        "system_info": system_info
    }