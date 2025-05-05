FROM node:20-alpine AS build

WORKDIR /app

# 의존성 파일 복사
COPY package.json package-lock.json ./

# 의존성 설치
RUN npm ci

# 소스 코드 복사
COPY . .

# 빌드 실행
RUN npm run build

# 서빙을 위한 간단한 서버 설치
RUN npm install -g serve

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 포트 설정 (Nginx 프록시 설정과 일치)
EXPOSE 8002

# 정적 파일 서빙 (포트 8002로)
CMD ["serve", "-s", "out", "-l", "8002"]