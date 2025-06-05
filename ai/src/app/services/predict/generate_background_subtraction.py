#!/usr/bin/env python3
import argparse
import os
import cv2
import shutil
from glob import glob
import logging

# ─── 로거 설정 ───────────────────────────────────────────────────────────────
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

def generate_background_subtraction(
    frames_dir, output_dir=None, history=200, var_thresh=16.0, learning=0.005
):
    """
    GPU를 사용한 MOG2 배경 제거
    
    Args:
        frames_dir (str): 입력 프레임 디렉토리
        output_dir (str): 출력 디렉토리 (None이면 frames_dir/bg_gpu)
        history (int): MOG2 history
        var_thresh (float): MOG2 varThreshold
        learning (float): MOG2 learningRate
    """
    if output_dir is None:
        output_dir = os.path.join(frames_dir, "bg_gpu")
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)

    mog_gpu = cv2.cuda.createBackgroundSubtractorMOG2(
        history=history, varThreshold=var_thresh, detectShadows=False
    )
    stream = cv2.cuda_Stream()

    jpgs = sorted(glob(os.path.join(frames_dir, "*.jpg")))
    if not jpgs:
        logger.warning("프레임이 하나도 없습니다.")
        return output_dir

    prev_fg_mask = None
    total = len(jpgs)
    step = max(1, total // 10)  # 10% 마다 로그
    logger.info(f"GPU 배경 제거 시작 ({total} 프레임, {step}-프레임마다 10% 로그)")

    for idx, path in enumerate(jpgs, 1):
        frame     = cv2.imread(path)
        gpu_frame = cv2.cuda_GpuMat(); gpu_frame.upload(frame)

        gpu_gray = cv2.cuda.cvtColor(gpu_frame, cv2.COLOR_BGR2GRAY)
        gpu_fg   = mog_gpu.apply(gpu_gray, learning, stream)
        stream.waitForCompletion()
        fg_mask  = gpu_fg.download() >= 128

        if prev_fg_mask is None:
            new_obj = fg_mask
        else:
            new_obj = fg_mask & (~prev_fg_mask)
        prev_fg_mask = fg_mask.copy()

        bg_mask = (~fg_mask & ~new_obj).astype("uint8") * 255
        bg_only = cv2.bitwise_and(frame, frame, mask=bg_mask)

        bg_model_gpu = mog_gpu.getBackgroundImage(stream)
        stream.waitForCompletion()
        bg_model = bg_model_gpu.download() if bg_model_gpu is not None else None

        name = os.path.splitext(os.path.basename(path))[0]
        cv2.imwrite(f"{output_dir}/{name}_bg.png",    bg_only)
        cv2.imwrite(f"{output_dir}/{name}_mask.png",  (fg_mask*255).astype("uint8"))
        cv2.imwrite(f"{output_dir}/{name}_new.png",   (new_obj*255).astype("uint8"))
        if bg_model is not None:
            cv2.imwrite(f"{output_dir}/{name}_bgmodel.png", bg_model)

        # 10% 단위 진행률 로그
        if idx % step == 0 or idx == total:
            logger.info(f"[{idx}/{total}] {(idx/total)*100:.0f}% 완료")

    logger.info(f"✅ GPU 배경 제거 완료 → {output_dir}")
    return output_dir

def main():
    parser = argparse.ArgumentParser(
        description="GPU로 MOG2 배경 제거: _bg, _mask, _new, _bgmodel.png 생성"
    )
    parser.add_argument(
        "--frames-dir", required=True,
        help="입력 프레임 디렉토리 (예: frames/video_002)"
    )
    parser.add_argument(
        "--output-dir", default=None,
        help="결과 저장 디렉토리 (기본: frames-dir/bg_gpu)"
    )
    parser.add_argument(
        "--history", type=int, default=200,
        help="MOG2 history 값"
    )
    parser.add_argument(
        "--var-thresh", type=float, default=16.0,
        help="MOG2 varThreshold 값"
    )
    parser.add_argument(
        "--learning", type=float, default=0.005,
        help="MOG2 learningRate 값"
    )
    args = parser.parse_args()

    generate_background_subtraction_gpu(
        frames_dir=args.frames_dir,
        output_dir=args.output_dir,
        history=args.history,
        var_thresh=args.var_thresh,
        learning=args.learning
    )

if __name__ == "__main__":
    main()
