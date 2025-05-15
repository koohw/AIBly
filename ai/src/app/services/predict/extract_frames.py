import cv2
import os
import shutil

def extract_frames(path: str, video_path: str, video_id: str) -> str:
    """비디오 파일을 프레임 단위로 분할하여 저장합니다."""
    # 프레임 저장할 디렉토리 경로 생성
    output_dir = os.path.join(path, video_id)
    
    # 폴더가 이미 존재하면 삭제하고 다시 생성
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)
    
    cap = cv2.VideoCapture(video_path)
    idx = 0
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        frame_path = os.path.join(output_dir, f"{idx:05d}.jpg")
        cv2.imwrite(frame_path, frame)
        idx += 1
    cap.release()
    
    return output_dir