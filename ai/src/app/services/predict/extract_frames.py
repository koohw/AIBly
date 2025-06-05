import cv2
import os
import shutil
import subprocess
import logging

logger = logging.getLogger(__name__)

def extract_frames(path: str, video_path: str, video_id: str) -> str:
    """비디오 파일을 프레임 단위로 분할하여 저장합니다. 모든 프레임 추출."""
    # 프레임 저장할 디렉토리 경로 생성
    output_dir = os.path.join(path, video_id)
    
    # 폴더가 이미 존재하면 삭제하고 다시 생성
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)
    
    # 동영상 정보 확인 - 프레임 수 확인
    frames_cmd = [
        'ffprobe', 
        '-v', 'error', 
        '-count_frames',
        '-select_streams', 'v:0', 
        '-show_entries', 'stream=nb_read_frames', 
        '-of', 'csv=p=0', 
        video_path
    ]
    
    # 동영상 정보 확인 - FPS 확인
    fps_cmd = [
        'ffprobe', 
        '-v', 'error', 
        '-select_streams', 'v:0', 
        '-show_entries', 'stream=r_frame_rate', 
        '-of', 'csv=p=0', 
        video_path
    ]
    
    try:
        # 프레임 수 확인 (별도 명령어)
        frames_output = subprocess.check_output(frames_cmd, universal_newlines=True).strip()
        
        # FPS 확인 (별도 명령어)
        fps_output = subprocess.check_output(fps_cmd, universal_newlines=True).strip()
        
        # 정보 처리
        try:
            total_frames = int(frames_output)
            logger.info(f"원본 영상 총 프레임 수: {total_frames}")
        except (ValueError, TypeError):
            logger.warning(f"프레임 수 파싱 실패: '{frames_output}'")
            total_frames = "알 수 없음"
        
        try:
            if '/' in fps_output:
                num, den = map(int, fps_output.split('/'))
                fps = num / den
            else:
                fps = float(fps_output)
            logger.info(f"원본 영상 FPS: {fps}")
        except (ValueError, TypeError):
            logger.warning(f"FPS 파싱 실패: '{fps_output}'")
            fps = "알 수 없음"
            
    except Exception as e:
        logger.warning(f"동영상 정보 확인 중 오류: {str(e)}")
        total_frames = "알 수 없음"
        fps = "알 수 없음"
    
    try:
        # FFmpeg 명령어로 직접 프레임 추출 (하드웨어 가속 사용)
        ffmpeg_cmd = [
            'ffmpeg', 
            '-hwaccel', 'auto',
            '-i', video_path,
            '-vsync', '0',  # 프레임 동기화 비활성화하여 모든 프레임 추출
            '-q:v', '2',
            f'{output_dir}/%05d.jpg'  # FFmpeg은 이미 1부터 시작
        ]
        
        # FFmpeg 실행
        logger.info(f"FFmpeg 명령어 실행: {' '.join(ffmpeg_cmd)}")
        subprocess.run(ffmpeg_cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        
        # 추출된 프레임 수 확인
        extracted_frames = len([f for f in os.listdir(output_dir) if f.endswith('.jpg')])
        logger.info(f"FFmpeg로 추출 완료: 총 {extracted_frames}개 프레임 (원본: {total_frames}개)")
    
    except subprocess.CalledProcessError:
        logger.warning("FFmpeg 처리 실패, OpenCV로 대체합니다.")
        # FFmpeg 실패 시 OpenCV로 폴백
        cap = cv2.VideoCapture(video_path)
        
        # OpenCV로 프레임 정보 확인
        cv_total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        cv_fps = cap.get(cv2.CAP_PROP_FPS)
        logger.info(f"OpenCV로 확인한 영상 정보: 총 {cv_total_frames}개 프레임, FPS: {cv_fps}")
        
        # 모든 프레임 순차적으로 추출
        frame_idx = 1  # 인덱스는 1부터 시작 (FFmpeg와 동일)
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            frame_path = os.path.join(output_dir, f"{frame_idx:05d}.jpg")
            cv2.imwrite(frame_path, frame)
            frame_idx += 1
        
        cap.release()
        logger.info(f"OpenCV로 추출 완료: 총 {frame_idx-1}개 프레임")
    
    return output_dir