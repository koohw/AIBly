import pandas as pd
from ...config import Config
import logging


def generate_accident_type_from_csv(result_data):
    """
    추론된 메타데이터의 키를 바탕으로 CSV 파일에서 해당하는 사고 유형을 찾아 반환합니다.
    """
    # 필요한 데이터 추출
    inferred_meta_data = result_data.get("inferred_meta", {})
    inferred_meta = inferred_meta_data.get("inferred_meta", {})
    damage_location = inferred_meta.get("damage_location", "")

    accident_type_data = result_data.get("vtn_result", {})
    accident_type_key = str(accident_type_data.get("accident_type", ""))
    # CSV 파일 경로
    csv_path = Config.ACCIDENT_DATA_CSV_PATH
    
    # CSV 불러오기
    df = pd.read_csv(csv_path)
    
    # 사고 유형 열에서 키가 포함된 행 찾기
    matched = df[df["사고 유형"].astype(str).str.contains(accident_type_key, na=False)]
    # 결과 추출
    if matched.empty:
        result = {
            "accident_type": "unknown",
            "damage_location": damage_location
        }
    else:
        accident_type = matched.iloc[0]["사고 유형"]
        result = {
            "accident_type": accident_type,
            "damage_location": damage_location
        }
    return result