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


def set_emergency_contact(device_id: str, contact_number: str) -> bool:
    """
    Simpan nomor kontak darurat. Kalau nomornya BEDA dari yang tersimpan
    sebelumnya (atau belum ada sama sekali), status konfirmasi di-reset ke
    False -- nomor baru harus opt-in ulang lewat balasan WA sebelum beneran
    dipakai buat kirim alert (lihat confirm_contact/reject_contact).

    Return True kalau ini nomor BARU/BERUBAH (perlu kirim pesan konfirmasi),
    False kalau nomornya sama persis kayak sebelumnya (ga perlu spam ulang
    minta konfirmasi).
    """
    db = init_firebase()
    if db is None:
        return True

    doc_ref = db.collection(DEVICES_COLLECTION).document(device_id)
    existing = doc_ref.get()
    existing_number = existing.to_dict().get("contact_number") if existing.exists else None

    is_new_number = existing_number != contact_number
    update = {"contact_number": contact_number}
    if is_new_number:
        update["contact_confirmed"] = False
    doc_ref.set(update, merge=True)
    return is_new_number


def get_emergency_contact(device_id: str, require_confirmed: bool = True) -> str | None:
    """
    Ambil nomor kontak darurat device ini. Default (require_confirmed=True):
    cuma return nomornya kalau kontak itu udah konfirmasi opt-in (balas IYA
    lewat WA) -- biar alert BENERAN cuma kekirim ke orang yang udah setuju,
    bukan asal nomor yang di-set doang. Set require_confirmed=False kalau
    memang butuh nomornya apa adanya (mis. buat nampilin di UI kontak).
    """
    db = init_firebase()
    if db is None:
        return None
    doc = db.collection(DEVICES_COLLECTION).document(device_id).get()
    if not doc.exists:
        return None
    data = doc.to_dict()
    if require_confirmed and not data.get("contact_confirmed", False):
        return None
    return data.get("contact_number")


def is_contact_confirmed(device_id: str) -> bool:
    db = init_firebase()
    if db is None:
        return False
    doc = db.collection(DEVICES_COLLECTION).document(device_id).get()
    if not doc.exists:
        return False
    return bool(doc.to_dict().get("contact_confirmed", False))


def confirm_contact(device_id: str) -> None:
    db = init_firebase()
    if db is None:
        return
    db.collection(DEVICES_COLLECTION).document(device_id).set({"contact_confirmed": True}, merge=True)


def reject_contact(device_id: str) -> None:
    """Kontak nolak jadi kontak darurat -- hapus nomornya, device harus pilih kontak lain."""
    db = init_firebase()
    if db is None:
        return
    db.collection(DEVICES_COLLECTION).document(device_id).set(
        {"contact_number": None, "contact_confirmed": False}, merge=True
    )


def find_device_ids_by_contact_number(contact_number: str) -> list[str]:
    """Cari device mana aja yang lagi nunggu konfirmasi dari nomor ini (dipanggil dari webhook Twilio)."""
    db = init_firebase()
    if db is None:
        return []
    query = db.collection(DEVICES_COLLECTION).where("contact_number", "==", contact_number)
    return [doc.id for doc in query.stream()]


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
