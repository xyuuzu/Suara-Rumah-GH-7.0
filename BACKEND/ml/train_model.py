"""
Script buat training model klasifikasi suara.

CARA PAKAI:
1. Siapkan dataset audio, struktur folder kayak gini:

   ml/dataset/
   ├── kdrt/          <- rekaman/contoh suara indikasi kekerasan (teriakan, bentakan, dll)
   │   ├── audio1.wav
   │   ├── audio2.wav
   ├── normal/        <- rekaman suara normal/aman
   │   ├── audio1.wav
   │   ├── audio2.wav

2. Jalankan: python ml/train_model.py

3. Model hasil training bakal tersimpan di ml/trained_model/model.pkl
"""

import os
import sys
import numpy as np
import joblib
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from sklearn.metrics import classification_report, confusion_matrix

# supaya bisa import feature_extraction dari folder app/
sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from app.services.feature_extraction import extract_features

DATASET_DIR = os.path.join(os.path.dirname(__file__), "dataset")
MODEL_OUT_DIR = os.path.join(os.path.dirname(__file__), "trained_model")

os.makedirs(MODEL_OUT_DIR, exist_ok=True)


def load_dataset():
    """Loop tiap folder label di dataset/, ekstrak fitur tiap file audio."""
    X, y = [], []
    labels = sorted(os.listdir(DATASET_DIR))  # misal: ['kdrt', 'normal']

    for label in labels:
        label_path = os.path.join(DATASET_DIR, label)
        if not os.path.isdir(label_path):
            continue

        files = [f for f in os.listdir(label_path) if f.lower().endswith((".wav", ".mp3"))]
        print(f"[INFO] Label '{label}': {len(files)} file audio")

        for fname in files:
            fpath = os.path.join(label_path, fname)
            try:
                features = extract_features(fpath)
                X.append(features)
                y.append(label)
            except Exception as e:
                print(f"[WARN] Gagal proses {fpath}: {e}")

    return np.array(X), np.array(y), labels


def main():
    print("[1/4] Load dataset & ekstraksi fitur (suara -> angka)...")
    X, y, labels = load_dataset()

    if len(X) == 0:
        print("[ERROR] Dataset kosong. Taruh file audio di ml/dataset/<label>/")
        return

    print(f"[INFO] Total data: {len(X)}, jumlah fitur per sample: {X.shape[1]}")

    print("[2/4] Split data train/test...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # normalisasi fitur -> penting karena skala MFCC, RMS, pitch beda-beda jauh
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    print("[3/4] Training model (RandomForest)...")
    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=None,
        random_state=42,
        class_weight="balanced",  # penting kalau data kdrt vs normal ga seimbang jumlahnya
    )
    model.fit(X_train_scaled, y_train)

    print("[4/4] Evaluasi model...")
    y_pred = model.predict(X_test_scaled)
    print(classification_report(y_test, y_pred))
    print("Confusion matrix:")
    print(confusion_matrix(y_test, y_pred))

    # simpan model + scaler jadi satu file, biar gampang di-load pas inference
    joblib.dump(
        {"model": model, "scaler": scaler, "labels": labels},
        os.path.join(MODEL_OUT_DIR, "model.pkl"),
    )
    print(f"[DONE] Model tersimpan di {MODEL_OUT_DIR}/model.pkl")


if __name__ == "__main__":
    main()
