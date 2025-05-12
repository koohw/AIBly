from fastapi import APIRouter
import pkg_resources
import sys
import platform
import logging
from typing import List, Dict
from pydantic import BaseModel

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 응답 모델 정의
class SystemInfo(BaseModel):
    python_version: str
    platform: str
    python_path: str

class TestResponse(BaseModel):
    status: str
    message: str
    installed_packages: List[str]
    system_info: SystemInfo

class GPUDevice(BaseModel):
    name: str
    memory: Dict[str, int]

class GPUInfo(BaseModel):
    available: bool
    count: int
    devices: List[GPUDevice]

class GPUResponse(BaseModel):
    status: str
    gpu_info: GPUInfo

router = APIRouter(
    prefix="",
    tags=["상태 확인"],
    responses={404: {"description": "Not found"}},
)

@router.get("/test", response_model=TestResponse, summary="시스템 상태 테스트")
async def test_libraries():
    """
    시스템 상태와 설치된 라이브러리 정보를 확인합니다.
    
    Returns:
        TestResponse: 시스템 정보와 설치된 라이브러리 목록을 포함한 응답
    """
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

@router.get("/gpu", response_model=GPUResponse, summary="GPU 상태 확인")
async def check_gpu():
    """
    GPU 사용 가능 여부와 관련 정보를 확인합니다.
    
    PyTorch를 사용하여 시스템에서 사용 가능한 GPU 장치 수와 메모리 정보를 제공합니다.
    
    Returns:
        GPUResponse: GPU 사용 가능 여부와 상세 정보를 포함한 응답
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