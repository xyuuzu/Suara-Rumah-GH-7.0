import os
import joblib
import numpy as np
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.services.feature_extraction import extract_features_from_bytes
from app.services.text_detection import preprocess_text
from app.services import redis_service, device_service, window_tracking
from app.services.twilio_service import send_whatsapp_alert
from app.services.firebase_service import save_detection_result, get_alerts_for_device, mark_alert_safe, get_alert_by_id

app = FastAPI(title="KDRT Voice & Text Detector API")

ALERT_CONFIDENCE_THRESHOLD = 0.7
SOS_LABEL = "darurat_sos"

# biar bisa diakses dari app mobile/frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # ganti ke domain spesifik pas udah production
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "ml", "trained_model", "model.pkl")
TEXT_MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "ml", "trained_model", "text_model.pkl")
FEATURE_MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "ml", "trained_model", "feature_model.pkl")

model_bundle = None
text_model_bundle = None
feature_model_bundle = None


class TextInput(BaseModel):
    text: str
    user_id: str | None = None

    model_config = {
        "json_schema_extra": {
            "example": {"text": "aku bunuh kamu kalau macam-macam", "user_id": "test-user"}
        }
    }


class FeatureInput(BaseModel):
    """Fitur numerik ringan yang diekstrak DI DEVICE (Kotlin), bukan audio mentah."""
    device_id: str
    rms: float
    zcr: float
    peak: float
    latitude: float | None = None
    longitude: float | None = None

    model_config = {
        "json_schema_extra": {
            # sample ini persis nyontek angka di panel "Data Terakhir" prototype -- peak tinggi
            # + RMS/ZCR rendah = pola transient khas "bantingan" (benda dibanting/pecah)
            "example": {
                "device_id": "test-device",
                "rms": 0.041,
                "zcr": 0.054,
                "peak": 1.0,
                "latitude": -0.502106,
                "longitude": 117.155594,
            }
        }
    }


class ContactInput(BaseModel):
    contact_number: str

    model_config = {"json_schema_extra": {"example": {"contact_number": "628123456789"}}}


class TestAlertInput(BaseModel):
    message: str = "Test alert dari Suara Rumah -- kalau kamu terima ini, integrasi WhatsApp jalan."


@app.on_event("startup")
def load_model():
    global model_bundle, text_model_bundle, feature_model_bundle

    if os.path.exists(MODEL_PATH):
        model_bundle = joblib.load(MODEL_PATH)
        print("[INFO] Model suara berhasil di-load.")
    else:
        print("[WARN] Model suara belum ada. Jalankan ml/train_model.py dulu.")

    if os.path.exists(TEXT_MODEL_PATH):
        text_model_bundle = joblib.load(TEXT_MODEL_PATH)
        print("[INFO] Model teks berhasil di-load.")
    else:
        print("[WARN] Model teks belum ada. Jalankan ml/train_text_model.py dulu.")

    if os.path.exists(FEATURE_MODEL_PATH):
        feature_model_bundle = joblib.load(FEATURE_MODEL_PATH)
        print("[INFO] Model fitur (RMS/ZCR/Peak) berhasil di-load.")
    else:
        print("[WARN] Model fitur belum ada. Jalankan ml/train_feature_model.py dulu.")

    redis_service.seed_default_keywords()


@app.get("/")
def root():
    return {"status": "ok", "message": "KDRT Voice & Text Detector API jalan"}


def maybe_send_kdrt_alert(result: dict, source: str) -> None:
    """Kirim alert WA lewat Twilio kalau prediksi kdrt dengan confidence tinggi."""
    if result["prediction"] == "kdrt" and result["confidence"] >= ALERT_CONFIDENCE_THRESHOLD:
        percent = round(result["confidence"] * 100, 1)
        send_whatsapp_alert(
            f"⚠️ Terdeteksi indikasi KDRT dari {source} (confidence: {percent}%). "
            f"Segera cek kondisi korban."
        )


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    if model_bundle is None:
        raise HTTPException(status_code=503, detail="Model belum di-load / belum ditraining.")

    if not file.filename.lower().endswith((".wav", ".mp3")):
        raise HTTPException(status_code=400, detail="Format file harus .wav atau .mp3")

    audio_bytes = await file.read()

    cached = redis_service.get_cached_result(audio_bytes, namespace="audio")
    if cached is not None:
        return {**cached, "cached": True}

    try:
        features = extract_features_from_bytes(audio_bytes)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Gagal proses audio: {e}")

    scaler = model_bundle["scaler"]
    model = model_bundle["model"]

    features_scaled = scaler.transform([features])
    prediction = model.predict(features_scaled)[0]
    probabilities = model.predict_proba(features_scaled)[0]

    classes = model.classes_
    prob_dict = {cls: float(prob) for cls, prob in zip(classes, probabilities)}

    result = {
        "prediction": prediction,
        "confidence": max(prob_dict.values()),
        "probabilities": prob_dict,
    }
    redis_service.set_cached_result(audio_bytes, namespace="audio", result=result)
    maybe_send_kdrt_alert(result, source="suara")
    if result["prediction"] == "kdrt":
        save_detection_result(user_id="anonymous", prediction=result["prediction"], confidence=result["confidence"])

    return {**result, "cached": False}


@app.post("/predict-text")
async def predict_text(payload: TextInput):
    if text_model_bundle is None:
        raise HTTPException(status_code=503, detail="Model teks belum di-load / belum ditraining.")

    text_bytes = payload.text.encode("utf-8")

    cached = redis_service.get_cached_result(text_bytes, namespace="text")
    if cached is not None:
        return {**cached, "cached": True}

    matched_keywords = redis_service.contains_violence_keyword(payload.text)

    clean_text = preprocess_text(payload.text)
    vectorizer = text_model_bundle["vectorizer"]
    model = text_model_bundle["model"]

    features = vectorizer.transform([clean_text])
    prediction = model.predict(features)[0]
    probabilities = model.predict_proba(features)[0]

    classes = model.classes_
    prob_dict = {cls: float(prob) for cls, prob in zip(classes, probabilities)}

    result = {
        "prediction": prediction,
        "confidence": max(prob_dict.values()),
        "probabilities": prob_dict,
        "keyword_flag": len(matched_keywords) > 0,
        "matched_keywords": matched_keywords,
    }
    redis_service.set_cached_result(text_bytes, namespace="text", result=result)
    maybe_send_kdrt_alert(result, source="teks")
    if result["prediction"] == "kdrt":
        save_detection_result(
            user_id=payload.user_id or "anonymous", prediction=result["prediction"], confidence=result["confidence"]
        )

    return {**result, "cached": False}


# ---------------------------------------------------------------------------
# Endpoint di bawah ini adalah surface resmi buat integrasi mobile (Suara
# Rumah) sesuai spec produk: mobile ekstrak RMS/ZCR/Peak di device (Kotlin),
# backend cuma klasifikasi + window tracking + trigger alert. Beda dari
# /predict di atas (yang masih terima upload audio mentah untuk testing
# backend independen dari mobile).
# ---------------------------------------------------------------------------


@app.post("/devices/register")
def register_device():
    """Dipanggil sekali pas setup awal app. Mobile simpan device_id+api_key di Room DB."""
    return device_service.register_device()


@app.post("/devices/{device_id}/contact")
def set_contact(device_id: str, payload: ContactInput):
    device_service.set_emergency_contact(device_id, payload.contact_number)
    return {"status": "ok"}


@app.get("/devices/{device_id}/contact")
def get_contact(device_id: str):
    return {"contact_number": device_service.get_emergency_contact(device_id)}


@app.post("/devices/{device_id}/test-alert")
def test_alert(device_id: str, payload: TestAlertInput):
    """
    Kirim WA test ke kontak darurat device ini -- buat verifikasi integrasi
    Twilio jalan tanpa perlu trigger eskalasi beneran lewat /predict-features
    2x. Nomor tujuan SELALU diambil dari kontak darurat yang di-set device ini
    (POST /devices/{device_id}/contact), BUKAN nomor tetap dari .env -- jadi
    tiap device bisa punya kontak darurat masing-masing yang beda-beda.
    """
    target = device_service.get_emergency_contact(device_id)
    if not target:
        raise HTTPException(
            status_code=400,
            detail="Device ini belum punya kontak darurat. Set dulu lewat POST /devices/{device_id}/contact.",
        )

    sent = send_whatsapp_alert(payload.message, target=target)
    return {"sent": sent, "target": target}


@app.get("/devices/{device_id}/latest")
def get_latest(device_id: str):
    """Data terakhir yang diproses buat device ini -- buat panel 'Data Terakhir' di dashboard."""
    data = device_service.get_latest_reading(device_id)
    if data is None:
        raise HTTPException(status_code=404, detail="Belum ada data buat device ini.")
    return data


@app.get("/devices/{device_id}/alerts")
def get_alerts(device_id: str, limit: int = 20):
    """Histori alert buat device ini -- buat panel 'Histori Alert' di dashboard."""
    return get_alerts_for_device(device_id, limit=limit)


@app.post("/predict-features")
def predict_features(payload: FeatureInput):
    """
    Endpoint utama yang dipanggil berulang kali sama mobile app (tiap kali ada
    rolling buffer audio baru diproses jadi RMS/ZCR/Peak di device). Klasifikasi
    pakai model scikit-learn yang ditraining di ml/train_feature_model.py
    (input: rms/zcr/peak -> label: normal/bantingan/teriakan/darurat_sos).
    """
    if feature_model_bundle is None:
        raise HTTPException(status_code=503, detail="Model fitur belum di-load / belum ditraining.")

    device_id = payload.device_id
    scaler = feature_model_bundle["scaler"]
    model = feature_model_bundle["model"]

    features_scaled = scaler.transform([[payload.rms, payload.zcr, payload.peak]])
    label = model.predict(features_scaled)[0]
    probabilities = model.predict_proba(features_scaled)[0]
    prob_dict = {cls: float(p) for cls, p in zip(model.classes_, probabilities)}
    confidence = max(prob_dict.values())

    is_anomaly = label != "normal"
    device_service.save_latest_reading(device_id, payload.rms, payload.zcr, payload.peak, label)

    anomaly_count = 0
    escalating = False
    alert_triggered = False
    alert_id = ""

    if is_anomaly:
        if label == SOS_LABEL:
            # Darurat SOS = alert LANGSUNG, ga perlu nunggu pola berulang kayak bantingan/teriakan biasa.
            escalating = True
        else:
            anomaly_count = window_tracking.record_anomaly(device_id)
            escalating = anomaly_count >= window_tracking.ESCALATION_THRESHOLD

        if escalating:
            message = (
                f"⚠️ Suara Rumah mendeteksi \"{label}\" (confidence: {round(confidence * 100, 1)}%). "
                f"Segera cek kondisi korban."
            )
            if payload.latitude is not None and payload.longitude is not None:
                message += f" Lokasi: https://maps.google.com/?q={payload.latitude},{payload.longitude}"

            target = device_service.get_emergency_contact(device_id)
            alert_triggered = send_whatsapp_alert(message, target=target)

            saved = save_detection_result(
                user_id=device_id,
                prediction=label,
                confidence=confidence,
                extra={
                    "rms": payload.rms,
                    "zcr": payload.zcr,
                    "peak": payload.peak,
                    "latitude": payload.latitude,
                    "longitude": payload.longitude,
                    "alert_sent": alert_triggered,
                    "type": "alert",
                },
            )
            if saved is not None:
                alert_id = saved[1].id

    return {
        "label": label,
        "status": label.upper(),
        "confidence": confidence,
        "probabilities": prob_dict,
        "anomaly": is_anomaly,
        "sos": label == SOS_LABEL,
        "anomaly_count_in_window": anomaly_count,
        "escalating": escalating,
        "alert_triggered": alert_triggered,
        "alert_id": alert_id,
        "rms": payload.rms,
        "zcr": payload.zcr,
        "peak": payload.peak,
    }


@app.post("/alerts/{alert_id}/confirm-safe")
def confirm_safe(alert_id: str):
    """Dipanggil pas user buka app & tap 'Aman' setelah alert terlanjur terkirim (follow-up message)."""
    alert = get_alert_by_id(alert_id)
    target = device_service.get_emergency_contact(alert["user_id"]) if alert else None
    send_whatsapp_alert("Update: pengguna menandai kondisi ini sebagai AMAN (alarm palsu).", target=target)
    mark_alert_safe(alert_id)
    return {"status": "ok"}
