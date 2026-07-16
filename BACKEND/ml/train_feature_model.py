"""
Script buat training model klasifikasi berbasis fitur ringan (RMS, Zero-
Crossing Rate, Peak Amplitude) -- 3 angka yang sama persis dengan yang
dikirim mobile ke POST /predict-features. Beda dari ml/train_model.py yang
melatih model dari fitur MFCC penuh hasil ekstraksi librosa atas audio
mentah -- model ini jauh lebih ringan (cuma 3 fitur input) karena memang
didesain buat jalan real-time dari data yang sudah diringkas di device.

CARA PAKAI:
1. Siapkan dataset di ml/dataset_features/data.csv, kolom: rms,zcr,peak,label
   (kalau belum ada, jalankan ml/generate_feature_dataset.py buat generate
   dataset sintetis dulu -- baca peringatan di file itu soal data sintetis)
2. (Opsional tapi disarankan) Jalankan ml/extract_real_features.py buat
   nambahin sinyal dari audio ASLI (ml/dataset/kdrt & ml/dataset/normal) --
   hasilnya di ml/dataset_features/real_audio_features.csv, otomatis
   ke-merge ke training set kalau file ini ada.
3. Jalankan: python ml/train_feature_model.py
4. Model hasil training tersimpan di ml/trained_model/feature_model.pkl
"""

import os

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

DATASET_PATH = os.path.join(os.path.dirname(__file__), "dataset_features", "data.csv")
REAL_DATASET_PATH = os.path.join(os.path.dirname(__file__), "dataset_features", "real_audio_features.csv")
MODEL_OUT_DIR = os.path.join(os.path.dirname(__file__), "trained_model")

# audio real jumlahnya jauh lebih sedikit dari sintetis (puluhan vs ribuan) --
# di-duplikasi sekian kali biar sinyalnya ga tenggelam pas digabung
REAL_DATA_OVERSAMPLE = 20

os.makedirs(MODEL_OUT_DIR, exist_ok=True)


def main():
    print("[1/4] Load dataset fitur (rms/zcr/peak)...")
    if not os.path.exists(DATASET_PATH):
        print(f"[ERROR] Dataset tidak ditemukan di {DATASET_PATH}")
        print("        Jalankan ml/generate_feature_dataset.py dulu, atau siapkan CSV: rms,zcr,peak,label")
        return

    df = pd.read_csv(DATASET_PATH).dropna(subset=["rms", "zcr", "peak", "label"])
    print(f"[INFO] Data sintetis: {len(df)}")

    if os.path.exists(REAL_DATASET_PATH):
        real_df = pd.read_csv(REAL_DATASET_PATH).dropna(subset=["rms", "zcr", "peak", "label"])
        real_df_oversampled = pd.concat([real_df] * REAL_DATA_OVERSAMPLE, ignore_index=True)
        print(f"[INFO] Data audio real: {len(real_df)} (di-oversample {REAL_DATA_OVERSAMPLE}x jadi {len(real_df_oversampled)})")
        df = pd.concat([df, real_df_oversampled], ignore_index=True)
    else:
        print("[INFO] ml/dataset_features/real_audio_features.csv belum ada -- training cuma pakai data sintetis.")
        print("       Jalankan ml/extract_real_features.py buat nambahin sinyal dari audio asli.")

    print(f"[INFO] Total data training: {len(df)}")
    print(df["label"].value_counts())

    X = df[["rms", "zcr", "peak"]].values
    y = df["label"].values

    print("[2/4] Split data train/test...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    print("[3/4] Training model (RandomForest)...")
    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=10,          # dibatasi biar ga hafal batas keputusan terlalu tajam
        min_samples_leaf=15,   # tiap leaf butuh cukup sample -> probabilitas ga gampang 1.0 mentah
        random_state=42,
        class_weight="balanced",
    )
    model.fit(X_train_scaled, y_train)

    print("[4/4] Evaluasi model...")
    y_pred = model.predict(X_test_scaled)
    print(classification_report(y_test, y_pred))
    print("Confusion matrix:")
    print(confusion_matrix(y_test, y_pred))

    joblib.dump(
        {"model": model, "scaler": scaler, "labels": sorted(df["label"].unique())},
        os.path.join(MODEL_OUT_DIR, "feature_model.pkl"),
    )
    print(f"[DONE] Model tersimpan di {MODEL_OUT_DIR}/feature_model.pkl")


if __name__ == "__main__":
    main()
