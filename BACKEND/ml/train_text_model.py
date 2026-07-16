"""
Script buat training model klasifikasi teks (chat/transkrip ucapan).

CARA PAKAI:
1. Siapkan dataset CSV di ml/dataset_text/data.csv, 2 kolom: text,label
   label cuma 2 nilai: "kdrt" atau "normal"

2. Jalankan: python ml/train_text_model.py

3. Model hasil training bakal tersimpan di ml/trained_model/text_model.pkl
"""

import os
import sys

import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import train_test_split

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from app.services.text_detection import preprocess_text

DATASET_PATH = os.path.join(os.path.dirname(__file__), "dataset_text", "data.csv")
MODEL_OUT_DIR = os.path.join(os.path.dirname(__file__), "trained_model")

os.makedirs(MODEL_OUT_DIR, exist_ok=True)


def main():
    print("[1/4] Load dataset teks...")
    if not os.path.exists(DATASET_PATH):
        print(f"[ERROR] Dataset tidak ditemukan di {DATASET_PATH}")
        print("        Siapkan CSV dengan kolom: text,label")
        return

    df = pd.read_csv(DATASET_PATH)
    df = df.dropna(subset=["text", "label"])
    print(f"[INFO] Total data: {len(df)}")
    print(df["label"].value_counts())

    print("[2/4] Preprocessing teks (lowercase, hapus stopword)...")
    df["clean_text"] = df["text"].astype(str).apply(preprocess_text)

    X_train, X_test, y_train, y_test = train_test_split(
        df["clean_text"], df["label"], test_size=0.2, random_state=42, stratify=df["label"]
    )

    print("[3/4] TF-IDF vectorize + training model (LogisticRegression)...")
    vectorizer = TfidfVectorizer(max_features=3000, ngram_range=(1, 2))
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)

    model = LogisticRegression(max_iter=1000, class_weight="balanced")
    model.fit(X_train_vec, y_train)

    print("[4/4] Evaluasi model...")
    y_pred = model.predict(X_test_vec)
    print(classification_report(y_test, y_pred))
    print("Confusion matrix:")
    print(confusion_matrix(y_test, y_pred))

    joblib.dump(
        {"model": model, "vectorizer": vectorizer, "labels": sorted(df["label"].unique())},
        os.path.join(MODEL_OUT_DIR, "text_model.pkl"),
    )
    print(f"[DONE] Model tersimpan di {MODEL_OUT_DIR}/text_model.pkl")


if __name__ == "__main__":
    main()
