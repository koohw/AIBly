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
최종 파일구조
/home/j-k12e203/traffic_data/
├── images/                 # (현재는 YOLO용만 정리되어 있음)
/videos/
│   ├── TL/
│   │   ├── t_junction/
│   │   │   ├── videos/      # mp4 영상
│   │   │   └── json/        # 각 영상별 사고 정보 json
│   ├── TS/
│   │   └── (동일)
/vtn_sequences/              # bbox tracklet 기반 VTN 입력 (accident type 없음)
/vtn_sequences_labeled/      # bbox + accident type 매핑 완료된 VTN 입력
/vtn_accident_project/       # VTN 모델, Dataset, config 파일들
--------------------------------
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
