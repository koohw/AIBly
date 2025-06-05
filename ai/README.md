## ⚙️ EC2 개발 환경 설정 (Python 3.12 + uv + hatchling)

Ubuntu EC2 인스턴스에서 이 프로젝트를 로컬 editable 모드로 설치하려면 아래 명령을 순서대로 실행하세요.

```bash
# 1. 저장소 클론 및 디렉토리 진입
git clone <YOUR_REPO_URL>
cd ai-server/ai

# 2. uv 설치 (이미 설치되어 있다면 생략 가능)
curl -Ls https://astral.sh/uv/install.sh | bash

# 3. 기존 가상환경 제거 (있을 경우)
rm -rf .venv

# 4. uv로 새로운 가상환경 생성 및 활성화
uv venv .venv
source .venv/bin/activate

# 5. hatchling 설치 (빌드 백엔드용)
uv pip install hatchling

# 6. 프로젝트 editable 모드로 설치
uv pip install -e .
```

## 🚀 CUDA 설치 및 GPU 지원

EC2 인스턴스에 CUDA를 설치하고 GPU 지원 PyTorch를 설정하려면:

```bash
# CUDA 툴킷 설치
sudo apt update
sudo apt install -y nvidia-cuda-toolkit

# GPU 지원 PyTorch 설치 (CUDA 11.8 기준)
uv pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118

# CUDA 설치 확인
nvcc --version
python -c "import torch; print('CUDA available:', torch.cuda.is_available()); print('CUDA version:', torch.version.cuda)"
```

## 🖥️ 서버 실행 방법

FastAPI 서버를 실행하려면:
deactivate
```bash
# 개발 모드로 실행 (자동 리로드)
cd ~/ai-server/ai
source .venv/bin/activate
python -m uvicorn src.app.main:app --host 0.0.0.0 --port 8000 --reload

# 또는 직접 main.py 실행
python src/app/main.py
```

## 🔄 서버 상시 기동 설정

systemd를 사용하여 서버를 상시 기동하려면:

1. 서비스 파일 생성:

```bash
sudo nano /etc/systemd/system/ai-server.service
```

2. 다음 내용 입력:
```nano
[Unit]
Description=AI Server FastAPI Application
After=network.target
[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/ai-server/ai
ExecStart=/home/ubuntu/ai-server/ai/.venv/bin/python -m uvicorn src.app.main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
Environment="PATH=/home/ubuntu/ai-server/ai/.venv/bin:/usr/local/bin:/usr/bin:/bin"
[Install]
WantedBy=multi-user.target
```


3. 서비스 활성화 및 시작:

```bash
sudo systemctl daemon-reload
sudo systemctl enable ai-server
sudo systemctl start ai-server
sudo systemctl status ai-server
```

4. 서비스 관리 명령어:

```bash
# 서비스 상태 확인
sudo systemctl status ai-server

# 서비스 재시작
sudo systemctl restart ai-server

# 서비스 중지
sudo systemctl stop ai-server

# 로그 확인
sudo journalctl -u ai-server -f
```

## 🔄 GitLab CI/CD로 모델 파일 자동 동기화

모델 파일이 업데이트될 때마다 자동으로 EC2 서버에 배포하도록 CI/CD가 설정되어 있습니다. `ai/src/app/resources/` 디렉토리의 변경사항은 자동으로 EC2 서버에 동기화됩니다.