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

# serve 패키지 설치 및 curl 설치 (헬스체크용)
RUN npm install -g serve && apk add --no-cache curl

# 포트 설정
EXPOSE 8002

# 정적 파일 서빙 실행
ENV PORT=8002
ENV NODE_ENV=production
CMD ["serve", "-s", "out", "-p", "8002"]