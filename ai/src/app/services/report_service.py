from fastapi import HTTPException
from .report import video_downloader, report_generator
from ..models.request_models import AnalysisRequest
from ..models.response_models import AnalysisResponse
import os
import time
from ..config import Config
from .predict import (
    extract_frames, 
    run_yolo_inference, 
    generate_timeline_log,
    run_lstm_inference,
    generate_traffic_light_events,
    generate_traffic_light_info,
    generate_inferred_meta,
    generate_vtn_input,
    infer_vtn,
    generate_accident_type_from_csv,
    generate_final_report
)
import logging
logger = logging.getLogger(__name__)

class ReportService:
    def __init__(self):
        pass

    def analyze_report(self, request: AnalysisRequest) -> AnalysisResponse:
        # 전체 분석 시작 시간
        total_start_time = time.time()
        
        # 0. 저장 경로 설정
        path = Config.USER_PATH + str(request.userId) + "/"
        file_name = str(request.videoId) + ".mp4"
        video_path = path + file_name
        os.makedirs(path, exist_ok=True)

        # 최종 결과를 저장할 딕셔너리
        result = {
            "userId": request.userId,
            "videoId": request.videoId,
            "fileName": request.fileName
        }

        # 1. 동영상 다운로드
        start_time = time.time()
        logger.info(f"동영상 다운로드 시작")
        try:
            video_downloader.download_report(request.presignedUrl, path, file_name)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"동영상 다운로드 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"동영상 다운로드 완료 (소요시간: {elapsed:.2f}초)")

        # 2. 동영상 분석
        logger.info(f"동영상 분석 시작")

        # 1. extract_frames
        start_time = time.time()
        logger.info(f"프레임 추출 시작")
        try:
            frames_dir = extract_frames.extract_frames(path, video_path, str(request.videoId))
            result["frames_dir"] = frames_dir
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"프레임 추출 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"프레임 추출 완료 (소요시간: {elapsed:.2f}초)")

        # 2. run_yolo_inference
        start_time = time.time()
        logger.info(f"YOLO 객체 탐지 시작")
        try:
            yolo_results = run_yolo_inference.run_yolo_inference(frames_dir)
            result["yolo_analysis"] = yolo_results
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"YOLO 객체 탐지 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"YOLO 객체 탐지 완료 (소요시간: {elapsed:.2f}초)")

        # 3. run_lstm_inference
        start_time = time.time()
        logger.info(f"LSTM 방향 추론 시작")
        try:
            lstm_data = run_lstm_inference.run_lstm_inference(yolo_results)
            result["lstm_analysis"] = lstm_data
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"LSTM 방향 추론 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"LSTM 방향 추론 완료 (소요시간: {elapsed:.2f}초)")

        # 4. generate_timeline_log
        start_time = time.time()
        logger.info(f"타임라인 생성 시작")
        try:
            timeline_data = generate_timeline_log.generate_timeline_log(yolo_results, request.videoId, result["lstm_analysis"]["direction"])
            result["timeline_analysis"] = timeline_data
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"타임라인 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"타임라인 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 5. generate_traffic_light_events
        start_time = time.time()
        logger.info(f"교통 신호등 이벤트 생성 시작")
        try:
            traffic_light_events = generate_traffic_light_events.generate_traffic_light_events(yolo_results)
            result["traffic_light_events"] = traffic_light_events
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"교통 신호등 이벤트 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"교통 신호등 이벤트 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 6. generate_traffic_light_info
        start_time = time.time()
        logger.info(f"교통 신호등 정보 생성 시작")
        try:
            # accident_frame_idx 정보 추가
            traffic_light_events_data = result["traffic_light_events"]
            traffic_light_events_data["accident_frame_idx"] = result["timeline_analysis"]["timeline"]["accident_frame_idx"]
            
            traffic_light_info = generate_traffic_light_info.generate_traffic_light_info(traffic_light_events_data)
            result["traffic_light_info"] = traffic_light_info
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"교통 신호등 정보 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"교통 신호등 정보 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 7. generate_inferred_meta
        start_time = time.time()
        logger.info(f"추론된 메타 정보 생성 시작")
        try:
            inferred_meta = generate_inferred_meta.generate_inferred_meta(result)
            result["inferred_meta"] = inferred_meta
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"추론된 메타 정보 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"추론된 메타 정보 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 8. generate_vtn_input
        start_time = time.time()
        logger.info(f"VTN 입력 생성 시작")
        try:
            vtn_input = generate_vtn_input.generate_vtn_input(result)
            result["vtn_input"] = vtn_input
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"VTN 입력 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"VTN 입력 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 9. infer_vtn
        start_time = time.time()
        logger.info(f"VTN 추론 시작")
        try:
            vtn_result = infer_vtn.infer_vtn(result)
            result["vtn_result"] = vtn_result
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"VTN 추론 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"VTN 추론 완료 (소요시간: {elapsed:.2f}초)")

        # 10 generate_accident_type_from_csv
        start_time = time.time()
        logger.info(f"CSV 기반 사고 유형 생성 시작")
        try:
            accident_type = generate_accident_type_from_csv.generate_accident_type_from_csv(result)
            result["accident_type"] = accident_type
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"CSV 기반 사고 유형 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"CSV 기반 사고 유형 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 11. generate_final_report
        start_time = time.time()
        logger.info(f"최종 보고서 생성 시작")
        try:
            final_report_data = generate_final_report.generate_final_report(result)
            result["final_report"] = final_report_data
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"최종 보고서 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"최종 보고서 생성 완료 (소요시간: {elapsed:.2f}초)")

        # 3. 양식에 맞게 결과 생성
        start_time = time.time()
        logger.info(f"결과 생성 시작")
        try:
            response = report_generator.generate_report(result)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"결과 생성 중 오류 발생: {str(e)}")
        elapsed = time.time() - start_time
        logger.info(f"결과 생성 완료 (소요시간: {elapsed:.2f}초)")
        
        # 전체 소요 시간 계산
        total_elapsed = time.time() - total_start_time
        logger.info(f"전체 분석 완료 (총 소요시간: {total_elapsed:.2f}초)")
        
        return response