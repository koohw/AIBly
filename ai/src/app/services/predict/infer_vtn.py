import torch
import os
import pickle
import json
import torch.nn as nn
from transformers import BertTokenizer, BertModel
from ...config import Config
import logging

logger = logging.getLogger(__name__)

# VTN 모델 클래스 정의
class VTNModel(nn.Module):
    def __init__(self, num_classes):
        super().__init__()
        self.bert = BertModel.from_pretrained("bert-base-uncased")
        self.classifier = nn.Linear(self.bert.config.hidden_size, num_classes)
        
    def forward(self, input_ids, attention_mask, labels=None):
        out = self.bert(input_ids=input_ids, attention_mask=attention_mask)
        logits = self.classifier(out.pooler_output)
        return (nn.CrossEntropyLoss()(logits, labels), logits) if labels is not None else logits

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
    
    # 1. label_map.json 로드
    if os.path.exists(label_map_path):
        with open(label_map_path, 'r', encoding='utf-8') as f:
            raw_map = json.load(f)
        label_map = {int(k): v for k, v in raw_map.items()}
        # 역매핑 생성 (클래스명 -> ID)
        label2id = {v: int(k) for k, v in raw_map.items()}
        logger.info(f"Label map loaded: {label_map}")
    else:
        logger.error(f"Label map not found at {label_map_path}")
        return {"accident_type": "unknown"}
    
    # 2. 모델 로드 (커스텀 VTNModel 사용)
    model = VTNModel(len(label_map))
    ckpt_path = os.path.join(model_dir, "pytorch_model.bin")
    
    if os.path.exists(ckpt_path):
        model.load_state_dict(torch.load(ckpt_path, map_location="cpu"))
        logger.info(f"Model loaded from {ckpt_path}")
    else:
        logger.error(f"Model checkpoint not found at {ckpt_path}")
        return {"accident_type": "unknown"}
        
    model.eval()
    
    # 3. 토크나이저 로드
    tokenizer = BertTokenizer.from_pretrained("bert-base-uncased")
    
    # 4. VTN 입력 로드
    with open(vtn_pkl_path, 'rb') as f:
        data = pickle.load(f)
    
    # 5. B차량 방향 카테고리 텐서에서 추출
    cat_tensor = data.get("category_tensor")
    b_idx = cat_tensor[3:6].tolist().index(1.0) if 1.0 in cat_tensor[3:6] else -1
    b_dir = ["from_left", "center", "from_right"][b_idx] if b_idx >= 0 else data.get("b_direction_text", "unknown")
    
    # 입력 텍스트 생성 (자연어 방식)
    b_text = f"B_direction is {b_dir}."
    
    bbox_str = " ".join(" ".join(map(str, b)) for b in data['bbox_sequence'])
    input_text = f"{b_text} [SEP] {bbox_str}"
    
    logger.info(f"B차량 방향: {b_dir}")
    logger.debug(f"Input text: {input_text[:200]}...")
    
    # 6. 토크나이징
    inputs = tokenizer(
        input_text,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=512
    )
    
    # 7. 추론
    with torch.no_grad():
        outputs = model(inputs['input_ids'], inputs['attention_mask'])
        logits = outputs[1] if isinstance(outputs, tuple) else outputs
        
        # 8. 방향에 따른 금지된 클래스 필터링
        forbidden = {
            "from_left":  [106, 111, 115],
            "from_right": [110, 107, 108, 109, 113, 114, 116]
        }
        
        if b_dir in forbidden:
            for forbidden_cls in forbidden[b_dir]:
                if forbidden_cls in label_map:
                    logits[0][label2id.get(forbidden_cls, -1)] = -1e9
                    logger.info(f"🚫 방향 {b_dir}에 맞지 않는 클래스 필터링: {forbidden_cls}")
        
        pred_index = logits.argmax(dim=1).item()
    
    # 9. 실제 class로 매핑
    pred_class = label_map.get(pred_index, pred_index)
    
    logger.info(f"VTN 추론 완료: accident_type={pred_class}")
    
    # 10. 결과 반환
    return {
        "accident_type": pred_class
    }
