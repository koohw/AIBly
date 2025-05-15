import torch
import os
import pickle
import json
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from ...config import Config


def infer_vtn(result_data):
    """
    VTN 모델을 사용하여 사고 유형을 추론합니다.
    """
    # VTN 입력 데이터 추출
    video_id = result_data.get("videoId")
    vtn_pkl_path = os.path.join(Config.VTN_PKL_PATH, f'{video_id}_vtn_input.pkl')
    # 모델 경로
    model_dir = os.path.join(Config.VTN_MODEL_PATH)
    
    # 1. 모델 및 토크나이저 로드
    model = AutoModelForSequenceClassification.from_pretrained(model_dir)
    tokenizer = AutoTokenizer.from_pretrained("bert-base-uncased")
    model.eval()
    
    # 2. 입력 로드
    with open(vtn_pkl_path, 'rb') as f:
        vtn_input = pickle.load(f)
    
    # 3. bbox_sequence + category_tensor를 텍스트 시퀀스로 변환
    # 예시: "0.1 0.3 0.5 0.7 | 0.2 0.4 0.6 0.8 ..." + " [SEP] 0 0 1"
    bbox_seq_str = " ".join([" ".join(map(str, b)) for b in vtn_input["bbox_sequence"]])
    cat_str = " ".join(map(str, vtn_input["category_tensor"]))
    input_text = bbox_seq_str + " [SEP] " + cat_str
    
    # 4. 토크나이저 인코딩
    inputs = tokenizer(
        input_text,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=512
    )
    
    # 5. 추론
    with torch.no_grad():
        outputs = model(**inputs)
        pred_class = torch.argmax(outputs.logits, dim=1).item()
    
    # 6. 결과 반환
    return {
        "accident_type": pred_class
    }
