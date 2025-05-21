import os
import cv2
import glob
import json
from typing import Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)

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

def generate_timeline_log(yolo_results, video_id, direction=None, bg_dir=None, traffic_light_data=None, 
                         skip_frames=30, post_offset=10, iou_threshold=0.5, vehicle_class=1):
    """
    배경 마스크 분석과 IoU 기반 역방향 추적을 통해 사고 시점을 추론하고 타임라인을 생성합니다.
    
    Args:
        yolo_results: YOLO 객체 탐지 결과
        video_id: 비디오 ID
        direction: B 차량의 진행 방향 정보 (선택적)
        bg_dir: 배경 마스크 이미지 디렉토리 경로
        traffic_light_data: 신호등 감지 데이터 (선택적)
        skip_frames: 분석에서 제외할 초기 프레임 수
        post_offset: 사고 후 '이후 상황' 이벤트까지의 프레임 수
        iou_threshold: 역방향 추적에 사용할 IoU 임계값
        vehicle_class: 차량 B의 클래스 ID
    
    Returns:
        사고 프레임 및 타임라인 정보
    """
    # 모든 프레임 인덱스 추출 및 정렬
    all_idxs = sorted(int(os.path.splitext(x["frame"])[0]) for x in yolo_results)
    
    # 1. 배경 마스크 분석을 통한 사고 프레임 후보 찾기 (순위화)
    mask_counts = []
    
    if bg_dir:
        for p in glob.glob(os.path.join(bg_dir, "*_new.png")):
            idx = int(os.path.basename(p).split('_')[0])
            if idx < skip_frames:
                continue
            mask = cv2.imread(p, 0)
            count = int((mask > 0).sum())
            mask_counts.append((count, idx))
        
        # 마스크 카운트 기준 내림차순 정렬
        mask_counts.sort(reverse=True)
    
    # 마스크 데이터가 없는 경우
    if not mask_counts:
        # 최소한의 프레임 선택
        accident_idx = skip_frames
        accident_box = None
    else:
        accident_idx = None
        accident_box = None
        
        # 순위화된 후보 프레임을 순회하면서 차량 B 검색
        for _, cand in mask_counts:
            # 1) 전방 검색 (현재 프레임 이후)
            found = False
            for idx in all_idxs:
                if idx < cand:
                    continue
                    
                # 해당 프레임에서 차량 B 찾기
                for item in yolo_results:
                    frame_idx = int(os.path.splitext(item["frame"])[0])
                    if frame_idx == idx:
                        for b in item.get("boxes", []):
                            if b["class"] == vehicle_class:
                                accident_idx, accident_box = idx, b["bbox"]
                                found = True
                                break
                        break
                
                if found:
                    break
            
            # 2) 후방 검색 (필요한 경우)
            if not found:
                for idx in reversed(all_idxs):
                    if idx >= cand or idx < skip_frames:
                        continue
                        
                    # 해당 프레임에서 차량 B 찾기
                    for item in yolo_results:
                        frame_idx = int(os.path.splitext(item["frame"])[0])
                        if frame_idx == idx:
                            for b in item.get("boxes", []):
                                if b["class"] == vehicle_class:
                                    accident_idx, accident_box = idx, b["bbox"]
                                    found = True
                                    break
                            break
                    
                    if found:
                        break
            
            if found:
                break
        
        # 후보 프레임에서도 못 찾은 경우, 첫 번째 마스크 후보 사용
        if accident_idx is None and mask_counts:
            accident_idx = mask_counts[0][1]
            accident_box = None
    
    # 3. IoU 기반 역방향 추적으로 첫 등장 프레임 찾기
    first_seen = accident_idx
    
    if accident_box:
        for item in reversed(yolo_results):
            idx = int(os.path.splitext(item["frame"])[0])
            if idx >= accident_idx or idx < skip_frames:
                continue
            
            for b in item.get("boxes", []):
                if b["class"] == vehicle_class and iou(accident_box, b["bbox"]) >= iou_threshold:
                    first_seen = idx
            
            if idx < first_seen:
                break
    
    # 4. 사고 프레임 정보 생성
    accident = {
        "frame": f"{accident_idx:05d}.jpg",
        "frame_idx": accident_idx
    }
    
    # 5. 타임라인 이벤트 구성
    events = [{"event": "vehicle_B_first_seen", "frame_idx": first_seen}]
    
    # 신호등 감지 이벤트 추가 (제공된 경우)
    if traffic_light_data:
        events.append({
            "event": "traffic_light_detected",
            "frame_idx": max(skip_frames, accident_idx - post_offset)
        })
        
    events.append({"event": "accident_estimated", "frame_idx": accident_idx})
    events.append({"event": "aftermath", "frame_idx": accident_idx + post_offset})
    
    # 6. 타임라인 구성
    timeline = {
        "video": str(video_id),
        "accident_frame": accident["frame"],
        "accident_frame_idx": accident_idx,
        "first_seen_idx": first_seen,
        "event_timeline": events
    }
    
    # 방향 정보가 제공된 경우 타임라인에 추가
    if direction:
        timeline["vehicle_B_direction"] = direction
    logger.info(f"timeline: {timeline}")
    logger.info(f"accident: {accident}")
    # 7. 결과 반환
    return {
        "accident_frame": accident,
        "timeline": timeline
    }