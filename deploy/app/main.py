from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, JSONResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
import boto3
from botocore.exceptions import ClientError
import os
import logging
import tempfile
from dotenv import load_dotenv
from pathlib import Path

# 기본 디렉토리 설정
BASE_DIR = Path(__file__).resolve().parent
TEMPLATES_DIR = BASE_DIR / "templates"
STATIC_DIR = BASE_DIR / "static"
TEMP_DIR = Path(tempfile.gettempdir())  # 시스템 임시 디렉토리 사용

# 환경변수 파일이 있으면 로드, 없으면 OS 환경변수 사용
env_file = Path(BASE_DIR.parent, ".env")
if env_file.exists():
    load_dotenv(env_file)

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# root_path 추가 - NGINX에서 /deploy 경로로 매핑하기 위함
app = FastAPI(
    title="Deploy Service", 
    description="S3 파일 다운로드 서비스", 
    root_path="/deploy"
)

# 정적 파일 및 템플릿 설정
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
templates = Jinja2Templates(directory=str(TEMPLATES_DIR))

# S3 클라이언트 설정
def get_s3_client():
    return boto3.client(
        's3',
        aws_access_key_id=os.getenv('AWS_ACCESS_KEY_ID'),
        aws_secret_access_key=os.getenv('AWS_SECRET_ACCESS_KEY'),
        region_name=os.getenv('AWS_REGION', 'ap-northeast-2')
    )

def get_bucket_name():
    return os.getenv('AWS_S3_BUCKET_NAME', 'ccrate-test')

# S3 폴더 경로 설정 (환경 변수에서 가져오기)
def get_s3_folder_path():
    # 기본값으로 'apks/' 설정, 끝에 슬래시가 있는지 확인
    folder_path = os.getenv('S3_FOLDER_PATH', 'apks/')
    if not folder_path.endswith('/'):
        folder_path += '/'
    return folder_path

@app.get("/", response_class=HTMLResponse)
async def read_root(request: Request):
    """메인 페이지"""
    return templates.TemplateResponse(
        "index.html", 
        {"request": request, "title": "배포 서비스"}
    )

# APK 다운로드 페이지 경로 변경: /deploy -> /apk-download
@app.get("/apk-download", response_class=HTMLResponse)
async def deploy_page(request: Request):
    """develop.apk 다운로드 페이지"""
    return templates.TemplateResponse(
        "apk-download.html", 
        {"request": request, "title": "APK 다운로드", "file_name": "develop.apk"}
    )

# 다운로드 엔드포인트 경로 변경
@app.get("/apk-download/download")
async def download_develop_apk():
    """S3 버킷에서 develop.apk 파일을 다운로드합니다"""
    try:
        s3_client = get_s3_client()
        bucket_name = get_bucket_name()
        s3_folder_path = get_s3_folder_path()
        
        # 임시 파일 저장 경로 (시스템 임시 디렉토리 사용)
        download_path = TEMP_DIR / "develop.apk"
        
        # S3에서 파일 다운로드 (폴더 경로 추가)
        s3_key = f"{s3_folder_path}develop.apk"
        logger.info(f"다운로드 시도: {bucket_name}/{s3_key}")
        
        s3_client.download_file(
            bucket_name, 
            s3_key, 
            str(download_path)
        )
        
        return FileResponse(
            path=str(download_path),
            filename="develop.apk",
            media_type="application/vnd.android.package-archive"
        )
    except ClientError as e:
        logger.error(f"S3에서 파일을 다운로드하는 중 오류 발생: {e}")
        raise HTTPException(status_code=404, detail="파일을 찾을 수 없습니다")
    except Exception as e:
        logger.error(f"예상치 못한 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=f"서버 오류가 발생했습니다: {str(e)}")

# 파일 목록 페이지 경로 변경: /deploy/test -> /files
@app.get("/files", response_class=HTMLResponse)
async def test_page(request: Request):
    """S3 버킷의 모든 파일 목록 페이지"""
    try:
        s3_client = get_s3_client()
        bucket_name = get_bucket_name()
        s3_folder_path = get_s3_folder_path()
        
        # S3 폴더 내 파일 목록 조회
        response = s3_client.list_objects_v2(
            Bucket=bucket_name,
            Prefix=s3_folder_path
        )
        
        files = []
        
        if 'Contents' in response:
            for obj in response['Contents']:
                # 폴더 제외
                if obj['Key'].endswith('/'):
                    continue
                    
                # 파일 이름에서 폴더 경로 제거
                file_name = obj['Key'].replace(s3_folder_path, "")
                if not file_name:  # 빈 문자열이면 건너뛰기
                    continue
                    
                files.append({
                    "file_name": file_name,
                    "size": obj['Size'],
                    "last_modified": obj['LastModified'].isoformat(),
                    "download_url": f"/deploy/files/download?file_name={file_name}"
                })
        
        return templates.TemplateResponse(
            "files.html", 
            {
                "request": request, 
                "title": "모든 파일 목록", 
                "files": files
            }
        )
    except Exception as e:
        logger.error(f"S3 파일 목록을 가져오는 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=f"서버 오류가 발생했습니다: {str(e)}")

# 개별 파일 다운로드 경로 변경
@app.get("/files/download")
async def download_file(file_name: str):
    """지정된 파일을 S3 버킷에서 다운로드합니다"""
    try:
        s3_client = get_s3_client()
        bucket_name = get_bucket_name()
        s3_folder_path = get_s3_folder_path()
        
        # 임시 파일 저장 경로 (시스템 임시 디렉토리 사용)
        download_path = TEMP_DIR / file_name
        
        # S3에서 파일 다운로드 (폴더 경로 추가)
        s3_key = f"{s3_folder_path}{file_name}"
        logger.info(f"다운로드 시도: {bucket_name}/{s3_key}")
        
        s3_client.download_file(
            bucket_name, 
            s3_key, 
            str(download_path)
        )
        
        # APK 파일 미디어 타입 설정
        media_type = "application/vnd.android.package-archive"
        
        return FileResponse(
            path=str(download_path),
            filename=file_name,
            media_type=media_type
        )
    except ClientError as e:
        logger.error(f"S3에서 파일을 다운로드하는 중 오류 발생: {e}")
        raise HTTPException(status_code=404, detail="파일을 찾을 수 없습니다")
    except Exception as e:
        logger.error(f"예상치 못한 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=f"서버 오류가 발생했습니다: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    # OpenAPI 문서가 root_path를 인식하도록 설정
    uvicorn.run("main:app", host="0.0.0.0", port=8003, root_path="/deploy", reload=True) 
