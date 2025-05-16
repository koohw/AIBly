import cv2
import os
import shutil
import subprocess

def extract_frames(path: str, video_path: str, video_id: str) -> str:
    """비디오 파일을 프레임 단위로 분할하여 저장합니다. FFmpeg 직접 사용."""
    # 프레임 저장할 디렉토리 경로 생성
    output_dir = os.path.join(path, video_id)
    
    # 폴더가 이미 존재하면 삭제하고 다시 생성
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)
    
    try:
        # FFmpeg 명령어로 직접 프레임 추출 (하드웨어 가속 사용)
        ffmpeg_cmd = [
            'ffmpeg', 
            '-hwaccel', 'auto',
            '-i', video_path,
            '-vf', 'fps=20', # 초당 20프레임으로 제한
            '-q:v', '2',
            f'{output_dir}/%05d.jpg'
        ]
        
        # FFmpeg 실행 (출력 무시)
        subprocess.run(ffmpeg_cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    except subprocess.CalledProcessError:
        # FFmpeg 실패 시 OpenCV로 폴백
        print("FFmpeg 처리 실패, OpenCV로 대체합니다.")
        cap = cv2.VideoCapture(video_path, cv2.CAP_FFMPEG)
        
        # 원본 비디오의 FPS 확인
        original_fps = cap.get(cv2.CAP_PROP_FPS)
        
        # 원본 FPS가 20보다 높으면 프레임 건너뛰기 계산
        if original_fps > 20:
            frame_interval = original_fps / 20
        else:
            frame_interval = 1  # 원본이 20 FPS 이하면 모든 프레임 사용
            
        frame_count = 0
        idx = 0
        
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
                
            # frame_interval에 따라 특정 프레임만 저장
            if frame_count % frame_interval < 1:
                frame_path = os.path.join(output_dir, f"{idx:05d}.jpg")
                cv2.imwrite(frame_path, frame)
                idx += 1
                
            frame_count += 1
            
        cap.release()
    
    return output_dir