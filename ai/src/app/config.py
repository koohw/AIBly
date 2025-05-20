import os

class Config:
    # GPU 설정
    CUDA_DEVICE_ORDER = "PCI_BUS_ID"
    CUDA_VISIBLE_DEVICES = "0"  # 사용할 GPU 번호
    
    @classmethod
    def set_gpu_environment(cls):
        """GPU 환경 설정"""
        os.environ["CUDA_DEVICE_ORDER"] = cls.CUDA_DEVICE_ORDER
        os.environ["CUDA_VISIBLE_DEVICES"] = cls.CUDA_VISIBLE_DEVICES
    
    # app 폴더 경로
    APP_PATH = os.path.dirname(__file__)
    
    # resources 폴더 경로
    RESOURCES_PATH = os.path.join(APP_PATH, "resources")

    # user 폴더 경로
    USER_PATH = os.path.join(RESOURCES_PATH, "users")

    # yolo 모델 경로
    YOLO_MODEL_PATH = os.path.join(RESOURCES_PATH, "yolo_models", "best.pt")

    # lstm 모델 경로
    LSTM_MODEL_PATH = os.path.join(RESOURCES_PATH, "lstm_models", "lstm_model.pt")

    # VTN pkl 경로
    VTN_PKL_PATH = os.path.join(RESOURCES_PATH, "vtn_pkls")
    
    # VTN Model 경로
    VTN_MODEL_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(APP_PATH))), "vtn_model")

    # label_map.json 경로
    LABEL_MAP_PATH = os.path.join(RESOURCES_PATH, "label_maps", "label_map.json")

    # accident_data 경로
    ACCIDENT_DATA_CSV_PATH = os.path.join(RESOURCES_PATH, "accident_datas", "accident_data.csv")

    # trajectory 경로
    TRAJECTORY_PATH = os.path.join(RESOURCES_PATH, "trajectory")