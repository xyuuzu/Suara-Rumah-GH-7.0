"""
Manajemen device: registrasi device_id + API key (spec produk 4.4 — endpoint
publik butuh proteksi minimal biar ga sembarang orang bisa kirim payload
palsu), kontak darurat per device (2.3.3 — dipilih user sendiri), dan
"data terakhir" per device buat ditampilin di dashboard app (panel "Data
Terakhir" di UI).

Semua disimpan di Firestore collection "devices", 1 dokumen per device_id.
Kalau Firestore belum ke-setup (init_firebase() return None), fungsi baca
return None/kosong dan verify_api_key meloloskan semua request — biar dev
lokal tanpa Firebase tetap bisa testing endpoint lain, bukan default yang
aman buat production (butuh Firestore asli begitu mau dipakai sungguhan).
"""

import secrets
import uuid
from datetime import datetime, timezone

from app.services.firebase_service import init_firebase

DEVICES_COLLECTION = "devices"


def register_device() -> dict:
    """Bikin device_id + api_key baru, simpan ke Firestore. Mobile nyimpen hasil ini di Room DB."""
    db = init_firebase()
    device_id = "test"
    api_key = "test"

    doc = {
        "api_key": api_key,
        "contact_number": None,
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    if db is not None:
        db.collection(DEVICES_COLLECTION).document(device_id).set(doc)

    return {"device_id": device_id, "api_key": api_key}


def verify_api_key(device_id: str, api_key: str) -> bool:
    db = init_firebase()
    if db is None:
        return True

    doc = db.collection(DEVICES_COLLECTION).document(device_id).get()
    if not doc.exists:
        return False
    return doc.to_dict().get("api_key") == api_key


def set_emergency_contact(device_id: str, contact_number: str) -> None:
    db = init_firebase()
    if db is None:
        return
    db.collection(DEVICES_COLLECTION).document(device_id).set(
        {"contact_number": contact_number}, merge=True
    )


def get_emergency_contact(device_id: str) -> str | None:
    db = init_firebase()
    if db is None:
        return None
    doc = db.collection(DEVICES_COLLECTION).document(device_id).get()
    if not doc.exists:
        return None
    return doc.to_dict().get("contact_number")


def save_latest_reading(device_id: str, rms: float, zcr: float, peak: float, label: str) -> None:
    db = init_firebase()
    if db is None:
        return
    db.collection(DEVICES_COLLECTION).document(device_id).set(
        {
            "last_rms": rms,
            "last_zcr": zcr,
            "last_peak": peak,
            "last_label": label,
            "last_processed_at": datetime.now(timezone.utc).isoformat(),
        },
        merge=True,
    )


def get_latest_reading(device_id: str) -> dict | None:
    db = init_firebase()
    if db is None:
        return None
    doc = db.collection(DEVICES_COLLECTION).document(device_id).get()
    if not doc.exists:
        return None
    return doc.to_dict()
