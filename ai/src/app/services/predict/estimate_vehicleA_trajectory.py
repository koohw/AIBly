#!/usr/bin/env python3
import os
import cv2
import numpy as np
import pandas as pd
from glob import glob
from scipy.signal import medfilt
from scipy.ndimage import gaussian_filter1d
import shutil
import time
import logging
import subprocess
from fastapi import HTTPException
from ...config import Config

# 로거 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MOG2_HISTORY = 200
MOG2_VAR_THRESH = 16
STEP = 15
CALIB_FRAMES = 60
MED_KSIZE = 11
GAUSS_SIGMA = 2
COLL_DIFF = 5.0
COLL_SMOOTH_WIN = 11
RANSAC_MIN_QUALITY = 0.3
RANSAC_REPROJ_THRESH = 4.0
METHODS = ["sparse_lk", "dense_far", "ransac_affine"]

def check_gpu_available():
    try:
        res = subprocess.run(
            ["nvidia-smi", "--query-gpu=name", "--format=csv,noheader"],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=5
        )
        return res.returncode == 0 and bool(res.stdout.strip())
    except:
        return False

def estimate_vehicleA_trajectory(analysis_result):
    # 환경 설정
    os.environ["CUDA_DEVICE_ORDER"]   = Config.CUDA_DEVICE_ORDER
    os.environ["CUDA_VISIBLE_DEVICES"] = Config.CUDA_VISIBLE_DEVICES
    os.environ["OMP_NUM_THREADS"]     = "2"
    os.environ["MKL_NUM_THREADS"]     = "2"

    # GPU 확인 및 초기화
    if not check_gpu_available():
        raise HTTPException(500, "차량A 궤적 추정에 필요한 GPU가 감지되지 않았습니다.")
    cv2.cuda.setDevice(0)
    logger.info("🚀 GPU 사용: 장치 #0")

    # 경로 준비
    user_id    = analysis_result["userId"]
    video_id   = analysis_result["videoId"]
    frames_dir = analysis_result["frames_dir"]
    out_dir    = os.path.join(Config.TRAJECTORY_PATH, str(user_id), str(video_id))
    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    # 프레임 로드
    jpgs = sorted(glob(f"{frames_dir}/*.jpg"))
    if not jpgs:
        return {"status":"error","message":"No frames found"}
    grays_full = [cv2.cvtColor(cv2.imread(p), cv2.COLOR_BGR2GRAY) for p in jpgs]
    grays = grays_full.copy()
    N, H, W = len(grays), *grays[0].shape

    # 배경 차분
    bgsub_flow  = cv2.createBackgroundSubtractorMOG2(MOG2_HISTORY, MOG2_VAR_THRESH, False)
    prev_fgmask = np.zeros((H, W), dtype=bool)

    # 포인트 그리드
    yy, xx   = np.mgrid[0:H:STEP, 0:W:STEP]
    pts_all  = np.vstack((xx.ravel(), yy.ravel())).T.astype(np.float32).reshape(-1,1,2)

    # GPU 옵티컬 플로우 객체
    stream    = cv2.cuda_Stream()
    sparse_lk = cv2.cuda.SparsePyrLKOpticalFlow.create(winSize=(15,15), maxLevel=2, iters=10, useInitialFlow=False)
    dense_far = cv2.cuda.FarnebackOpticalFlow.create(numLevels=3, pyrScale=0.5,
                                                     fastPyramids=False, winSize=15,
                                                     numIters=3, polyN=5, polySigma=1.2, flags=0)

    raw, qualities = {}, {}

    for name in METHODS:
        logger.info(f"[{name}] 옵티컬 플로우 계산 시작 (GPU 사용)")
        dxs, dys, qs = [0.0], [0.0], [0.0]
        prev_gray     = grays[0]
        prev_fgmask[:] = False
        prev_gray_gpu = cv2.cuda_GpuMat()
        prev_gray_gpu.upload(prev_gray)

        for idx, gray in enumerate(grays[1:], 1):
            # 진행률 로그
            if idx % max(1, N//10) == 0:
                logger.info(f"[{name}] 진행률: {idx}/{N} ({idx/N*100:.1f}%)")

            fg     = bgsub_flow.apply(gray, learningRate=0.005)
            fgmask = fg >= 128
            new_obj= fgmask & ~prev_fgmask
            prev_fgmask[:] = fgmask
            mask   = (~fgmask & ~new_obj)

            dx = dy = q = 0.0

            if name == "sparse_lk":
                sel = mask[::STEP, ::STEP].ravel()
                pts = pts_all[sel]
                if pts.size:
                    pts1     = pts.reshape(1, -1, 2).astype(np.float32)
                    p0_gpu   = cv2.cuda_GpuMat(); p0_gpu.upload(pts1)
                    gray_gpu = cv2.cuda_GpuMat(); gray_gpu.upload(gray)
                    next_pts = cv2.cuda_GpuMat()
                    status   = cv2.cuda_GpuMat()
                    err      = cv2.cuda_GpuMat()

                    sparse_lk.calc(prev_gray_gpu, gray_gpu, p0_gpu, next_pts, status, err, stream)
                    stream.waitForCompletion()
                    p1 = next_pts.download()
                    st = status.download()

                    if st is None or st.size == 0:
                        dx = dy = q = 0.0
                    else:
                        ok       = (st.flatten() == 1)
                        q        = ok.sum() / len(pts1[0])
                        if ok.any():
                            old = pts1[0][ok]
                            new = p1[0][ok]
                            flow = new - old
                            dx, dy = -flow.mean(axis=0)

            elif name == "dense_far":
                gray_gpu = cv2.cuda_GpuMat(); gray_gpu.upload(gray)
                flow_gpu = dense_far.calc(prev_gray_gpu, gray_gpu, None, stream)
                stream.waitForCompletion()
                flow     = flow_gpu.download()
                fx, fy   = flow[...,0][mask], flow[...,1][mask]
                q        = np.isfinite(fx).sum() / (mask.sum() or 1)
                if fx.size:
                    dx, dy = -np.median(fx), -np.median(fy)

            else:  # ransac_affine
                pts1     = pts_all.reshape(1, -1, 2).astype(np.float32)
                p_gpu    = cv2.cuda_GpuMat(); p_gpu.upload(pts1)
                gray_gpu = cv2.cuda_GpuMat(); gray_gpu.upload(gray)
                next_pts = cv2.cuda_GpuMat()
                status   = cv2.cuda_GpuMat()
                err      = cv2.cuda_GpuMat()

                sparse_lk.calc(prev_gray_gpu, gray_gpu, p_gpu, next_pts, status, err, stream)
                stream.waitForCompletion()
                p1 = next_pts.download()
                st = status.download()

                if st is None or st.size == 0:
                    dx = dy = q = 0.0
                else:
                    ok       = (st.flatten() == 1)
                    q        = ok.sum() / len(pts_all)
                    if q >= RANSAC_MIN_QUALITY and ok.sum() >= 3:
                        old = pts_all[ok].reshape(-1,2)
                        new = p1[0][ok].reshape(-1,2)
                        M, _ = cv2.estimateAffinePartial2D(old, new,
                                                          method=cv2.RANSAC,
                                                          ransacReprojThreshold=RANSAC_REPROJ_THRESH)
                        if M is not None:
                            dx, dy = -M[0,2], -M[1,2]
                        else:
                            flow = new - old
                            dx, dy = -np.median(flow, axis=0)

            dxs.append(float(dx))
            dys.append(float(dy))
            qs.append(float(q))
            prev_gray_gpu.upload(gray)

        raw[name]       = (np.array(dxs), np.array(dys))
        qualities[name] = np.array(qs)
        pd.DataFrame({
            f"{name}_dx":      dxs,
            f"{name}_dy":      dys,
            f"{name}_quality": qs
        }).to_csv(f"{out_dir}/{name}_raw_flow.csv", index_label="frame")

    # 후처리 및 저장
    proc, colls = {}, {}
    for name, (dx, dy) in raw.items():
        V0     = np.vstack([dx[1:CALIB_FRAMES], dy[1:CALIB_FRAMES]]).T
        u      = V0.mean(axis=0); u /= (np.linalg.norm(u) + 1e-8)
        l      = np.array([-u[1], u[0]])
        lat    = np.stack([dx, dy], axis=1).dot(l); lat[:CALIB_FRAMES] = 0.0
        lat_med= medfilt(lat, kernel_size=MED_KSIZE)
        lat_sm = gaussian_filter1d(lat_med, sigma=GAUSS_SIGMA)
        traj   = np.cumsum(lat_sm)
        d      = np.abs(np.diff(traj, prepend=traj[0]))
        s      = np.convolve(d, np.ones(COLL_SMOOTH_WIN)/COLL_SMOOTH_WIN, mode="same")
        coll   = int(np.argmax(s > COLL_DIFF))
        proc[name]  = traj
        colls[name] = coll
        pd.DataFrame({
            f"{name}_lat_raw":    lat,
            f"{name}_lat_med":    lat_med,
            f"{name}_lat_smooth": lat_sm,
            f"{name}_traj":       traj
        }).to_csv(f"{out_dir}/{name}_processed.csv", index_label="frame")

    # 앙상블
    all_traj = np.vstack([proc[m] for m in METHODS])
    ens_traj = np.median(all_traj, axis=0)
    ens_coll = int(pd.Series(list(colls.values())).median())
    pd.DataFrame({
        **{f"{m}_traj": proc[m] for m in METHODS},
        "ensemble_traj":  ens_traj,
        "collision_idx":  ens_coll
    }).to_csv(os.path.join(out_dir, "ensemble_trajectory.csv"), index_label="frame")

    return {
        "ensemble_trajectory": ens_traj.tolist(),
        "collision_idx":       ens_coll,
        "trajectory_methods":  METHODS,
        "output_dir":          out_dir,
        "csv_path":            os.path.join(out_dir, "ensemble_trajectory.csv"),
        "status":              "success",
        "gpu_used":            True
    }
