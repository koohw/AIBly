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

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 포트 설정
EXPOSE 8002

# Next.js 프로덕션 서버 실행
ENV PORT=8002
ENV NODE_ENV=production
CMD ["npm", "run", "start"]