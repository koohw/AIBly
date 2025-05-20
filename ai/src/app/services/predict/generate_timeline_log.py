import os
import cv2
import glob

def iou(boxA, boxB):
    """
    두 바운딩 박스 간의 IoU(Intersection over Union)를 계산합니다.
    """
    xA = max(boxA[0], boxB[0])
    yA = max(boxA[1], boxB[1])
    xB = min(boxA[2], boxB[2])
    yB = min(boxA[3], boxB[3])
    interW = max(0, xB - xA)
    interH = max(0, yB - yA)
    inter = interW * interH
    areaA = (boxA[2] - boxA[0]) * (boxA[3] - boxA[1])
    areaB = (boxB[2] - boxB[0]) * (boxB[3] - boxB[1])
    return inter / float(areaA + areaB - inter + 1e-8)

def generate_timeline_log(yolo_results, video_id, direction=None, bg_dir=None, skip_frames=30, post_offset=10, iou_threshold=0.5):
    """
    배경 마스크 분석과 IoU 기반 역방향 추적을 통해 사고 시점을 추론하고 타임라인을 생성합니다.
    
    Args:
        yolo_results: YOLO 객체 탐지 결과
        video_id: 비디오 ID
        direction: B 차량의 진행 방향 정보 (선택적)
        bg_dir: 배경 마스크 이미지 디렉토리 경로
        skip_frames: 분석에서 제외할 초기 프레임 수
        post_offset: 사고 후 '이후 상황' 이벤트까지의 프레임 수
        iou_threshold: 역방향 추적에 사용할 IoU 임계값
    
    Returns:
        사고 프레임 및 타임라인 정보
    """
    # vehicle_B 클래스 번호
    VEHICLE_B_CLASS = 1
    
    # 1. 배경 마스크 분석을 통한 사고 프레임 찾기
    best_idx, best_count = None, -1
    
    if bg_dir:
        mask_paths = sorted(glob.glob(os.path.join(bg_dir, "*_new.png")))
        for p in mask_paths:
            idx = int(os.path.basename(p).split('_')[0])
            if idx < skip_frames:
                continue
            mask = cv2.imread(p, 0)
            count = int((mask > 0).sum())
            if count > best_count:
                best_count, best_idx = count, idx
    
    # 적합한 프레임을 찾지 못한 경우
    if best_idx is None:
        # 대체 로직 (정상 흐름 유지를 위해 오류 대신 최소 면적 방식 사용)
        min_area = float("inf")
        for item in yolo_results:
            frame = item["frame"]
            frame_idx = int(os.path.splitext(frame)[0])
            
            if frame_idx < skip_frames:
                continue
                
            for box in item.get("boxes", []):
                if box["class"] == VEHICLE_B_CLASS:
                    x1, y1, x2, y2 = box["bbox"]
                    w = x2 - x1
                    h = y2 - y1
                    area = w * h
                    
                    if area < min_area:
                        min_area = area
                        best_idx = frame_idx
    
    # 그래도 프레임을 찾지 못한 경우 (오류 방지)
    if best_idx is None:
        best_idx = skip_frames
    
    # 2. 사고 프레임에서의 B 차량 바운딩 박스 찾기
    accident_box = None
    for item in yolo_results:
        frame = item["frame"]
        idx = int(os.path.splitext(frame)[0])
        if idx == best_idx:
            for b in item.get('boxes', []):
                if b['class'] == VEHICLE_B_CLASS:
                    accident_box = b['bbox']
                    break
            break
    
    # 3. IoU 기반 역방향 추적으로 첫 등장 프레임 찾기
    first_seen = best_idx
    
    if accident_box:
        for item in reversed(yolo_results):
            frame = item["frame"]
            idx = int(os.path.splitext(frame)[0])
            if idx >= best_idx or idx < skip_frames:
                continue
            for b in item.get('boxes', []):
                if b['class'] == VEHICLE_B_CLASS and iou(accident_box, b['bbox']) >= iou_threshold:
                    first_seen = idx
                    break
            if idx < first_seen:
                break
    
    # 4. 사고 프레임 정보 생성
    accident = {
        "frame": f"{best_idx:05d}.jpg",
        "frame_idx": best_idx
    }
    
    # 5. 타임라인 이벤트 구성
    events = [{"event": "vehicle_B_first_seen", "frame_idx": first_seen}]
    events.append({"event": "accident_estimated", "frame_idx": best_idx})
    events.append({"event": "aftermath", "frame_idx": best_idx + post_offset})
    
    # 6. 타임라인 구성
    timeline = {
        "video": str(video_id),
        "accident_frame": accident["frame"],
        "accident_frame_idx": best_idx,
        "first_seen_idx": first_seen,
        "event_timeline": events
    }
    
    # 방향 정보가 제공된 경우 타임라인에 추가
    if direction:
        timeline["vehicle_B_direction"] = direction
        
    # 7. 결과 반환
    return {
        "accident_frame": accident,
        "timeline": timeline
    }