from fastapi import HTTPException
import requests
import os
import cv2

def download_report(s3_url: str, path: str, file_name: str) -> None:
    # 1. 동영상 다운로드
    try:
        response = requests.get(s3_url)
        
        # 응답 코드 확인
        if response.status_code != 200:
            raise HTTPException(status_code=400, detail=f"URL에서 파일을 다운로드할 수 없습니다: {response.status_code}")
        
        file_path = os.path.join(path, file_name)
        
        # 파일 저장
        with open(file_path, 'wb') as f:
            f.write(response.content)
            
        # 2. OpenCV로 비디오 파일 확인
        try:
            cap = cv2.VideoCapture(file_path)
            if not cap.isOpened():
                os.remove(file_path)  # 잘못된 파일 삭제
                raise HTTPException(status_code=400, detail="다운로드한 파일이 유효한 동영상이 아닙니다")
            
            # 비디오 기본 정보 확인
            fps = cap.get(cv2.CAP_PROP_FPS)
            frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            
            # 최소한의 비디오 특성 검증 (예: 프레임 수가 0이면 유효하지 않음)
            if frame_count <= 0:
                os.remove(file_path)
                raise HTTPException(status_code=400, detail="빈 동영상 파일입니다")
                
            cap.release()
            
        except cv2.error as e:
            os.remove(file_path)  # 오류 발생 시 파일 삭제
            raise HTTPException(status_code=400, detail=f"OpenCV로 동영상을 열 수 없습니다: {str(e)}")
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"동영상 다운로드 및 검증 중 오류 발생: {str(e)}")