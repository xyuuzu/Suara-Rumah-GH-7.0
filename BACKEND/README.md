# Suara Rumah — Backend

Backend pendeteksi indikasi KDRT dari **suara** dan **teks**, dibangun dengan FastAPI. Fokus utama: deteksi kekerasan fisik akut sedini mungkin dari pola akustik (bukan isi percakapan), lalu kirim alert WhatsApp otomatis ke kontak darurat yang sudah menyetujui (opt-in).

## Ringkasan

| | |
|---|---|
| **Input dari mobile** | 3 angka per rolling buffer: RMS, Zero-Crossing Rate, Peak Amplitude — **audio mentah tidak pernah dikirim ke server** |
| **Klasifikasi** | scikit-learn (RandomForest) → `normal` / `bantingan` / `teriakan` / `darurat_sos` |
| **Alert** | WhatsApp via Twilio, otomatis ke kontak darurat yang sudah opt-in |
| **Database** | Firebase Firestore — cuma nyimpen hasil klasifikasi & metadata, bukan audio |
| **Cache/window tracking** | Redis (dengan fallback in-memory kalau Redis mati) |

## Fitur Utama

- **Klasifikasi 4 level** — bukan cuma "kdrt/normal" biner: `bantingan` (benda pecah), `teriakan`, `darurat_sos` (kombinasi paling ekstrem). Model ditraining dari gabungan data sintetis (12.000 sampel) + fitur asli hasil ekstraksi dari rekaman KDRT nyata (87 sampel, di-oversample), ~89% akurasi evaluasi.
- **Eskalasi bertingkat** — `darurat_sos` langsung trigger alert di deteksi pertama (sinyal paling eksplisit, tidak perlu nunggu). `bantingan`/`teriakan` butuh 2 deteksi anomali dalam 30 detik dulu, biar tidak gampang false alarm dari 1 kejadian sesaat.
- **Alert WhatsApp yang actionable** — bukan cuma notifikasi mentah, tapi pesan terstruktur: lokasi (link Google Maps), langkah yang disarankan (hubungi/kunjungi korban, jangan sebar ke medsos sebelum ada bukti, kumpulkan bukti, kontak Unit PPA/hotline 110/SAPA 129/LBH APIK/Komnas Perempuan).
- **Lampiran audio bukti** — tiap alert asli otomatis dilampiri 1 klip audio (dari dataset KDRT nyata) supaya notifikasi lebih konkret. *(Klip demo, bukan rekaman live — lihat catatan jujur di bawah.)*
- **Opt-in kontak darurat** — nomor baru yang didaftarkan sebagai kontak darurat **tidak langsung aktif**. Sistem kirim WA minta persetujuan ("Balas IYA/TIDAK"), dan cuma yang setuju yang bisa menerima alert asli — bukan asal kirim ke nomor yang tidak minta.
- **Cache & keyword filter cepat** — Redis cache hasil prediksi (hash SHA-256 konten), plus keyword set kata-kata indikasi kekerasan buat sinyal tambahan di endpoint teks.

## Arsitektur Singkat

```
Mobile (Kotlin) ekstrak RMS/ZCR/Peak di device
        │  POST /predict-features {device_id, rms, zcr, peak}
        ▼
FastAPI klasifikasi (RandomForest) → normal / bantingan / teriakan / darurat_sos
        │
        ├─ darurat_sos?           → alert LANGSUNG
        ├─ bantingan/teriakan?    → window tracking (Redis/in-memory) → alert kalau ≥2x dalam 30 detik
        └─ normal                 → tidak ada aksi
        │
        ▼ (kalau alert)
Twilio kirim 2 pesan WA terpisah: teks (lokasi + langkah disarankan) + audio bukti
        │
        ▼
Firestore simpan hasil (label, confidence, waktu, status) — TANPA audio mentah
```

## Tech Stack

| Layer | Pilihan | Alasan singkat |
|---|---|---|
| API server | FastAPI (Python) | Auto docs (Swagger), validasi request otomatis (Pydantic), 1 bahasa sama pipeline ML |
| Audio→angka | librosa | Standar de facto ekstraksi fitur akustik di Python |
| Klasifikasi | scikit-learn (RandomForest, LogisticRegression) | Cepat ditraining & inference, cukup buat skala fitur/data saat ini, ga perlu deep learning |
| Alert WA | Twilio (WhatsApp Sandbox) | Sesuai spec produk, gampang diintegrasi |
| Database | Firebase Firestore | Managed, cuma simpan hasil terproses (bukan audio) |
| Cache/window | Redis (+fallback in-memory) | Cepat buat data sementara, fallback biar tetap jalan tanpa Redis |

Alasan lebih detail tiap pilihan ada di [`completed.txt`](completed.txt).

## API Endpoints (ringkas)

Detail lengkap request/response tiap endpoint (termasuk contoh nyata hasil testing) ada di [`API_BLUEPRINT.txt`](API_BLUEPRINT.txt) dan [`twilio.json`](twilio.json). Cara testing cepat tanpa ngoding: Swagger UI di `/docs`, atau import [`postman_collection.json`](postman_collection.json).

| Endpoint | Fungsi |
|---|---|
| `POST /predict-features` | Endpoint utama mobile — kirim RMS/ZCR/Peak, dapat klasifikasi + trigger alert |
| `POST /devices/{id}/contact` | Set kontak darurat (kirim WA opt-in ke nomor baru) |
| `POST /devices/{id}/test-alert` | Kirim WA test manual ke kontak darurat (kalau sudah opt-in) |
| `GET /devices/{id}/latest` | Data terakhir device (buat dashboard) |
| `GET /devices/{id}/alerts` | Histori alert device |
| `POST /alerts/{id}/confirm-safe` | Follow-up "kondisi aman" setelah alert |
| `POST /twilio/webhook` | Nangkep balasan IYA/TIDAK dari kontak darurat |
| `POST /predict`, `POST /predict-text` | Endpoint lama, upload audio/teks langsung — buat testing backend independen dari mobile |

## Catatan Jujur — Batasan Saat Ini

Bagian ini sengaja ditulis eksplisit, bukan disembunyikan:

- **Dataset audio real masih sangat kecil** (13 file). Akurasi model gabungan (~89%) menunjukkan model belajar pola dataset saat ini, **bukan** jaminan akurat ke kondisi nyata di lapangan. Prioritas berikutnya: perbanyak data kalibrasi dari device fisik asli.
- **Klip audio yang dilampirkan ke alert adalah klip demo** dari dataset yang sudah ada, **bukan rekaman live dari kejadian yang lagi dideteksi** — arsitektur produk memang sengaja tidak pernah mengirim audio mentah dari mobile ke server (privasi).
- **Belum ada autentikasi API** (device ID + API key sempat diimplementasi, lalu sengaja dilepas untuk mempercepat development) — endpoint saat ini terbuka. Perlu dipasang lagi sebelum dipakai dengan data pengguna asli.
- **Lampiran audio butuh URL publik** (ngrok saat dev, atau domain hasil deploy) — Twilio tidak bisa mengambil file dari `localhost`.
- **Mode Twilio masih Sandbox** — penerima harus "join" sandbox dulu sebelum bisa menerima pesan; untuk produksi perlu upgrade ke WhatsApp Business number resmi.

## Setup untuk Developer

<details>
<summary><b>Klik untuk buka panduan setup lengkap (venv, dataset, training, .env)</b></summary>

### 0. Prasyarat
- **Python 3.13** direkomendasikan (numpy/scipy/librosa terbaru punya wheel prebuilt untuk versi ini). **Hindari Python 3.14** (terlalu baru, banyak dependency belum ada wheel-nya).
- Redis (opsional — ada fallback in-memory kalau tidak tersedia)

### 1. Virtual environment
```bash
venv\Scripts\activate           # Windows (cmd/powershell)
source venv/Scripts/activate    # Windows Git Bash
```

### 2. Install requirements
```bash
pip install --prefer-binary -r requirements.txt
```
> `numpy`/`scipy` sengaja tidak di-pin versi lama karena tidak ada wheel untuk Python 3.13+. `--prefer-binary` memastikan pip ambil wheel siap pakai, bukan compile dari source.

### 3. Dataset
Dataset audio real (13 file, dari `Data sound kdrt/`) sudah ada di `ml/dataset/kdrt/` & `ml/dataset/normal/`. Dataset fitur RMS/ZCR/Peak untuk model `/predict-features`:
```bash
python ml/generate_feature_dataset.py   # dataset sintetis (12.000 baris)
python ml/extract_real_features.py      # tambah sinyal dari audio real (87 sampel)
```

### 4. Training model
```bash
python ml/train_model.py           # model suara (MFCC)        -> ml/trained_model/model.pkl
python ml/train_text_model.py      # model teks (TF-IDF)       -> ml/trained_model/text_model.pkl
python ml/train_feature_model.py   # model fitur (RMS/ZCR/Peak) -> ml/trained_model/feature_model.pkl
```

### 5. Jalankan server
```bash
uvicorn app.main:app --reload
```
Buka `http://127.0.0.1:8000/docs` untuk Swagger UI.

### 6. Setup Redis (opsional)
Docker: `docker run -p 6379:6379 redis`, atau Memurai (Windows native), atau Redis Cloud. Isi `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD` di `.env` (copy dari `.env.example`).

### 7. Setup Firebase
1. Buat project di [Firebase Console](https://console.firebase.google.com/) → aktifkan Firestore
2. Project Settings → Service Accounts → Generate new private key
3. Simpan file JSON sebagai `firebase-credentials.json` di root project (sudah di `.gitignore`)
4. Isi `FIREBASE_CREDENTIALS_PATH` di `.env`

Untuk deploy cloud (tidak bisa upload file), isi `FIREBASE_CREDENTIALS_JSON` di env var dengan isi file JSON-nya langsung.

### 8. Setup Twilio
1. Daftar di [twilio.com](https://www.twilio.com/) → aktifkan WhatsApp Sandbox (Console → Messaging → Try it out)
2. Ambil Account SID & Auth Token
3. Nomor penerima harus kirim kode join ke `whatsapp:+14155238886` dari WA masing-masing
4. Isi `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM`, `TWILIO_TARGET` di `.env`
5. Untuk lampiran audio & webhook konfirmasi kontak, isi `PUBLIC_BASE_URL` dengan URL ngrok/domain publik

Kalau kredensial Redis/Firebase/Twilio belum diisi, API tetap jalan normal — fitur terkait cuma di-skip dengan warning log, tidak error.

</details>

## Roadmap

- Kalibrasi model pakai data RMS/ZCR/Peak dari device fisik asli (bukan sintetis)
- Pasang kembali autentikasi device/API key sebelum dipakai data pengguna asli
- Upgrade Twilio dari Sandbox ke WhatsApp Business number resmi
- Deploy ke domain publik permanen (menggantikan ketergantungan ngrok)
- Perbanyak dataset audio real & granularitas kelas (marah, panik, ancaman verbal, dll.)
- Kombinasi hasil model suara + teks jadi 1 skor gabungan
