"""
Ekstrak fitur RMS/ZCR/Peak dari audio ASLI (ml/dataset/kdrt & ml/dataset/normal,
disalin dari 'Data sound kdrt/') buat menambah data training model
`/predict-features` dengan sinyal akustik nyata, bukan cuma sintetis murni.

Cara kerja:
1. Tiap file audio dipotong jadi window ~3 detik (mendekati rolling buffer
   yang dipakai mobile), RMS/ZCR/Peak dihitung per window -> beberapa
   sample per file, bukan cuma 1.
2. Window dari folder `normal/` langsung dilabel "normal" (ga ambigu, folder
   ini emang isinya suara mirip² keras/nangis TAPI BUKAN kekerasan).
3. Window dari folder `kdrt/` TIDAK punya sub-label per potongan detik
   (bantingan/teriakan/darurat_sos) -- audio real cuma dilabel "kdrt" secara
   keseluruhan, bukan per detik. Makanya tiap window di-assign ke kelas
   TERDEKAT (nearest-centroid) di antara 3 kelas kekerasan, dibandingkan ke
   centroid (rata-rata rms/zcr/peak) tiap kelas di dataset sintetis
   (ml/dataset_features/data.csv, HARUS sudah digenerate duluan lewat
   ml/generate_feature_dataset.py). Ini HEURISTIK, bukan ground-truth --
   didokumentasikan terbuka di sini biar ga disalahartikan sebagai label
   manual asli.

Output: ml/dataset_features/real_audio_features.csv, digabung otomatis sama
dataset sintetis pas ml/train_feature_model.py jalan.
"""

import os

import librosa
import numpy as np
import pandas as pd

BASE_DIR = os.path.dirname(__file__)
KDRT_DIR = os.path.join(BASE_DIR, "dataset", "kdrt")
NORMAL_DIR = os.path.join(BASE_DIR, "dataset", "normal")
SYNTHETIC_PATH = os.path.join(BASE_DIR, "dataset_features", "data.csv")
OUT_PATH = os.path.join(BASE_DIR, "dataset_features", "real_audio_features.csv")

WINDOW_SEC = 3.0
VIOLENCE_LABELS = ["bantingan", "teriakan", "darurat_sos"]


def extract_windows(path, window_sec=WINDOW_SEC):
    y, sr = librosa.load(path, sr=22050)
    y, _ = librosa.effects.trim(y)

    window_len = int(window_sec * sr)
    if len(y) < window_len:
        windows = [y]
    else:
        n_windows = len(y) // window_len
        windows = [y[i * window_len:(i + 1) * window_len] for i in range(n_windows)]

    results = []
    for w in windows:
        if len(w) == 0:
            continue
        rms = float(np.sqrt(np.mean(w ** 2)))
        zcr = float(np.mean(librosa.feature.zero_crossing_rate(w)))
        peak = float(np.max(np.abs(w)))
        results.append((rms, zcr, peak))
    return results


def load_violence_centroids():
    df = pd.read_csv(SYNTHETIC_PATH)
    centroids = {}
    for label in VIOLENCE_LABELS:
        sub = df[df["label"] == label]
        centroids[label] = (sub["rms"].mean(), sub["zcr"].mean(), sub["peak"].mean())
    return centroids


def nearest_violence_class(rms, zcr, peak, centroids):
    best_label, best_dist = None, float("inf")
    for label, (crms, czcr, cpeak) in centroids.items():
        dist = (rms - crms) ** 2 + (zcr - czcr) ** 2 + (peak - cpeak) ** 2
        if dist < best_dist:
            best_dist = dist
            best_label = label
    return best_label


def main():
    if not os.path.exists(SYNTHETIC_PATH):
        print(f"[ERROR] {SYNTHETIC_PATH} belum ada. Jalankan ml/generate_feature_dataset.py dulu.")
        return

    centroids = load_violence_centroids()
    print(f"[INFO] Centroid kelas kekerasan (dari dataset sintetis): {centroids}")

    rows = []

    kdrt_files = [f for f in os.listdir(KDRT_DIR) if f.lower().endswith((".mp3", ".wav"))]
    for fname in kdrt_files:
        path = os.path.join(KDRT_DIR, fname)
        windows = extract_windows(path)
        for rms, zcr, peak in windows:
            label = nearest_violence_class(rms, zcr, peak, centroids)
            rows.append({"rms": round(rms, 4), "zcr": round(zcr, 4), "peak": round(peak, 4), "label": label})
        print(f"[INFO] kdrt/{fname}: {len(windows)} window diekstrak")

    normal_files = [f for f in os.listdir(NORMAL_DIR) if f.lower().endswith((".mp3", ".wav"))]
    for fname in normal_files:
        path = os.path.join(NORMAL_DIR, fname)
        windows = extract_windows(path)
        for rms, zcr, peak in windows:
            rows.append({"rms": round(rms, 4), "zcr": round(zcr, 4), "peak": round(peak, 4), "label": "normal"})
        print(f"[INFO] normal/{fname}: {len(windows)} window diekstrak")

    df = pd.DataFrame(rows)
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    df.to_csv(OUT_PATH, index=False)
    print(f"[DONE] {len(df)} sample dari audio real ditulis ke {OUT_PATH}")
    print(df["label"].value_counts())
    print("Label kelas kekerasan (bantingan/teriakan/darurat_sos) di sini hasil heuristik")
    print("nearest-centroid, BUKAN ground-truth manual per detik audio.")


if __name__ == "__main__":
    main()
