"""
Modul ini yang mengubah file suara (.wav/.mp3) jadi angka-angka (numerical features)
yang nantinya dipakai buat training atau prediksi di model scikit-learn.

Fitur yang umum dipakai buat deteksi emosi/karakteristik suara:
- MFCC (Mel-Frequency Cepstral Coefficients) -> menangkap karakter timbre suara
- Chroma -> menangkap info pitch/nada
- Mel Spectrogram -> representasi frekuensi
- Zero Crossing Rate -> menangkap "kekasaran"/noisiness suara
- RMS Energy -> menangkap kekerasan/volume suara (penting buat deteksi teriakan)
- Pitch (F0) -> nada dasar suara, biasanya naik saat marah/panik
"""

import librosa
import numpy as np


def extract_features(file_path: str, sr: int = 22050) -> np.ndarray:
    """
    Ambil 1 file audio, keluarkan 1 array angka (feature vector).

    Args:
        file_path: path ke file audio (.wav, .mp3, dll)
        sr: sample rate, default 22050 Hz (standar librosa)

    Returns:
        np.ndarray 1 dimensi berisi gabungan semua fitur (rata-rata per fitur)
    """
    # load audio, otomatis di-resample ke `sr`
    y, sr = librosa.load(file_path, sr=sr)

    # buang bagian hening di awal/akhir biar fitur lebih fokus ke suara aktualnya
    y, _ = librosa.effects.trim(y)

    features = []

    # 1. MFCC (13 koefisien -> diambil rata-rata & std tiap koefisien)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
    features.extend(np.mean(mfcc, axis=1))
    features.extend(np.std(mfcc, axis=1))

    # 2. Chroma
    chroma = librosa.feature.chroma_stft(y=y, sr=sr)
    features.extend(np.mean(chroma, axis=1))

    # 3. Mel Spectrogram
    mel = librosa.feature.melspectrogram(y=y, sr=sr)
    features.extend(np.mean(mel, axis=1))

    # 4. Zero Crossing Rate
    zcr = librosa.feature.zero_crossing_rate(y)
    features.append(np.mean(zcr))

    # 5. RMS Energy (kekerasan suara)
    rms = librosa.feature.rms(y=y)
    features.append(np.mean(rms))
    features.append(np.max(rms))  # puncak energi, berguna buat deteksi teriakan

    # 6. Pitch (F0) pakai pyin, dengan fallback kalau gagal deteksi pitch
    try:
        f0, voiced_flag, _ = librosa.pyin(
            y, fmin=librosa.note_to_hz("C2"), fmax=librosa.note_to_hz("C7")
        )
        f0_clean = f0[~np.isnan(f0)]
        features.append(np.mean(f0_clean) if len(f0_clean) > 0 else 0.0)
        features.append(np.std(f0_clean) if len(f0_clean) > 0 else 0.0)
    except Exception:
        features.append(0.0)
        features.append(0.0)

    return np.array(features)


def extract_features_from_bytes(audio_bytes: bytes, sr: int = 22050) -> np.ndarray:
    """
    Versi extract_features tapi input-nya bytes (buat file yang di-upload lewat API,
    jadi ga perlu disimpan ke disk dulu).
    """
    import io
    y, sr = librosa.load(io.BytesIO(audio_bytes), sr=sr)
    y, _ = librosa.effects.trim(y)

    features = []
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
    features.extend(np.mean(mfcc, axis=1))
    features.extend(np.std(mfcc, axis=1))

    chroma = librosa.feature.chroma_stft(y=y, sr=sr)
    features.extend(np.mean(chroma, axis=1))

    mel = librosa.feature.melspectrogram(y=y, sr=sr)
    features.extend(np.mean(mel, axis=1))

    zcr = librosa.feature.zero_crossing_rate(y)
    features.append(np.mean(zcr))

    rms = librosa.feature.rms(y=y)
    features.append(np.mean(rms))
    features.append(np.max(rms))

    try:
        f0, voiced_flag, _ = librosa.pyin(
            y, fmin=librosa.note_to_hz("C2"), fmax=librosa.note_to_hz("C7")
        )
        f0_clean = f0[~np.isnan(f0)]
        features.append(np.mean(f0_clean) if len(f0_clean) > 0 else 0.0)
        features.append(np.std(f0_clean) if len(f0_clean) > 0 else 0.0)
    except Exception:
        features.append(0.0)
        features.append(0.0)

    return np.array(features)
