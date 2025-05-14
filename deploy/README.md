# Deploy Service

S3 버킷에서 파일을 다운로드하는 FastAPI 기반 서비스입니다. HTML 인터페이스를 통해 사용자 친화적인 환경을 제공합니다.

## 기능

- `/deploy` - 사용자 친화적인 인터페이스로 S3 버킷에서 develop.apk 파일을 다운로드합니다.
- `/deploy/test` - S3 버킷의 모든 파일 목록을 HTML 테이블로 확인하고 다운로드할 수 있습니다.
- `/deploy/download` - develop.apk 파일을 직접 다운로드합니다.
- `/deploy/test/download?file_name={파일명}` - S3 버킷에서 지정된 파일을 다운로드합니다.

## 개발 환경 설정

1. 필요한 패키지 설치:
```bash
pip install -r requirements.txt
```

2. 환경 변수 설정:
   - `.env` 파일 생성 (로컬 개발용):
   ```
   # AWS IAM Credentials
   AWS_ACCESS_KEY_ID=your_access_key_here
   AWS_SECRET_ACCESS_KEY=your_secret_key_here
   AWS_REGION=ap-northeast-2
   
   # S3 설정
   AWS_S3_BUCKET_NAME=ccrate-test
   S3_FOLDER_PATH=apks/
   ```
   - 또는 직접 환경 변수 설정:
   ```bash
   export AWS_ACCESS_KEY_ID=your_access_key_here
   export AWS_SECRET_ACCESS_KEY=your_secret_key_here
   export AWS_REGION=ap-northeast-2
   export AWS_S3_BUCKET_NAME=ccrate-test
   export S3_FOLDER_PATH=apks/
   ```

3. 서버 실행:
```bash
cd app
uvicorn main:app --host 0.0.0.0 --port 8003 --reload
```

## Docker로 실행하기

```bash
# 기본 빌드
docker build -t deploy-service .

# 실행 (환경 변수 주입)
docker run -p 8003:8003 \
  -e AWS_ACCESS_KEY_ID=your_access_key_here \
  -e AWS_SECRET_ACCESS_KEY=your_secret_key_here \
  -e AWS_REGION=ap-northeast-2 \
  -e AWS_S3_BUCKET_NAME=ccrate-test \
  -e S3_FOLDER_PATH=apks/ \
  deploy-service
```

## 디렉토리 구조

```
deploy/
│
├── app/
│   ├── main.py               # FastAPI 애플리케이션 코드
│   ├── templates/            # HTML 템플릿 파일
│   │   ├── base.html         # 기본 레이아웃
│   │   ├── index.html        # 메인 페이지
│   │   ├── deploy.html       # APK 다운로드 페이지
│   │   └── test.html         # 모든 파일 목록 페이지
│   └── static/               # 정적 파일
│       └── css/
│           └── style.css     # CSS 스타일
│
├── requirements.txt          # 필요한 패키지 목록
├── Dockerfile                # Docker 컨테이너 설정
├── .gitlab-deploy-ci.yml     # GitLab CI/CD 파이프라인 설정
└── README.md                 # 프로젝트 설명
```

## 참고 사항

- S3 버킷 내 파일은 `apks/` 폴더 안에 있어야 합니다.
- `develop.apk` 파일은 메인 다운로드 대상으로 설정되어 있습니다.
- 윈도우 환경에서 실행할 경우, 시스템 임시 디렉토리에 다운로드한 파일이 저장됩니다.

## API 문서

서버 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- Swagger UI: http://localhost:8003/docs
- ReDoc: http://localhost:8003/redoc 