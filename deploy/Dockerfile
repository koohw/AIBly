FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 사용자 전환 (패키지 설치를 위해 root 사용)
USER root

# 필요한 패키지 설치
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY app/ .

# 임시 디렉토리 권한 설정 (Windows 환경 호환성)
RUN mkdir -p /tmp && chmod 777 /tmp

# 컨테이너 포트 설정
EXPOSE 8003

# 애플리케이션 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8003"] 