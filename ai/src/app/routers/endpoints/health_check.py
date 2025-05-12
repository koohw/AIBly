from fastapi import APIRouter
import pkg_resources
import sys
import platform
import logging

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter(prefix="")

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