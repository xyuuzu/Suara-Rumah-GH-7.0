"""
Demo audio picker -- pas sistem mendeteksi kekerasan (baik dari /predict
maupun /predict-features), lampirin 1 klip audio DEMO ke pesan WA alert,
diambil dari dataset audio yang UDAH ADA di project (ml/dataset/kdrt/,
hasil real dari folder 'Data sound kdrt/').

PENTING -- INI BUKAN audio asli dari kejadian yang lagi dideteksi:
- /predict-features cuma nerima RMS/ZCR/Peak (3 angka), ga pernah nerima
  audio mentah beneran sama sekali (sesuai arsitektur privasi produk).
- /predict emang nerima upload audio asli, TAPI yang dikirim ke WA bukan
  audio yang di-upload itu -- tetap klip demo dari dataset, biar
  konsisten & simpel (ga perlu nyimpen audio user secara live).

Klip demo ini dipakai murni buat bikin notifikasi WA lebih konkret/
meyakinkan pas demo hackathon, BUKAN bukti kejadian sungguhan. Kalau nanti
mau beneran kirim audio asli dari kejadian, itu perubahan arsitektur
terpisah (lihat catatan privasi di README).

Tiap alert yang make demo audio dicatet di saved_audio/audio_saved.json
(audit log ringan -- cuma metadata, BUKAN file audio baru, karena audio-
nya udah ada duluan di ml/dataset/kdrt/).
"""

import json
import os
import random
from datetime import datetime, timezone

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
DEMO_AUDIO_DIR = os.path.join(BASE_DIR, "ml", "dataset", "kdrt")
MANIFEST_DIR = os.path.join(BASE_DIR, "saved_audio")
MANIFEST_PATH = os.path.join(MANIFEST_DIR, "audio_saved.json")


def _list_demo_files() -> list:
    if not os.path.isdir(DEMO_AUDIO_DIR):
        return []
    return [f for f in os.listdir(DEMO_AUDIO_DIR) if f.lower().endswith((".mp3", ".wav"))]


def pick_demo_audio() -> str | None:
    """Pilih 1 file audio demo secara acak dari ml/dataset/kdrt/. Return nama file, atau None kalau folder kosong."""
    files = _list_demo_files()
    if not files:
        return None
    return random.choice(files)


def _load_manifest() -> list:
    if not os.path.exists(MANIFEST_PATH):
        return []
    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def _save_manifest(entries: list) -> None:
    os.makedirs(MANIFEST_DIR, exist_ok=True)
    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(entries, f, indent=2, ensure_ascii=False)


def log_demo_audio_used(filename: str, user_id: str, prediction: str, confidence: float) -> None:
    """Catat pemakaian klip demo ke saved_audio/audio_saved.json (audit log)."""
    entry = {
        "filename": filename,
        "source": "demo_dataset (ml/dataset/kdrt/)",
        "user_id": user_id,
        "prediction": prediction,
        "confidence": confidence,
        "used_at": datetime.now(timezone.utc).isoformat(),
    }
    manifest = _load_manifest()
    manifest.append(entry)
    _save_manifest(manifest)
