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

@router.get("/gpu")
async def check_gpu():
    """
    GPU 사용 가능 여부를 확인하는 엔드포인트
    """
    gpu_info = {
        "available": False,
        "count": 0,
        "devices": []
    }
    
    try:
        # PyTorch를 사용하여 GPU 확인
        import torch
        gpu_info["available"] = torch.cuda.is_available()
        if gpu_info["available"]:
            gpu_info["count"] = torch.cuda.device_count()
            gpu_info["devices"] = [
                {
                    "name": torch.cuda.get_device_name(i),
                    "memory": {
                        "total": torch.cuda.get_device_properties(i).total_memory,
                        "reserved": torch.cuda.memory_reserved(i),
                        "allocated": torch.cuda.memory_allocated(i)
                    }
                }
                for i in range(torch.cuda.device_count())
            ]
        logger.info(f"PyTorch GPU 확인: {gpu_info['available']}")
    except ImportError:
        logger.warning("PyTorch가 설치되어 있지 않습니다.")
            
    return {
        "status": "200 OK",
        "gpu_info": gpu_info
    }