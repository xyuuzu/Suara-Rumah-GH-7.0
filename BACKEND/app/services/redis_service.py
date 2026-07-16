"""
Koneksi ke Redis, dipakai untuk 2 hal:

1. CACHE hasil prediksi (audio & teks) berdasarkan hash konten.
   Kalau file suara / teks yang sama pernah diproses, langsung ambil dari
   cache -> tidak perlu ekstraksi fitur (librosa) atau inference ulang.
   Ini yang dimaksud "filtering data frekuensi suara & teks" di level infra:
   Redis menyaring request duplikat sebelum masuk ke pipeline ML yang berat.

2. KEYWORD FILTER cepat untuk teks (Redis Set).
   Daftar kata/frasa indikasi kekerasan disimpan di Redis Set supaya bisa
   dicek dengan SISMEMBER (O(1)) sebelum/di samping hasil model ML -> jadi
   lapisan pengaman tambahan kalau model belum yakin tapi ada kata eksplisit.

SETUP:
- Windows: gampangnya pakai Docker `docker run -p 6379:6379 redis` atau
  install Memurai (build Redis buat Windows). Bisa juga pakai Redis Cloud
  (gratis untuk dev) dan isi host/port/password di .env.
- Set di .env: REDIS_HOST, REDIS_PORT, REDIS_DB, REDIS_PASSWORD (opsional)
"""

import hashlib
import json
import os

import redis
from dotenv import load_dotenv

load_dotenv()

_client = None

KEYWORD_SET_KEY = "kdrt:keywords"
CACHE_PREFIX = "kdrt:cache:"
DEFAULT_TTL_SECONDS = 60 * 60 * 24  # 1 hari

# Starter list kata/frasa indikasi kekerasan verbal (Bahasa Indonesia).
# Ini cuma modal awal -> harus terus diperkaya sesuai temuan riil.
DEFAULT_KEYWORDS = [
    "bunuh kamu", "gua bunuh", "aku bunuh", "kubunuh",
    "pukul kamu", "gua pukul", "aku pukul kamu",
    "diam kamu bodoh", "dasar bodoh", "goblok kamu",
    "awas kalau", "jangan macam-macam", "kamu tidak berguna",
    "tidak berguna kamu", "menyesal kamu", "hancurkan kamu",
    "aku hajar", "gua hajar", "tampar kamu", "cekik kamu",
    "keluar dari rumah ini", "berani sama aku", "kamu pantas dipukul",
]


def get_redis_client():
    """Ambil (atau bikin) koneksi Redis singleton. Return None kalau gagal connect."""
    global _client
    if _client is not None:
        return _client

    try:
        client = redis.Redis(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=int(os.getenv("REDIS_PORT", 6379)),
            db=int(os.getenv("REDIS_DB", 0)),
            password=os.getenv("REDIS_PASSWORD") or None,
            decode_responses=True,
            socket_connect_timeout=2,
        )
        client.ping()
        _client = client
        print("[INFO] Redis berhasil terkoneksi.")
        return _client
    except Exception as e:
        print(f"[WARN] Redis tidak tersedia ({e}). Caching & keyword filter dilewati.")
        return None


def _hash_content(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def get_cached_result(content: bytes, namespace: str) -> dict | None:
    """Cek apakah konten (bytes audio, atau teks yg di-encode) sudah pernah diproses."""
    client = get_redis_client()
    if client is None:
        return None

    key = f"{CACHE_PREFIX}{namespace}:{_hash_content(content)}"
    cached = client.get(key)
    return json.loads(cached) if cached else None


def set_cached_result(content: bytes, namespace: str, result: dict, ttl: int = DEFAULT_TTL_SECONDS) -> None:
    client = get_redis_client()
    if client is None:
        return

    key = f"{CACHE_PREFIX}{namespace}:{_hash_content(content)}"
    client.setex(key, ttl, json.dumps(result))


def seed_default_keywords() -> None:
    """Isi Redis Set dengan starter keyword kalau masih kosong. Aman dipanggil berkali-kali."""
    client = get_redis_client()
    if client is None:
        return

    if client.scard(KEYWORD_SET_KEY) == 0:
        client.sadd(KEYWORD_SET_KEY, *DEFAULT_KEYWORDS)
        print(f"[INFO] Seed {len(DEFAULT_KEYWORDS)} keyword kekerasan ke Redis.")


def contains_violence_keyword(text: str) -> list[str]:
    """
    Cek apakah teks mengandung salah satu keyword kekerasan di Redis Set.
    Return list keyword yang match (bisa lebih dari satu), kosong kalau tidak ada.
    Kalau Redis tidak tersedia, return list kosong (fallback ke hasil model ML saja).
    """
    client = get_redis_client()
    if client is None:
        return []

    text_lower = text.lower()
    keywords = client.smembers(KEYWORD_SET_KEY)
    return [kw for kw in keywords if kw in text_lower]
