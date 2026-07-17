"""
Koneksi ke Firebase (Firestore) buat nyimpen history hasil deteksi.

SETUP (dev lokal, pakai FILE):
1. Buka Firebase Console -> Project Settings -> Service Accounts
2. Generate new private key -> download file JSON
3. Simpan file itu sebagai `firebase-credentials.json` di root project
   (JANGAN di-commit ke git! tambahin ke .gitignore)
4. Set path-nya di .env: FIREBASE_CREDENTIALS_PATH=firebase-credentials.json

SETUP (deploy cloud, mis. Render, pakai ENV VAR):
Platform cloud kayak Render ga punya cara gampang buat "upload file" ke
env aplikasi lewat API -- jadi kalau `FIREBASE_CREDENTIALS_JSON` diisi
(isi PERSIS file JSON credential-nya, bukan path), dipakai duluan
daripada FIREBASE_CREDENTIALS_PATH. firebase_admin.credentials.Certificate()
nerima dict langsung, ga harus dari file.
"""

import json
import os
import firebase_admin
from firebase_admin import credentials, firestore
from dotenv import load_dotenv

load_dotenv()

_db = None


def init_firebase():
    global _db
    if _db is not None:
        return _db

    cred_json = os.getenv("FIREBASE_CREDENTIALS_JSON")
    if cred_json:
        try:
            cred = credentials.Certificate(json.loads(cred_json))
        except (json.JSONDecodeError, ValueError) as e:
            print(f"[WARN] FIREBASE_CREDENTIALS_JSON ga valid: {e}")
            return None
    else:
        cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH", "firebase-credentials.json")
        if not os.path.exists(cred_path):
            print(f"[WARN] File credential Firebase tidak ditemukan di {cred_path}")
            return None
        cred = credentials.Certificate(cred_path)

    firebase_admin.initialize_app(cred)
    _db = firestore.client()
    print("[INFO] Firebase berhasil terkoneksi.")
    return _db


def save_detection_result(user_id: str, prediction: str, confidence: float, extra: dict = None):
    """Simpan 1 hasil deteksi ke koleksi 'detections' di Firestore."""
    db = init_firebase()
    if db is None:
        print("[WARN] Firebase belum terkoneksi, hasil deteksi tidak disimpan.")
        return None

    doc_data = {
        "user_id": user_id,
        "prediction": prediction,
        "confidence": confidence,
        "timestamp": firestore.SERVER_TIMESTAMP,
    }
    if extra:
        doc_data.update(extra)

    doc_ref = db.collection("detections").add(doc_data)
    return doc_ref


def get_alerts_for_device(user_id: str, limit: int = 20) -> list:
    """Ambil histori deteksi/alert buat 1 device, terbaru duluan. Dipakai buat panel 'Histori Alert' di app."""
    db = init_firebase()
    if db is None:
        return []

    query = (
        db.collection("detections")
        .where("user_id", "==", user_id)
        .order_by("timestamp", direction=firestore.Query.DESCENDING)
        .limit(limit)
    )

    results = []
    for doc in query.stream():
        data = doc.to_dict()
        data["id"] = doc.id
        if data.get("timestamp") is not None:
            data["timestamp"] = data["timestamp"].isoformat()
        results.append(data)
    return results


def get_alert_by_id(alert_id: str) -> dict | None:
    """Ambil 1 dokumen alert by id -- dipakai buat cari tau device pemilik alert (buat confirm-safe)."""
    db = init_firebase()
    if db is None:
        return None
    doc = db.collection("detections").document(alert_id).get()
    if not doc.exists:
        return None
    data = doc.to_dict()
    data["id"] = doc.id
    return data


def mark_alert_safe(alert_id: str) -> None:
    """Tandai 1 alert sebagai 'kondisi aman' (dipanggil pas user tap tombol 'Aman' setelah alert terkirim)."""
    db = init_firebase()
    if db is None:
        return
    db.collection("detections").document(alert_id).set({"status": "confirmed_safe"}, merge=True)
