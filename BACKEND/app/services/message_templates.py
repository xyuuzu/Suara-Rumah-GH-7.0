"""
Template pesan alert WhatsApp yang dikirim ke kontak darurat -- format
"actionable, human-readable, terstruktur" biar penerima langsung tau harus
ngapain saat kondisi darurat sungguhan, bukan cuma notifikasi mentah berisi
label/confidence teknis.

Nama korban di-random dari daftar dummy (belum ada data profil user asli
di project ini, cuma device_id) -- KHUSUS BUAT DEMO. Ganti ke nama asli
begitu ada data profil user yang bisa diambil (misal field "name" di
collection devices/{device_id} pas user isi nama sendiri di app).
Nama-nya konsisten per device_id (bukan acak tiap kali alert kekirim),
biar ga aneh liat nama "korban" beda-beda tiap alert dari device yang sama
pas demo.
"""

from datetime import datetime, timedelta, timezone

DUMMY_NAMES = [
    "Siti", "Dewi", "Rina", "Ayu", "Putri", "Sari", "Wulan", "Fitri", "Indah", "Lestari",
]

BULAN_ID = {
    1: "Januari", 2: "Februari", 3: "Maret", 4: "April", 5: "Mei", 6: "Juni",
    7: "Juli", 8: "Agustus", 9: "September", 10: "Oktober", 11: "November", 12: "Desember",
}

WIB = timezone(timedelta(hours=7))


def _pick_dummy_name(device_id: str) -> str:
    idx = sum(ord(c) for c in device_id) % len(DUMMY_NAMES)
    return DUMMY_NAMES[idx]


def build_emergency_message(device_id: str, latitude: float | None = None, longitude: float | None = None) -> str:
    """
    Bikin pesan darurat lengkap: konteks, lokasi, langkah disarankan, hotline,
    waktu kejadian. Nadanya sengaja dibuat halus/ga langsung mengejutkan --
    dibuka dengan sapaan & konteks dulu sebelum masuk ke detail deteksi,
    biar penerima ga kaget/panik duluan sebelum ngerti apa yang terjadi.
    """
    nama = _pick_dummy_name(device_id)
    now = datetime.now(WIB)
    waktu = f"{now.hour:02d}:{now.minute:02d}, {now.day} {BULAN_ID[now.month]} {now.year}"

    lokasi_line = (
        f"📍 Lokasi: https://maps.google.com/?q={latitude},{longitude}"
        if latitude is not None and longitude is not None
        else "📍 Lokasi: tidak tersedia"
    )

    return (
        "Halo 👋 Ini pesan otomatis dari Suara Rumah.\n\n"
        f"{nama} sempat menandai kamu sebagai kontak darurat mereka. Barusan sistem mendeteksi "
        f"pola suara yang agak mencurigakan di sekitar {nama} -- mungkin ga apa-apa, tapi kami mau "
        "pastiin dulu semuanya baik-baik aja.\n\n"
        f"{lokasi_line}\n\n"
        "Kalau berkenan, ini yang bisa kamu lakukan:\n"
        f"1. Hubungi atau kunjungi {nama} sekarang — pilih salah satu\n"
        "2. Jika tidak ada respons, coba cara satunya (hubungi kalau tadi kunjungi, atau sebaliknya)\n"
        "3. Jika situasi terlihat berbahaya, hubungi:\n"
        "   • Polisi: 110\n"
        "   • Hotline KemenPPPA: 129\n\n"
        f"Waktu kejadian: {waktu}\n\n"
        f"Bantuanmu berarti banget buat {nama}. Terima kasih 🙏"
    )


def build_follow_up_safe_message(device_id: str) -> str:
    """Pesan follow-up pas user tap 'Aman' setelah alert terlanjur terkirim."""
    nama = _pick_dummy_name(device_id)
    return f"✅ SUARA RUMAH — Update\n\n{nama} menandai kondisi ini sebagai AMAN (alarm palsu). Terima kasih atas perhatiannya."


def build_contact_confirmation_request(device_id: str) -> str:
    """Dikirim ke nomor yang BARU di-set sebagai kontak darurat -- minta persetujuan opt-in dulu
    sebelum nomor itu beneran mulai bisa nerima alert asli."""
    nama = _pick_dummy_name(device_id)
    return (
        "Halo 👋 Ini pesan otomatis dari Suara Rumah.\n\n"
        f"{nama} baru aja menambahkan nomor kamu sebagai kontak darurat di aplikasi ini. "
        f"Kalau nanti sistem mendeteksi sesuatu yang mencurigakan, kamu bakal dapat notifikasi "
        f"buat bantu cek kondisi {nama}.\n\n"
        f"Kamu bersedia jadi kontak darurat buat {nama}?\n\n"
        "Balas *IYA* untuk konfirmasi, atau *TIDAK* kalau belum bisa."
    )


def build_contact_confirmed_reply(device_id: str) -> str:
    nama = _pick_dummy_name(device_id)
    return f"Terima kasih sudah konfirmasi 🙏 Kamu sekarang jadi kontak darurat buat {nama} di Suara Rumah."


def build_contact_declined_reply(device_id: str) -> str:
    nama = _pick_dummy_name(device_id)
    return f"Baik, kamu ga akan kami jadikan kontak darurat {nama}. Terima kasih sudah kasih tau."


def build_contact_unclear_reply() -> str:
    return "Maaf, kami belum ngerti balasannya. Balas *IYA* kalau bersedia jadi kontak darurat, atau *TIDAK* kalau belum bisa."
