import torch
import os
import pickle
import json
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from ...config import Config
import logging

logger = logging.getLogger(__name__)

def infer_vtn(result_data):
    """
    VTN 모델을 사용하여 사고 유형을 추론합니다.
    
    Args:
        result_data: 이전 단계에서 생성된 분석 결과 데이터
        
    Returns:
        dict: 추론된 사고 유형 정보
    """
    # VTN 입력 데이터 추출
    video_id = result_data.get("videoId")
    vtn_pkl_path = os.path.join(Config.VTN_PKL_PATH, f'{video_id}_vtn_input.pkl')
    
    # 모델 및 라벨 맵 경로
    model_dir = Config.VTN_MODEL_PATH
    label_map_path = Config.LABEL_MAP_PATH
    
    # 1. 모델 및 토크나이저 로드
    model = AutoModelForSequenceClassification.from_pretrained(model_dir)
    tokenizer = AutoTokenizer.from_pretrained("bert-base-uncased")
    model.eval()
    
    # 2. label_map.json 로드
    if os.path.exists(label_map_path):
        with open(label_map_path, 'r', encoding='utf-8') as f:
            raw_map = json.load(f)
        label_map = {int(k): v for k, v in raw_map.items()}
        logger.info(f"Label map loaded: {label_map}")
    else:
        logger.warning(f"Label map not found at {label_map_path}, using indices directly")
        label_map = {}
    
    # 3. VTN 입력 로드
    with open(vtn_pkl_path, 'rb') as f:
        data = pickle.load(f)
    
    # 4. 시퀀스 → 텍스트
    bbox_seq_str = " ".join(" ".join(map(str, b)) for b in data['bbox_sequence'])
    cat_str = " ".join(map(str, data['category_tensor']))
    input_text = bbox_seq_str + " [SEP] " + cat_str
    
    # 5. 토크나이징
    inputs = tokenizer(
        input_text,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=512
    )
    
    # 6. 추론
    with torch.no_grad():
        outputs = model(**inputs)
        pred_index = torch.argmax(outputs.logits, dim=1).item()
    
    # 7. 실제 class로 매핑
    pred_class = label_map.get(pred_index, pred_index)
    
    logger.info(f"VTN 추론 완료: accident_type={pred_class}")
    
    # 8. 결과 반환
    return {
        "accident_type": pred_class
    }
