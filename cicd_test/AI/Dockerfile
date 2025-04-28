FROM debian:bullseye-slim
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \

        curl ca-certificates tar xz-utils \
    && rm -rf /var/lib/apt/lists/*

# ─── uv 설치 ─────────────────────────────────────
ENV UV_INSTALL_DIR=/usr/local/bin
RUN curl -LsSf https://astral.sh/uv/install.sh | sh
ENV PATH="/usr/local/bin:$PATH"
RUN uv --version
# ────────────────────────────────────────────────

WORKDIR /app
COPY pyproject.toml .

# Python 3.11 가상환경 + 의존성
RUN uv venv -p 3.11
ENV PATH="/app/.venv/bin:$PATH"
RUN uv pip compile pyproject.toml -o uv.lock
RUN uv pip sync uv.lock

COPY . .

# 컨테이너 내부 8000번 포트 공개
EXPOSE 8000

# Flask 앱 실행 (app.py 내부에 8000 지정돼 있다고 가정)
CMD ["python", "app.py"]
