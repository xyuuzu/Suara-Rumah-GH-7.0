"""
Suara Rumah — Backend FastAPI
=============================
Klasifikasi fitur audio dan pipeline alert darurat.

Arsitektur (dari PRD):
- Klasifikasi via threshold rule (MVP, tanpa model ML berat)
- Sliding-window tracker anomali in-memory (dictionary per user-id)
- Alert via Twilio API (SMS/WhatsApp)
- Batasan MVP: in-memory state, bukan Redis — cukup untuk skala demo

Jalankan:
    pip install fastapi uvicorn twilio
    uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

from fastapi import FastAPI, Header, HTTPException, Depends
from pydantic import BaseModel, ConfigDict, Field, AliasChoices
from typing import Optional
from datetime import datetime
import time
import os

app = FastAPI(
    title="Suara Rumah API",
    description="Backend untuk deteksi anomali akustik kekerasan domestik",
    version="2.0.0"
)

# ── Konfigurasi ──
API_KEY = "suara-rumah-mvp-key-2024"
TWILIO_ACCOUNT_SID = os.getenv("TWILIO_ACCOUNT_SID", "")
TWILIO_AUTH_TOKEN = os.getenv("TWILIO_AUTH_TOKEN", "")
TWILIO_FROM_NUMBER = os.getenv("TWILIO_FROM_NUMBER", "")

# ── Threshold Rules untuk Klasifikasi (MVP) ──
# Nilai-nilai ini dikalibrasi secara manual dari clip simulasi
THRESHOLDS = {
    "rms_high": 0.35,           # Suara keras (teriakan)
    "rms_very_high": 0.55,      # Suara sangat keras (benda pecah)
    "zcr_scream": 0.15,         # ZCR tinggi = teriakan
    "peak_impulse": 0.70,       # Peak amplitude tinggi = suara impulsif
    "escalation_window": 5,     # Jumlah data points dalam sliding window
    "escalation_threshold": 3,  # Minimal anomali untuk dianggap eskalatif
}


# ══════════════════════════════════════════════
#  Data Models (Pydantic)
# ══════════════════════════════════════════════

class AudioFeatureRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    rms: float
    zcr: float
    peak_amplitude: float = Field(
        validation_alias=AliasChoices("peak", "peakAmplitude"),
        serialization_alias="peak",
    )
    timestamp: int
    user_id: str = Field(
        validation_alias=AliasChoices("user_id", "userId"),
        serialization_alias="user_id",
    )
    latitude: Optional[float] = None
    longitude: Optional[float] = None

class ClassificationResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    is_anomaly: bool = Field(serialization_alias="is_anomaly")
    confidence: float
    label: str
    is_escalating: bool = Field(default=False, serialization_alias="escalating")
    consecutive_anomalies: int = Field(default=0, serialization_alias="consecutive_anomalies")
    alert_id: Optional[str] = Field(default=None, serialization_alias="alert_id")
    message: Optional[str] = None

class AlertRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    user_id: str = Field(
        validation_alias=AliasChoices("user_id", "userId"),
        serialization_alias="user_id",
    )
    contactNumbers: list[str]
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    timestamp: int
    alertType: str  # "emergency", "false_alarm_followup", "cancel"

class ApiResponse(BaseModel):
    success: bool
    message: str
    alertId: Optional[str] = None


# ══════════════════════════════════════════════
#  In-Memory State (MVP — ganti Redis untuk produksi)
# ══════════════════════════════════════════════

# Sliding window tracker per user: { userId: [list of recent classifications] }
anomaly_tracker: dict[str, list[dict]] = {}

# Alert state per user: { userId: { "active": bool, "alertId": str, ... } }
alert_state: dict[str, dict] = {}


# ══════════════════════════════════════════════
#  Auth Dependency
# ══════════════════════════════════════════════

async def verify_api_key(x_api_key: str = Header(...)):
    """Autentikasi minimal — API key statis per-device."""
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return x_api_key


# ══════════════════════════════════════════════
#  Klasifikasi Logic
# ══════════════════════════════════════════════

def classify_audio(features: AudioFeatureRequest) -> ClassificationResponse:
    """
    Klasifikasi berbasis threshold rule.
    
    Logic:
    1. Cek peak amplitude → suara impulsif (benda pecah)
    2. Cek RMS + ZCR → teriakan berkepanjangan
    3. Cek sliding window → eskalasi
    """
    is_anomaly = False
    confidence = 0.0
    label = "normal"
    
    # Rule 1: Suara impulsif (benda pecah/jatuh)
    if features.peak_amplitude >= THRESHOLDS["peak_impulse"]:
        is_anomaly = True
        confidence = min(features.peak_amplitude, 1.0)
        label = "crash"
    
    # Rule 2: Teriakan (RMS tinggi + ZCR tinggi)
    elif (features.rms >= THRESHOLDS["rms_high"] and 
          features.zcr >= THRESHOLDS["zcr_scream"]):
        is_anomaly = True
        confidence = min((features.rms + features.zcr) / 2, 1.0)
        label = "scream"
    
    # Rule 3: Suara sangat keras (mungkin gabungan)
    elif features.rms >= THRESHOLDS["rms_very_high"]:
        is_anomaly = True
        confidence = min(features.rms, 1.0)
        label = "loud_noise"
    
    # Normal
    else:
        confidence = 1.0 - features.rms  # Confidence "normal" tinggi kalau RMS rendah
        label = "normal"
    
    # ── Sliding Window Tracker ──
    user_id = features.user_id
    if user_id not in anomaly_tracker:
        anomaly_tracker[user_id] = []
    
    # Tambah ke window
    anomaly_tracker[user_id].append({
        "is_anomaly": is_anomaly,
        "label": label,
        "timestamp": features.timestamp
    })
    
    # Pertahankan hanya N entries terakhir
    window_size = THRESHOLDS["escalation_window"]
    if len(anomaly_tracker[user_id]) > window_size:
        anomaly_tracker[user_id] = anomaly_tracker[user_id][-window_size:]
    
    # Hitung anomali berturut-turut
    consecutive = 0
    for entry in reversed(anomaly_tracker[user_id]):
        if entry["is_anomaly"]:
            consecutive += 1
        else:
            break
    
    is_escalating = consecutive >= THRESHOLDS["escalation_threshold"]
    
    return ClassificationResponse(
        is_anomaly=is_anomaly,
        confidence=round(confidence, 3),
        label=label,
        is_escalating=is_escalating,
        consecutive_anomalies=consecutive,
        message=f"Window: {len(anomaly_tracker[user_id])}/{window_size}, "
                f"Consecutive anomalies: {consecutive}"
    )


# ══════════════════════════════════════════════
#  Twilio Integration
# ══════════════════════════════════════════════

def send_twilio_message(to_number: str, body: str) -> bool:
    """Kirim pesan via Twilio. Return True jika berhasil."""
    try:
        if not all([TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER]):
            print(f"[TWILIO STUB] To: {to_number} | Body: {body}")
            return True  # Stub mode — log ke console
        
        from twilio.rest import Client
        client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
        message = client.messages.create(
            body=body,
            from_=TWILIO_FROM_NUMBER,
            to=to_number
        )
        print(f"[TWILIO] Sent to {to_number}: {message.sid}")
        return True
    except Exception as e:
        print(f"[TWILIO ERROR] {e}")
        return False


# ══════════════════════════════════════════════
#  API Endpoints
# ══════════════════════════════════════════════

@app.get("/api/v1/health")
async def health_check():
    """Health check — verifikasi backend bisa dijangkau."""
    return ApiResponse(
        success=True,
        message="Suara Rumah backend is running"
    )


@app.post("/predict", response_model=ClassificationResponse)
@app.post("/predict-features", response_model=ClassificationResponse)
@app.post("/api/v1/analyze", response_model=ClassificationResponse)
async def analyze_audio(
    request: AudioFeatureRequest,
    _: str = Depends(verify_api_key)
):
    """
    Terima fitur audio, jalankan klasifikasi, dan kembalikan hasil.
    Sliding window tracker di-update secara otomatis.
    """
    result = classify_audio(request)
    return result


@app.post("/api/v1/trigger-alert", response_model=ApiResponse)
async def trigger_alert(
    request: AlertRequest,
    _: str = Depends(verify_api_key)
):
    """
    Trigger pengiriman pesan darurat ke semua kontak darurat.
    Dipanggil setelah grace period habis tanpa pembatalan.
    """
    if request.alertType != "emergency":
        raise HTTPException(status_code=400, detail="alertType must be 'emergency'")
    
    alert_id = f"alert-{request.user_id}-{int(time.time())}"
    
    # Compose pesan darurat
    location_str = ""
    if request.latitude is not None and request.longitude is not None:
        location_str = (
            f"\n📍 Lokasi: https://maps.google.com/?q="
            f"{request.latitude},{request.longitude}"
        )
    
    timestamp_str = datetime.fromtimestamp(
        request.timestamp / 1000
    ).strftime("%d/%m/%Y %H:%M:%S")
    
    message_body = (
        f"🚨 PERINGATAN DARURAT — Suara Rumah\n"
        f"Pola suara anomali terdeteksi dan tidak dibatalkan.\n"
        f"⏰ Waktu: {timestamp_str}"
        f"{location_str}\n"
        f"Segera hubungi atau periksa kondisi pengguna."
    )
    
    # Kirim ke semua kontak
    success_count = 0
    for number in request.contactNumbers:
        if send_twilio_message(number, message_body):
            success_count += 1
    
    # Simpan state alert
    alert_state[request.user_id] = {
        "active": True,
        "alertId": alert_id,
        "timestamp": request.timestamp,
        "contactsSent": success_count
    }
    
    return ApiResponse(
        success=success_count > 0,
        message=f"Alert sent to {success_count}/{len(request.contactNumbers)} contacts",
        alertId=alert_id
    )


@app.post("/api/v1/cancel-alert", response_model=ApiResponse)
async def cancel_alert(
    request: AlertRequest,
    _: str = Depends(verify_api_key)
):
    """
    Batalkan alert — dipanggil saat user membatalkan selama grace period.
    """
    if request.user_id in alert_state:
        alert_state[request.user_id]["active"] = False
    
    # Reset sliding window untuk user ini
    if request.user_id in anomaly_tracker:
        anomaly_tracker[request.user_id] = []
    
    return ApiResponse(
        success=True,
        message="Alert cancelled successfully"
    )


@app.post("/api/v1/report-safe", response_model=ApiResponse)
async def report_safe(
    request: AlertRequest,
    _: str = Depends(verify_api_key)
):
    """
    Kirim pesan susulan "kondisi aman" ke kontak darurat.
    Dipanggil saat user menandai alert sebelumnya sebagai alarm palsu.
    """
    if request.alertType != "false_alarm_followup":
        raise HTTPException(
            status_code=400, 
            detail="alertType must be 'false_alarm_followup'"
        )
    
    message_body = (
        "✅ UPDATE — Suara Rumah\n"
        "Pengguna menandai kondisi sudah aman.\n"
        "Ini adalah pembaruan dari peringatan sebelumnya."
    )
    
    success_count = 0
    for number in request.contactNumbers:
        if send_twilio_message(number, message_body):
            success_count += 1
    
    # Reset state
    if request.user_id in alert_state:
        alert_state[request.user_id]["active"] = False
    if request.user_id in anomaly_tracker:
        anomaly_tracker[request.user_id] = []
    
    return ApiResponse(
        success=success_count > 0,
        message=f"Safe report sent to {success_count}/{len(request.contactNumbers)} contacts"
    )


@app.get("/api/v1/tracker/{user_id}")
async def get_tracker_state(
    user_id: str,
    _: str = Depends(verify_api_key)
):
    """Debug endpoint — lihat sliding window state untuk user tertentu."""
    return {
        "userId": user_id,
        "window": anomaly_tracker.get(user_id, []),
        "alertState": alert_state.get(user_id, {}),
        "windowSize": len(anomaly_tracker.get(user_id, []))
    }
