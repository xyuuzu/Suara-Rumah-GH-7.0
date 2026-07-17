"""
Kirim alert WhatsApp lewat Twilio (WhatsApp/SMS Sandbox) pas sistem
mendeteksi indikasi KDRT / pola eskalatif -- sesuai spec produk (Suara
Rumah, bagian 4.7: Alert/Notifikasi = Twilio API).

SETUP:
1. Daftar/login di https://www.twilio.com/, aktifkan WhatsApp Sandbox
   (Console -> Messaging -> Try it out -> Send a WhatsApp message)
2. Ambil Account SID & Auth Token dari Twilio Console dashboard
3. Nomor WhatsApp sandbox default: whatsapp:+14155238886 -- di mode
   sandbox, tiap nomor penerima harus join dulu (kirim kode join yang
   ditampilkan Twilio Console ke nomor sandbox itu dari WA masing2)
   sebelum bisa nerima pesan
4. Isi di .env: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN,
   TWILIO_WHATSAPP_FROM (opsional, default ke nomor sandbox),
   TWILIO_TARGET (nomor WA tujuan fallback, format +628xxxxxxxxxx)

Kalau kredensial belum diisi, fungsi ini cuma warning & skip (ga bikin
API error).
"""

import os

from dotenv import load_dotenv
from twilio.rest import Client

load_dotenv()

DEFAULT_SANDBOX_FROM = "whatsapp:+14155238886"


def _as_whatsapp_number(number: str) -> str:
    number = number.strip()
    return number if number.startswith("whatsapp:") else f"whatsapp:{number}"


def send_whatsapp_alert(message: str, target: str | None = None, media_url: str | None = None) -> bool:
    """
    Kirim 1 pesan WA lewat Twilio. Return True kalau sukses, False kalau di-skip/gagal.

    `target` opsional -- kalau ga dikasih (atau None), fallback ke TWILIO_TARGET
    di .env. Dipakai buat kirim ke kontak darurat spesifik per device.

    `media_url` opsional -- URL PUBLIK (harus bisa diakses dari internet,
    Twilio yang fetch dari situ) buat lampirin file (mis. audio bukti
    kekerasan) ke pesan WA. Lihat app/services/demo_audio.py buat cara
    bikin URL ini.

    CATATAN: WhatsApp GA NAMPILIN caption buat pesan tipe audio/voice note
    (beda dari gambar/video/dokumen) -- jadi pesan audio di sini SENGAJA
    dikirim TANPA body/caption. Kalau mau nyantumin keterangan soal audio-
    nya, taruh di `message` (pesan teks utama), bukan coba nempel ke pesan
    audio -- udah kebukti ga muncul pas dites beneran di WhatsApp.
    """
    account_sid = os.getenv("TWILIO_ACCOUNT_SID")
    auth_token = os.getenv("TWILIO_AUTH_TOKEN")
    from_number = os.getenv("TWILIO_WHATSAPP_FROM", DEFAULT_SANDBOX_FROM)
    target = target or os.getenv("TWILIO_TARGET")

    if not account_sid or not auth_token:
        print("[WARN] Twilio credential belum diisi, alert WA dilewati.")
        return False

    if not target:
        print("[WARN] TWILIO_TARGET belum diisi, alert WA dilewati.")
        return False

    try:
        client = Client(account_sid, auth_token)
        from_wa = _as_whatsapp_number(from_number)
        to_wa = _as_whatsapp_number(target)

        # Teks & audio SENGAJA dikirim sebagai 2 pesan terpisah, bukan digabung jadi
        # 1 pesan (body + media_url). WhatsApp ngebatesin panjang CAPTION media jauh
        # lebih pendek (~1024 karakter) dibanding pesan teks biasa -- kalau digabung,
        # teks yang agak panjang bisa ke-drop DIAM-DIAM (Twilio tetap lapor sukses,
        # tapi cuma audio yang nyampe di WhatsApp, teksnya ilang). Kejadian nyata,
        # udah kena sendiri pas testing.
        client.messages.create(from_=from_wa, to=to_wa, body=message)

        if media_url:
            client.messages.create(from_=from_wa, to=to_wa, media_url=[media_url])

        suffix = " (dengan lampiran audio, pesan terpisah)" if media_url else ""
        print(f"[INFO] Alert WA terkirim ke {target}{suffix}.")
        return True
    except Exception as e:
        print(f"[WARN] Gagal kirim alert WA lewat Twilio: {e}")
        return False
