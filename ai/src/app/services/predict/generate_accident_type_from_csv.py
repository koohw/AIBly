import pandas as pd
from ...config import Config
import logging

logger = logging.getLogger(__name__)

def generate_accident_type_from_csv(result_data):
    """
    VTN 모델이 예측한 사고 유형 코드를 이용해 CSV 파일에서 과실 비율 정보를 추출합니다.
    
    Args:
        result_data: 이전 단계에서 생성된 분석 결과 데이터
        
    Returns:
        dict: 사고 유형, 과실 비율 등의 정보
    """
    # 1. VTN 결과에서 예측된 사고 유형 코드 추출
    vtn_result = result_data.get("vtn_result", {})
    pred_code = vtn_result.get("accident_type")
    
    # damage_location은 inferred_meta에서 가져옴
    inferred_meta_data = result_data.get("inferred_meta", {})
    inferred_meta = inferred_meta_data.get("inferred_meta", {})
    damage_location = inferred_meta.get("damage_location", "")
    
    # 2. CSV 파일 로드
    csv_path = Config.ACCIDENT_DATA_CSV_PATH
    df = pd.read_csv(csv_path)
    
    # 3. 사고 유형 열에서 정확히 일치하는 행 검색
    try:
        float_pred_code = float(pred_code)
        matched = df[df["사고 유형"] == float_pred_code]
    except (ValueError, TypeError):
        # 숫자로 변환할 수 없는 경우
        logger.warning(f"사고 유형 코드 '{pred_code}'를 숫자로 변환할 수 없습니다.")
        matched = df[df["사고 유형"].astype(str) == str(pred_code)]
    
    # 4. 결과 구성
    if matched.empty:
        logger.warning(f"사고 유형 코드 '{pred_code}'에 해당하는 데이터가 CSV에 없습니다.")
        result = {
            "accident_type_code": pred_code,
            "fault_ratio_A": None,
            "fault_ratio_B": None,
            "description": None,
            "damage_location": damage_location
        }
    else:
        row = matched.iloc[0]
        try:
            result = {
                "accident_type_code": int(row["사고 유형"]),
                "fault_ratio_A": float(row["과실 비율 A"]),
                "fault_ratio_B": float(row["과실 비율 B"]),
                "description": row.get("차번호/사고유형"),
                "damage_location": damage_location
            }
        except (ValueError, KeyError) as e:
            logger.error(f"CSV 데이터 처리 중 오류 발생: {str(e)}")
            result = {
                "accident_type_code": pred_code,
                "damage_location": damage_location
            }
    
    return result