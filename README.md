# 이슈 자동 생성기 설명

create_issue.exe와 같은 위치에 env 파일을 위치시킨 후 실행합니다.
이슈 내용 및 작업내용들을 작성한 후 enter를 입력하면 5~10초 뒤 자동으로 이슈 내용이 생성됩니다.
확인후 적합하다면 y(Y)를 입력하면 이슈 및 브랜치가 자동으로 생성됩니다.
AI hub 
https://www.aihub.or.kr/aihubdata/data/view.do?currMenu=115&dataSetSn=597&topMenu=100&utm_source=chatgpt.com

*항상 위에 실행할 것
import os
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "2" 
--------------------
* 다운로드 코드
./aihubshell -mode d -datasetkey 597 -filekey 509344 -aihubapikey '1C24F55B-D5C2-41D2-B59F-F66122379825'
----------------------
*팀 프로세스 조회하기
$ps -ef|grep k12e203
*서버 리소스 확인
$htop 실행
--------------------------
위치	파일명
1.Training/원천데이터/	
TS_차대차_이미지_T자형교차로.zip
1.Training/라벨링데이터/	
TL_차대차_이미지_T자형교차로.zip
2.Validation/원천데이터_231108_add/	
VS_차대차_이미지_T자형교차로.zip
2.Validation/라벨링데이터_231108_add/	
VL_차대차_이미지_T자형교차로.zip
--------------------------
다운로드 후 압축풀기
import zipfile
import os
import shutil

# 압축 파일 경로
train_image_zip = '/home/j-k12e203/traffic_data/095.교통사고_영상_데이터/01.데이터/1.Training/원천데이터/TS_차대차_이미지_T자형교차로.zip'
train_label_zip = '/home/j-k12e203/traffic_data/095.교통사고_영상_데이터/01.데이터/1.Training/라벨링데이터/TL_차대차_이미지_T자형교차로.zip'
val_image_zip = '/home/j-k12e203/traffic_data/095.교통사고_영상_데이터/01.데이터/2.Validation/원천데이터_231108_add/VS_차대차_이미지_T자형교차로.zip'
val_label_zip = '/home/j-k12e203/traffic_data/095.교통사고_영상_데이터/01.데이터/2.Validation/라벨링데이터_231108_add/VL_차대차_이미지_T자형교차로.zip'

# 임시 압축 해제 폴더
extracted_dir = '/home/j-k12e203/traffic_data/extracted'
os.makedirs(extracted_dir, exist_ok=True)

# 최종 저장 폴더
final_dirs = {
    'train_images': '/home/j-k12e203/traffic_data/train/images/t_junction',
    'train_labels': '/home/j-k12e203/traffic_data/train/labels/t_junction',
    'val_images': '/home/j-k12e203/traffic_data/val/images/t_junction',
    'val_labels': '/home/j-k12e203/traffic_data/val/labels/t_junction',
}

for path in final_dirs.values():
    os.makedirs(path, exist_ok=True)

# 압축 해제 함수
def unzip_and_move(zip_path, dest_folder):
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(dest_folder)

# 압축 해제
unzip_and_move(train_image_zip, f'{extracted_dir}/train_images_tj')
unzip_and_move(train_label_zip, f'{extracted_dir}/train_labels_tj')
unzip_and_move(val_image_zip, f'{extracted_dir}/val_images_tj')
unzip_and_move(val_label_zip, f'{extracted_dir}/val_labels_tj')

print("✅ 압축 해제 완료")

# 파일 이동
def move_files(src_dir, dst_dir, extension):
    for root, _, files in os.walk(src_dir):
        for file in files:
            if file.endswith(extension):
                src_path = os.path.join(root, file)
                dst_path = os.path.join(dst_dir, file)
                
                # 이미 같은 이름 파일이 있으면 삭제
                if os.path.exists(dst_path):
                    os.remove(dst_path)
                
                shutil.move(src_path, dst_dir)

# 파일 이동 실행
move_files(f'{extracted_dir}/train_images_tj', final_dirs['train_images'], '.png')
move_files(f'{extracted_dir}/train_labels_tj', final_dirs['train_labels'], '.json')
move_files(f'{extracted_dir}/val_images_tj', final_dirs['val_images'], '.png')
move_files(f'{extracted_dir}/val_labels_tj', final_dirs['val_labels'], '.json')

print("✅ 파일 이동 완료!")

# (선택) 임시 폴더 정리
shutil.rmtree(extracted_dir)
print("✅ 임시 폴더 삭제 완료!")
---------------------
최종 폴더 점검 코드
import os

# 폴더 경로
paths = {
    'train_images_tj': '/home/j-k12e203/traffic_data/train/images/t_junction',
    'train_labels_tj': '/home/j-k12e203/traffic_data/train/labels/t_junction',
    'val_images_tj': '/home/j-k12e203/traffic_data/val/images/t_junction',
    'val_labels_tj': '/home/j-k12e203/traffic_data/val/labels/t_junction',
}

# 각 폴더 파일 개수 확인
for name, path in paths.items():
    if os.path.exists(path):
        files = os.listdir(path)
        num_files = len(files)
        print(f"✅ {name} 폴더에 {num_files}개 파일 존재")
    else:
        print(f"❌ {name} 폴더가 존재하지 않음 (오류!)")
---------------------------
COCO 포맷 변환 코드
import os
import json
import glob

# 경로 설정
train_label_dir = '/home/j-k12e203/traffic_data/train/labels/t_junction'
val_label_dir = '/home/j-k12e203/traffic_data/val/labels/t_junction'

output_train_coco = '/home/j-k12e203/traffic_data/train_coco.json'
output_val_coco = '/home/j-k12e203/traffic_data/val_coco.json'

# 클래스 매핑
category_mapping = {
    'vehicle': 1,
    'pedestrian': 2,
    'traffic-light-red': 3,
    'traffic-light-green': 4,
    'traffic-light-yellow': 5,
    'traffic-sign': 6,
}

categories = [
    {"id": 1, "name": "vehicle"},
    {"id": 2, "name": "pedestrian"},
    {"id": 3, "name": "traffic-light-red"},
    {"id": 4, "name": "traffic-light-green"},
    {"id": 5, "name": "traffic-light-yellow"},
    {"id": 6, "name": "traffic-sign"},
]

def convert_to_coco(label_dir, output_json):
    images = []
    annotations = []
    annotation_id = 0
    image_id = 0

    label_files = sorted(glob.glob(os.path.join(label_dir, '*.json')))

    for label_file in label_files:
        with open(label_file, 'r', encoding='utf-8') as f:
            label_data = json.load(f)

        file_name = label_data['file_name']
        width = label_data['width']
        height = label_data['height']

        images.append({
            "id": image_id,
            "file_name": file_name,
            "width": width,
            "height": height,
        })

        for obj in label_data.get('objects', []):
            category = obj['category']
            if category not in category_mapping:
                continue

            bbox = obj['bbox']
            x, y, w, h = bbox

            annotations.append({
                "id": annotation_id,
                "image_id": image_id,
                "category_id": category_mapping[category],
                "bbox": [x, y, w, h],
                "area": w * h,
                "iscrowd": 0,
            })
            annotation_id += 1

        image_id += 1

    coco_format = {
        "info": {},
        "licenses": [],
        "images": images,
        "annotations": annotations,
        "categories": categories,
    }

    with open(output_json, 'w', encoding='utf-8') as f:
        json.dump(coco_format, f, ensure_ascii=False, indent=4)
    print(f"✅ COCO 파일 생성 완료: {output_json}")

# 변환 실행
convert_to_coco(train_label_dir, output_train_coco)
convert_to_coco(val_label_dir, output_val_coco)
---------------------------
COCO 포맷 확인 코드
import json

# 경로 설정
train_coco_path = '/home/j-k12e203/traffic_data/train_coco.json'
val_coco_path = '/home/j-k12e203/traffic_data/val_coco.json'

def check_coco_format(coco_json_path):
    with open(coco_json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    print(f"✅ {coco_json_path} 확인 결과:")
    print(f"  📦 images 수: {len(data.get('images', []))}")
    print(f"  🏷️ annotations 수: {len(data.get('annotations', []))}")
    print(f"  🏷️ categories 수: {len(data.get('categories', []))}")
    print()

# 체크 실행
check_coco_format(train_coco_path)
check_coco_format(val_coco_path)
----------------------------------
COCO => Yolo 포맷 변환
import os
import json
from tqdm import tqdm

# 경로 설정
train_coco_path = '/home/j-k12e203/traffic_data/train_coco.json'
val_coco_path = '/home/j-k12e203/traffic_data/val_coco.json'

train_output_dir = '/home/j-k12e203/traffic_data/yolo_labels/train/'
val_output_dir = '/home/j-k12e203/traffic_data/yolo_labels/val/'

# 디렉토리 생성
os.makedirs(train_output_dir, exist_ok=True)
os.makedirs(val_output_dir, exist_ok=True)

# 변환 함수
def convert_coco_to_yolo(coco_path, output_dir):
    with open(coco_path, 'r') as f:
        coco = json.load(f)

    # 이미지 ID -> 파일 이름 매핑
    id_to_filename = {img['id']: img['file_name'] for img in coco['images']}

    # 출력용 딕셔너리 초기화
    yolo_labels = {filename: [] for filename in id_to_filename.values()}

    # 어노테이션 변환
    for ann in tqdm(coco['annotations'], desc=f"Processing {os.path.basename(coco_path)}"):
        image_id = ann['image_id']
        bbox = ann['bbox']
        category_id = ann['category_id']

        file_name = id_to_filename[image_id]

        # bbox 변환 (x_center, y_center, width, height), 값은 이미지 크기에 맞춰서 0~1로 정규화
        img_width = 1920
        img_height = 1080

        x_min, y_min, width, height = bbox
        x_center = (x_min + width / 2) / img_width
        y_center = (y_min + height / 2) / img_height
        width /= img_width
        height /= img_height

        # YOLO 포맷: category_id x_center y_center width height
        yolo_labels[file_name].append(f"{category_id} {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}")

    # 저장
    for file_name, labels in yolo_labels.items():
        txt_file = os.path.splitext(file_name)[0] + '.txt'
        txt_path = os.path.join(output_dir, txt_file)
        with open(txt_path, 'w') as f:
            f.write('\n'.join(labels))

# 변환 실행
convert_coco_to_yolo(train_coco_path, train_output_dir)
convert_coco_to_yolo(val_coco_path, val_output_dir)

print("✅ 변환 완료! YOLO 포맷 라벨이 생성되었습니다.")
print(f"Train labels 위치: {train_output_dir}")
print(f"Val labels 위치: {val_output_dir}")
------------------
yolo 변환 파일 구조
/home/j-k12e203/traffic_data/yolo_labels/
    ├── train/
    │   ├── 001_147_001.txt
    │   ├── 001_147_003.txt
    │   └── ...
    └── val/
        ├── 001_147_002.txt
        ├── 001_147_010.txt
        └── ...
-----------------------------
data.yaml 코드
nano /home/j-k12e203/traffic_data/data.yaml

path: /home/j-k12e203/traffic_data

train: train/images/t_junction
val: val/images/t_junction

nc: 6

names:
  0: vehicle
  1: pedestrian
  2: traffic-light-red
  3: traffic-light-green
  4: traffic-light-yellow
  5: traffic-sign
--------------------------
파일 이동 코드
# YOLO txt 라벨 복사
find /home/j-k12e203/traffic_data/yolo_labels/train/ -name '*.txt' -exec cp {} /home/j-k12e203/traffic_data/train/labels/t_junction/ \;
cp /home/j-k12e203/traffic_data/yolo_labels/val/*.txt /home/j-k12e203/traffic_data/val/labels/t_junction/
--------------------------
YOLO 실행코드
yolo detect train data=/home/j-k12e203/traffic_data/data.yaml model=yolov8n.pt epochs=10 imgsz=640 device=2 batch=32
---------------------------
YOLO 추론 결과 json 저장
from ultralytics import YOLO
import os
import json
from tqdm import tqdm

# 1. 모델 불러오기 (경로 수정)
model = YOLO('/home/j-k12e203/traffic_data/runs/detect/train7/weights/best.pt')

# 2. source 폴더 설정 (val 또는 test)
source_dir = '/home/j-k12e203/traffic_data/val/images/t_junction'  # 예시
output_json_path = './yolo_detection_results.json'

# 3. 결과 저장용 리스트
all_results = []

# 4. source_dir 안의 모든 이미지 추론
image_files = [f for f in os.listdir(source_dir) if f.endswith(('.png', '.jpg', '.jpeg'))]

for img_file in tqdm(image_files, desc="Running YOLO inference"):
    img_path = os.path.join(source_dir, img_file)
    
    # 1장 이미지 추론
    results = model.predict(img_path, save=False, conf=0.3, verbose=False)[0]
    
    # 추론 결과를 리스트에 저장
    detections = []
    for box in results.boxes:
        bbox = box.xyxy[0].cpu().numpy().tolist()  # [x1, y1, x2, y2]
        score = box.conf[0].cpu().item()
        cls = int(box.cls[0].cpu().item())
        
        detections.append({
            'bbox': bbox,
            'score': score,
            'class': cls,
        })
    
    all_results.append({
        'image_file': img_file,
        'detections': detections
    })

# 5. 결과를 JSON으로 저장
with open(output_json_path, 'w') as f:
    json.dump(all_results, f, indent=2)

print(f"YOLO Detection 결과 저장 완료: {output_json_path}")

-------------------------------
yolo json 형식
[
  {
    "image_file": "example1.png",
    "detections": [
      {"bbox": [x1, y1, x2, y2], "score": 0.95, "class": 0},
      {"bbox": [x1, y1, x2, y2], "score": 0.88, "class": 2}
    ]
  },
  {
    "image_file": "example2.png",
    "detections": []
  }
]
--------------------------
tracklet 생성

import json
import os
from tqdm import tqdm

# 1. 파일 경로 설정
tracklets_path = './tracklets.json'
meta_folder = '/home/j-k12e203/traffic_data/val/labels/t_junction'  # (또는 train으로도 가능)
output_path = './tracklets_with_video_info.json'

# 2. tracklets.json 읽기
with open(tracklets_path, 'r') as f:
    tracklets = json.load(f)

# 3. 메타데이터 읽기 (모든 파일 읽기)
file2videoinfo = {}

json_files = [f for f in os.listdir(meta_folder) if f.endswith('.json')]

for json_file in tqdm(json_files, desc="Loading meta files"):
    json_path = os.path.join(meta_folder, json_file)
    with open(json_path, 'r') as f:
        meta = json.load(f)
        file_name = meta['file_name']
        video_file_name = meta['video_file_name']
        sequence_frame_number = meta['sequence_frame_number']
        file2videoinfo[file_name] = {
            'video_file_name': video_file_name,
            'sequence_frame_number': sequence_frame_number
        }

# 4. tracklets에 video info 추가
new_tracklets = []

for tr in tqdm(tracklets, desc="Matching video info"):
    file_name = tr['image_file']
    if file_name in file2videoinfo:
        tr['video_file_name'] = file2videoinfo[file_name]['video_file_name']
        tr['sequence_frame_number'] = file2videoinfo[file_name]['sequence_frame_number']
    else:
        tr['video_file_name'] = 'unknown'
        tr['sequence_frame_number'] = -1
    new_tracklets.append(tr)

# 5. 저장
with open(output_path, 'w') as f:
    json.dump(new_tracklets, f, indent=2)

print(f"✅ Tracklets에 video info 추가 완료: {output_path}")

------------------------
Tracklet json 형식
[
  {
    "frame_idx": 0,
    "image_file": "00001.png",
    "bbox": [100.0, 120.0, 200.0, 250.0],
    "score": 0.95,
    "class": 0,
    "track_id": 5
  },
  {
    "frame_idx": 0,
    "image_file": "00001.png",
    "bbox": [50.0, 80.0, 120.0, 150.0],
    "score": 0.91,
    "class": 2,
    "track_id": 7
  },
  ...
]
------------------------------
VTN 시퀀스 생성 코드 16프레임 기준
import json
import os
from collections import defaultdict
from tqdm import tqdm

# 파일 경로
tracklets_path = './tracklets_with_video_info.json'
output_dir = './vtn_sequences'
os.makedirs(output_dir, exist_ok=True)

# 읽기
with open(tracklets_path, 'r') as f:
    tracklets = json.load(f)

# 비디오별로 묶기
video_dict = defaultdict(list)
for t in tracklets:
    video_dict[t['video_file_name']].append(t)

# 비디오별로 처리
sequence_length = 16
seq_id = 0

for video_name, tracks in tqdm(video_dict.items(), desc="Building sequences"):
    # sequence_frame_number 기준 정렬
    tracks = sorted(tracks, key=lambda x: x['sequence_frame_number'])

    # 모든 frames 모으기
    frame_dict = defaultdict(list)
    for tr in tracks:
        frame_idx = tr['sequence_frame_number']
        frame_dict[frame_idx].append({
            'track_id': tr['track_id'],
            'bbox': tr['bbox'],
            'class': tr['class'],
            'score': tr['score'],
        })

    frame_indices = sorted(frame_dict.keys())
    
    # 슬라이딩 윈도우로 시퀀스 만들기
    for i in range(len(frame_indices) - sequence_length + 1):
        selected_frames = frame_indices[i:i+sequence_length]
        
        sequence = {
            'video_name': video_name,
            'start_frame': selected_frames[0],
            'frames': []
        }

        valid = True
        for fi in selected_frames:
            if fi not in frame_dict:
                valid = False
                break
            sequence['frames'].append({
                'frame_idx': fi,
                'objects': frame_dict[fi]
            })

        if valid:
            # 파일 하나씩 저장
            seq_path = os.path.join(output_dir, f'seq_{seq_id:06d}.json')
            with open(seq_path, 'w') as f:
                json.dump(sequence, f, indent=2)
            seq_id += 1

print(f"✅ VTN용 시퀀스 생성 완료: {output_dir} 안에 {seq_id}개 시퀀스 저장됨")
---------------------------
시퀀스 json 형식
{
  "video_name": "bb_1_160505_pedestrian_117_091",
  "start_frame": 0,
  "frames": [
    { "frame_idx": 0, "objects": [{ "track_id": 3, "bbox": [...], "class": 0, "score": 0.9 }, {...}] },
    { "frame_idx": 1, "objects": [{ "track_id": 3, "bbox": [...], "class": 0, "score": 0.91 }, {...}] },
    ...
    { "frame_idx": 15, "objects": [...] }
  ]
}
--------------------------
accident_dataset.py 코드 
파일 위치 : vtn_accident_project/dataset
import os
import json
from torch.utils.data import Dataset

class AccidentDataset(Dataset):
    def __init__(self, sequence_dir, label_json_path, sequence_length=16, transform=None):
        self.sequence_dir = sequence_dir
        self.transform = transform
        self.sequence_length = sequence_length
        
        self.sequence_files = sorted([
            os.path.join(sequence_dir, f) for f in os.listdir(sequence_dir) if f.endswith('.json')
        ])
        
        with open(label_json_path, 'r') as f:
            label_data = json.load(f)
        
        self.video_to_label = {}
        for item in label_data:
            video_name = item['video']['video_name']
            accident_type = item['video']['traffic_accident_type']
            self.video_to_label[video_name] = accident_type

    def __len__(self):
        return len(self.sequence_files)

    def __getitem__(self, idx):
        seq_path = self.sequence_files[idx]
        with open(seq_path, 'r') as f:
            seq_data = json.load(f)

        video_name = seq_data['video_name']
        label = self.video_to_label.get(video_name, -1)

        frames = seq_data['frames']
        assert len(frames) == self.sequence_length, f"Expected {self.sequence_length} frames but got {len(frames)}"

        all_objects = []
        for frame in frames:
            objs = frame['objects']
            all_objects.append(objs)
        
        sample = {
            'objects': all_objects,
            'label': label,
            'video_name': video_name
        }
        
        if self.transform:
            sample = self.transform(sample)
        
        return sample
--------------------------------------
vtn_accident.py 코드
파일 위치 vtn_accident_project/model/vtn_accident.py
import torch
import torch.nn as nn

class SimpleVTN(nn.Module):
    def __init__(self, num_classes=132, sequence_length=16, hidden_dim=512):
        super(SimpleVTN, self).__init__()
        
        self.sequence_length = sequence_length
        self.hidden_dim = hidden_dim
        
        # (1) input: bounding box sparse feature를 Linear Embedding
        self.input_embed = nn.Linear(4, hidden_dim)  # bbox (x1,y1,x2,y2) 4개
        
        # (2) Transformer Encoder (VTN Backbone)
        encoder_layer = nn.TransformerEncoderLayer(d_model=hidden_dim, nhead=8)
        self.transformer_encoder = nn.TransformerEncoder(encoder_layer, num_layers=6)

        # (3) Classification Head
        self.classifier = nn.Sequential(
            nn.LayerNorm(hidden_dim),
            nn.Linear(hidden_dim, num_classes)
        )
    
    def forward(self, batch_objects):
        """
        batch_objects: [batch_size, sequence_length, num_objects, 4]
        """
        batch_size, seq_len, num_objects, _ = batch_objects.shape
        
        # object dimension을 합치자
        x = batch_objects.view(batch_size, seq_len * num_objects, 4)
        
        # (1) bbox를 hidden_dim으로 embedding
        x = self.input_embed(x)  # [batch_size, seq_len*num_objects, hidden_dim]
        
        # (2) Transformer Encoder
        x = x.permute(1, 0, 2)  # [seq_len*num_objects, batch_size, hidden_dim]
        x = self.transformer_encoder(x)  # [seq_len*num_objects, batch_size, hidden_dim]
        x = x.permute(1, 0, 2)  # [batch_size, seq_len*num_objects, hidden_dim]

        # (3) 평균 pooling (전체 objects over all frames)
        x = x.mean(dim=1)  # [batch_size, hidden_dim]
        
        # (4) Classification
        out = self.classifier(x)  # [batch_size, num_classes]
        return out
------------------------
## 📥 입력 데이터 구성

### 1. 📦 bbox 시퀀스
| 변수 | 설명 | 형태 |
|------|------|------|
| `bbox` | 프레임별 차량/신호등 위치 정보 | `[T, N, 4]` (x, y, w, h) |
| 전처리 | A/B 차량 + 신호등 등 **사고 관련 객체만 필터링** 사용 |

### 2. 🧾 사고 메타 정보 (정수형 → `nn.Embedding`)
| 변수명 | 설명 |
|--------|------|
| `accident_place_feature` | 사고 장소 형태 |
| `vehicle_a_progress_info` | 차량 A 진행 방향 |
| `vehicle_b_progress_info` | 차량 B 진행 방향 |
| `video_point_of_view` | 블박 시점 여부 |
| `damage_location` | 피해 차량 부위 (1~8 방향) |

---

## 🎯 출력 (Target)

| 변수 | 설명 |
|------|------|
| `traffic_accident_type` | 사고 유형 클래스 번호 (현재 129개: 0~128) |

> T자형 교차로 데이터 기반으로 106~128 일부 클래스만 먼저 학습
## 🏋️ 학습 결과

- ✅ `train.pkl`: 1080건, `val.pkl`: 163건 (T자형 교차로 기준)
- ✅ Loss 감소: **3.09 → 0.14**
- ✅ Validation Accuracy: **98.16%**
- ✅ 단일 추론 예측 성공: 예측=정답 (예: 110 → 110)
- ✅ confusion matrix에서 대부분 대각선 정렬 (고정밀 모델)

---
# 🚗 VTN 기반 교통사고 분석 자동화 파이프라인

> 블랙박스 영상을 입력하면 YOLO + VTN 기반으로 사고 메타데이터와 타임라인 로그를 자동으로 생성하는 프로젝트입니다.

---

## ✅ 목표

- mp4 영상 → 프레임 추출
- YOLOv8 객체 탐지 → `.txt` 출력
- VTN 모델 예측 → 사고 유형 및 메타 정보 추론
- 타임라인 로그 생성 → 충돌, 등장, 이탈 자동 감지

---
# 🧠 Traffic Data AI Project (2025.05 기준)

본 프로젝트는 교통사고 인식 및 예측을 위한 YOLO, UFLD, VTN 모델 기반의 복합 AI 시스템입니다.  
다양한 프레임워크가 통합되어 있으며, 디렉토리는 기능 중심으로 정리되어 있습니다.

---

## 🗂️ 1. 이미지 및 YOLO 라벨 관련

| 경로 | 설명 | 확장자 |
|------|------|--------|
| `/home/j-k12e203/traffic_data/train/images/t_junction/` | 학습용 이미지 | `.png` |
| `/home/j-k12e203/traffic_data/train/labels/t_junction/` | 학습용 YOLO 라벨 | `.txt` |
| `/home/j-k12e203/traffic_data/val/images/t_junction/` | 검증용 이미지 | `.png` |
| `/home/j-k12e203/traffic_data/val/labels/t_junction/` | 검증용 YOLO 라벨 | `.txt` |
| `/home/j-k12e203/traffic_data/yolo_outputs/t_junction/` | YOLO 추론 결과 (이미지 + 라벨) | `.jpg`, `.txt` |

---

## 🗂️ 2. UFLD 추론 결과 (현재 사용 안 함)

| 경로 | 설명 |
|------|------|
| `/home/j-k12e203/traffic_data/ufld_jsons/` | UFLD 추론 결과 JSON (`lanes`, `h_samples` 포함) |
| `/home/j-k12e203/traffic_data/Ultra-Fast-Lane-Detection/weights/culane_18.pth` | UFLD 사전학습 가중치 |

---

## 🗂️ 3. VTN 관련 핵심 데이터

| 경로 | 설명 |
|------|------|
| `/home/j-k12e203/traffic_data/merged_analysis/visualized/` | YOLO + UFLD 시각화 결과 |
| `/home/j-k12e203/traffic_data/merged_analysis/timeline_logs/` | VTN timeline log (future용) |
| `/home/j-k12e203/traffic_data/merged_analysis/vtn_inputs/` | 최종 VTN 학습용 `.jsonl` 파일 |

---

## 🗂️ 4. VTN 시퀀스 라벨 데이터

| 경로 | 설명 |
|------|------|
| `/home/j-k12e203/traffic_data/VTN/t_junction/video_jsons/Training/` | 영상 통합 json (훈련) |
| `/home/j-k12e203/traffic_data/VTN/t_junction/video_jsons/Validation/` | 영상 통합 json (검증) |
| `/home/j-k12e203/traffic_data/VTN/t_junction/image_jsons/` | 시퀀스별 프레임 YOLO bbox 중심 JSON |

---

## 🗂️ 5. vehicle_B 관련 주요 생성 데이터

| 경로 | 파일명 | 설명 |
|------|--------|------|
| `/home/j-k12e203/traffic_data/VTN/t_junction/Scripts/` | `vehicleB_trajectories.json` | bbox 궤적 (frame_idx, cx, cy, w, h, x, y 포함) |
| | `vehicleB_directions.json` | polyfit 기반 방향 분류 결과 |
| | `vehicleB_direction_hod.json` | HOD 기반 방향 추정 결과 |
| | `compare_directions_with_progress_info.py` | 방향 정확도 검증 스크립트 |

---

## 🗂️ 6. vehicle_B 궤적 기반 학습 데이터 (MLP/LSTM)

| 경로 | 파일명 | 설명 |
|------|--------|------|
| `/home/j-k12e203/traffic_data/VTN/t_junction/Scripts/vehicleB_features/` | `vehicleB_feature_dataset.jsonl` | 원본 feature 학습 데이터 (851개) |
| | `vehicleB_augmented_dataset.jsonl` | from_right 중심 증강 (125개) |
| | `vehicleB_combined_dataset.jsonl` | 병합된 최종 데이터셋 (976개) |
| | `missing_label_report.txt` | GT 없는 시퀀스 리포트 |
| | `analyze_feature_label_distribution.py` | label 비율 통계 출력 |
| | `train_mlp_classifier.py` | MLP 학습 스크립트 (GPU 2번 제한 포함) |
| | `generate_feature_dataset.py` | feature 추출 jsonl 생성 |
| | `generate_augmented_features.py` | 증강용 jsonl 생성 |
| | `merge_feature_datasets.py` | jsonl 병합 스크립트 |

---

## 🗂️ 7. 시각화 관련

| 경로 | 설명 |
|------|------|
| `/home/j-k12e203/traffic_data/VTN/t_junction/plots/` | vehicle_B 궤적 시각화 이미지 저장 경로 |
| *.png 예시 | `00_bb_1_130317_vehicle_35_061.png` → HOD/trajectory 기반 시각화 결과 |

---

## ✅ 참고
- 모든 경로는 **절대경로** 기준 `/home/j-k12e203/traffic_data/` 아래 구성되어 있습니다.
- UFLD는 현재 사용하지 않고 있으며, 추후 필요 시 재사용 가능하도록 결과만 보존되어 있습니다.
- 학습 및 추론 시 GPU 2번만 사용하도록 모든 스크립트에 명시적 제한이 포함되어 있습니다.

경로 설계에 대해서 방향성 잡음
masking 진행함.
