import os
import torch
import numpy as np
from ...config import Config

class VehicleBLSTMClassifier(torch.nn.Module):
    def __init__(self, input_dim, hidden_dim, num_classes):
        super().__init__()
        self.lstm = torch.nn.LSTM(input_dim, hidden_dim, num_layers=2, batch_first=True, bidirectional=True)
        self.classifier = torch.nn.Linear(hidden_dim * 2, num_classes)  # bidirectional → *2

    def forward(self, x):
        lstm_out, _ = self.lstm(x)
        last_out = lstm_out[:, -1, :]  # 마지막 time step
        return self.classifier(last_out)

def extract_vehicleB_sequence(yolo_results):
    """YOLO 결과에서 vehicle_B의 궤적을 추출합니다."""
    VEHICLE_B_CLASS = 1
    trajectory = []
    
    sorted_frames = sorted(yolo_results, key=lambda x: int(os.path.splitext(x["frame"])[0]))
    
    for item in sorted_frames:
        frame_idx = int(os.path.splitext(item["frame"])[0])
        
        for box in item.get("boxes", []):
            if box["class"] == VEHICLE_B_CLASS:
                x1, y1, x2, y2 = box["bbox"]
                center_x = (x1 + x2) / 2
                center_y = (y1 + y2) / 2
                width = x2 - x1
                height = y2 - y1
                feature = [frame_idx, center_x, center_y, width, height, width * height]
                trajectory.append(feature)
                break
    
    return np.array(trajectory, dtype=np.float32)

def run_lstm_inference(yolo_results):
    """Vehicle B의 궤적을 기반으로 이동 방향을 LSTM 모델로 추론합니다."""
    # 모델 경로
    model_path = Config.LSTM_MODEL_PATH
    
    # Vehicle B 궤적 추출
    trajectory = extract_vehicleB_sequence(yolo_results)
    
    # 예측
    if len(trajectory) < 2:
        direction = "unknown"
    else:
        # 모델 로드 및 추론
        model = VehicleBLSTMClassifier(input_dim=6, hidden_dim=64, num_classes=3)
        model.load_state_dict(torch.load(model_path))
        model.eval()
        
        traj_tensor = torch.from_numpy(np.array([trajectory], dtype=np.float32))
        with torch.no_grad():
            pred = model(traj_tensor).argmax(1).item()
            direction = ["from_left", "center", "from_right"][pred]
    
    return {
        "direction": direction,
        "trajectory": trajectory.tolist() if len(trajectory) > 0 else []
    }