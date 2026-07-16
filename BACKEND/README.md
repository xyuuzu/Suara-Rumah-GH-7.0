# KDRT Voice & Text Detector — Setup Guide

Aplikasi pendeteksi indikasi KDRT dari **suara** dan **teks**. Fokus utama: deteksi kekerasan (verbal/teriakan) sedini mungkin. Backend pakai FastAPI, fitur suara diekstrak jadi angka lewat librosa, fitur teks diekstrak jadi angka lewat TF-IDF, keduanya diklasifikasi pakai model scikit-learn. Redis dipakai sebagai lapisan cache/filter di depan pipeline ML, Firebase (Firestore) buat simpan history hasil deteksi.

## 0. Prasyarat
- **Python 3.11 atau 3.12** direkomendasikan. Environment ini pakai **Python 3.13** (`C:\Python313`) karena itu versi non-3.14 yang tersedia di mesin — numpy/scipy/librosa versi terbaru sudah punya wheel prebuilt untuk 3.13. **Hindari Python 3.14** (baru rilis, numpy/numba/librosa belum ada wheel-nya, bakal gagal install / harus compile dari source).
- pip
- Redis server (lihat bagian 6)

## 1. Virtual environment

Venv sudah dibuat di `venv/` pakai Python 3.13. Aktifkan:

```bash
venv\Scripts\activate           # Windows (cmd/powershell)
source venv/Scripts/activate    # Windows Git Bash
```

## 2. Install semua requirements

```bash
pip install --prefer-binary -r requirements.txt
```

> `numpy`/`scipy` sengaja tidak di-pin versi lama di `requirements.txt` karena versi lama tidak punya wheel untuk Python 3.13+ (butuh compiler Fortran buat build dari source, biasanya tidak ada di Windows). `--prefer-binary` memastikan pip ambil wheel siap pakai.
> Kalau ada error terkait `soundfile`/ffmpeg pas load format selain `.wav`, install ffmpeg dan tambahkan ke PATH (Windows: download dari ffmpeg.org).

## 3. Dataset

### Dataset audio saat ini: real data dari `Data sound kdrt/`

`ml/dataset/kdrt/` (7 file) dan `ml/dataset/normal/` (6 file) sudah diisi dari rekaman asli di folder `Data sound kdrt/` (bukan sintetis lagi):
- `Data sound kdrt/high confident/` → label `kdrt` (contoh jelas kekerasan: gebuk, tonjok, teriak marah)
- `Data sound kdrt/low confident/` → label `normal` (suara mirip² keras/nangis tapi BUKAN kekerasan — dipakai sebagai hard-negative biar model ga asal nge-flag semua suara keras/nangis sebagai KDRT)

**Catatan penting**: 13 sample total (7/6) itu masih SANGAT KECIL buat machine learning. Angka akurasi yang keluar dari `train_model.py` (train/test split ~10/3) bakal kelihatan tinggi tapi tidak bisa dipercaya secara statistik — itu bukan sinyal model udah bagus, cuma karena test set-nya cuma 2-3 sample. Prioritas berikutnya: perbanyak data di kedua folder sebelum model ini dipakai buat apa pun yang serius.

### Dataset sementara (dummy/sintetis) — kalau mau generate ulang buat testing pipeline

```bash
python ml/generate_dummy_dataset.py
```

Script ini masih ada dan bisa dipakai kalau butuh data dummy lagi (misal buat testing pipeline di mesin lain yang belum ada data asli). **Tapi hati-hati**: menjalankan ini akan menimpa isi `ml/dataset/kdrt/` & `ml/dataset/normal/` dengan audio sintetis lagi — jangan jalankan kalau mau tetap pakai dataset real di atas.

### Dataset asli/publik buat modal awal

**Audio (proxy emosi marah/takut/teriak, belum ada dataset "KDRT" publik spesifik):**
- [RAVDESS – Kaggle](https://www.kaggle.com/uwrfkaggler/ravdess-emotional-speech-audio) — 1.440 rekaman, 8 emosi termasuk *angry* & *fearful*
- [CREMA-D – Kaggle](https://www.kaggle.com/ejlok1/cremad) — 7.442 klip, 6 emosi termasuk *anger* & *fear*
- Untuk deteksi teriakan murni, kombinasikan dengan dataset sound-event umum (ESC-50, UrbanSound8K) sebagai negative samples.

**Teks Bahasa Indonesia (proxy abusive/hate speech, belum ada dataset "KDRT" spesifik juga):**
- [Indonesian Abusive and Hate Speech Twitter Text – Kaggle](https://www.kaggle.com/datasets/ilhamfp31/indonesian-abusive-and-hate-speech-twitter-text)
- [id-multi-label-hate-speech-and-abusive-language-detection – GitHub](https://github.com/okkyibrohim/id-multi-label-hate-speech-and-abusive-language-detection)
- [id-hatespeech-detection – GitHub](https://github.com/ialfina/id-hatespeech-detection)

Tidak ditemukan dataset publik yang spesifik berlabel "KDRT". Untuk data yang benar-benar representatif, pertimbangkan kerja sama riset dengan lembaga seperti Komnas Perempuan / LBH APIK, atau kumpulkan & label sendiri (dengan protokol privasi yang ketat — lihat bagian Privasi di bawah).

Struktur folder buat data asli sama seperti dummy:
```
ml/dataset/kdrt/       <- suara indikasi kekerasan (teriakan, bentakan, dsb) .wav/.mp3
ml/dataset/normal/     <- suara percakapan normal
ml/dataset_text/data.csv   <- kolom: text,label (label: kdrt / normal)
```

### Dataset fitur RMS/ZCR/Peak (buat model `/predict-features`, dipakai mobile)

Beda dari dataset audio di atas (yang isinya file `.wav`/`.mp3` diproses librosa server-side), model di balik `/predict-features` cuma butuh 3 angka per sample: RMS, ZCR, Peak Amplitude -- persis fitur yang diekstrak di device (Kotlin), bukan audio mentah. Dataset-nya:

```bash
python ml/generate_feature_dataset.py
```

Generate `ml/dataset_features/data.csv` (12.000 baris, 3.000 tiap kelas): `normal`, `bantingan` (benda dibanting/pecah), `teriakan`, `darurat_sos` (kombinasi paling ekstrem, buat sinyal darurat eksplisit). Range tiap kelas sengaja dibuat overlap sama tetangganya (misal `teriakan` vs `darurat_sos`) biar model belajar probabilitas yang realistis, bukan batas keputusan yang terlalu tajam. **Ini SINTETIS** — range angka tiap kelas cuma perkiraan awal berdasar karakteristik akustik yang masuk akal (baca komentar di file), bukan hasil kalibrasi dari device fisik asli.

```bash
python ml/extract_real_features.py
```

Nambahin sinyal dari **audio ASLI** yang udah ada di `ml/dataset/kdrt/` & `ml/dataset/normal/` (13 file dari `Data sound kdrt/`) — tiap file dipotong jadi window ~3 detik, RMS/ZCR/Peak dihitung per window pakai librosa, hasilnya ditulis ke `ml/dataset_features/real_audio_features.csv` (87 sample) dan otomatis ke-merge (di-oversample 20x) pas training. Window dari folder `kdrt/` di-assign ke label kekerasan terdekat secara heuristik (nearest-centroid ke rata-rata sintetis) karena audio real cuma dilabel "kdrt" secara keseluruhan, bukan per detik — baca komentar di file buat detail. Opsional tapi disarankan; kalau dilewatin, training cuma pakai data sintetis.

## 4. Training model

```bash
python ml/train_model.py           # model suara (MFCC dkk)    -> ml/trained_model/model.pkl
python ml/train_text_model.py      # model teks (TF-IDF)       -> ml/trained_model/text_model.pkl
python ml/train_feature_model.py   # model fitur (RMS/ZCR/Peak) -> ml/trained_model/feature_model.pkl (otomatis pakai real_audio_features.csv kalau ada)
```

`train_model.py`:
1. Baca semua audio di `ml/dataset/`
2. Ubah tiap audio jadi angka (MFCC, chroma, mel spectrogram, ZCR, RMS energy, pitch) pakai librosa
3. Training RandomForest
4. Evaluasi & simpan model+scaler ke `ml/trained_model/model.pkl`

`train_text_model.py`:
1. Baca `ml/dataset_text/data.csv`
2. Preprocessing (lowercase, hapus stopword Bahasa Indonesia via Sastrawi)
3. TF-IDF vectorize + training LogisticRegression
4. Evaluasi & simpan model+vectorizer ke `ml/trained_model/text_model.pkl`

`train_feature_model.py`:
1. Baca `ml/dataset_features/data.csv` (kolom: rms, zcr, peak, label)
2. Training RandomForest (3 fitur input -> 4 kelas: normal/bantingan/teriakan/darurat_sos)
3. Evaluasi & simpan model+scaler ke `ml/trained_model/feature_model.pkl`

## 5. Jalankan FastAPI server

```bash
uvicorn app.main:app --reload
```

Buka `http://127.0.0.1:8000/docs` buat Swagger UI. Endpoint utama:
- `POST /predict-features` — endpoint resmi buat mobile: kirim `{device_id, rms, zcr, peak}`, dapat klasifikasi `normal`/`bantingan`/`teriakan`/`darurat_sos` + confidence dari model fitur. `darurat_sos` langsung trigger alert WA di deteksi pertama; `bantingan`/`teriakan` butuh pola berulang dulu (lihat `API_BLUEPRINT.txt`).
- `POST /predict` — upload file `.wav`/`.mp3`, dapat prediksi `kdrt`/`normal` + confidence dari suara (endpoint lama, buat testing backend independen dari mobile)
- `POST /predict-text` — kirim JSON `{"text": "..."}`, dapat prediksi dari teks + `keyword_flag` (kalau ada kata kekerasan eksplisit yang match di Redis keyword set)

Detail lengkap kontrak input/output tiap endpoint (termasuk contoh dummy buat testing) ada di `API_BLUEPRINT.txt`.

## 6. Setup Redis (caching hasil + keyword filter cepat)

Redis dipakai untuk:
1. **Cache hasil prediksi** berdasarkan hash (SHA-256) konten audio/teks — request yang identik tidak perlu diproses ulang oleh librosa/model (mahal secara komputasi).
2. **Keyword filter** — Redis Set berisi kata/frasa indikasi kekerasan, dicek cepat (`SISMEMBER`) di endpoint `/predict-text` sebagai sinyal tambahan di luar hasil model ML.

Pilih salah satu cara jalanin Redis di Windows:
- **Docker** (paling gampang): `docker run -p 6379:6379 redis`
- **Memurai** (build Redis native Windows): download dari memurai.com
- **Redis Cloud** (gratis untuk dev, tidak perlu install lokal): buat instance di redis.io/cloud, isi host/port/password di `.env`

Copy `.env.example` jadi `.env`, sesuaikan `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`. Kalau Redis tidak tersedia, API tetap jalan (cache & keyword filter cuma di-skip, prediksi ML tetap jalan normal).

## 7. Setup Firebase (opsional, buat simpan history deteksi)

1. Buat project di [Firebase Console](https://console.firebase.google.com/)
2. Aktifkan Firestore Database
3. Project Settings → Service Accounts → Generate new private key
4. Simpan file JSON hasil download sebagai `firebase-credentials.json` di root project
5. Copy `.env.example` jadi `.env`, isi `FIREBASE_CREDENTIALS_PATH`
6. `firebase-credentials.json` dan `.env` sudah ada di `.gitignore` (jangan sampai ke-push ke git, ini kredensial sensitif)

## 8. Setup Twilio (opsional, WhatsApp alert otomatis)

Twilio ([twilio.com](https://www.twilio.com/)) dipakai buat kirim WhatsApp alert otomatis pas prediksi hasilnya `kdrt` dengan confidence ≥ 70% (threshold di `ALERT_CONFIDENCE_THRESHOLD`, `app/main.py`). Berlaku di `/predict` (suara), `/predict-text` (teks), dan `/predict-features` (mobile — trigger otomatis pas pola eskalatif kedeteksi).

1. Daftar & login di [twilio.com](https://www.twilio.com/)
2. Aktifkan WhatsApp Sandbox: Console → Messaging → Try it out → Send a WhatsApp message
3. Ambil **Account SID** & **Auth Token** dari dashboard Twilio Console
4. Mode sandbox: tiap nomor penerima harus join dulu (kirim kode join yang ditampilkan Twilio ke nomor sandbox `whatsapp:+14155238886` dari WA masing-masing) sebelum bisa nerima pesan
5. Isi di `.env`: `TWILIO_ACCOUNT_SID=<sid>`, `TWILIO_AUTH_TOKEN=<token>`, `TWILIO_WHATSAPP_FROM=<nomor sandbox atau nomor Twilio kamu, opsional — default ke sandbox>`, `TWILIO_TARGET=<nomor WA tujuan fallback, format +628xxxxxxxxxx>`

Kalau kredensial Twilio belum diisi, alert cuma di-skip (log `[WARN] ... dilewati`) — endpoint `/predict`, `/predict-text`, dan `/predict-features` tetap jalan normal, tidak error.

## Alur kerja sistem

```
User rekam/upload suara ATAU ketik teks
        │
        ▼
FastAPI terima request (/predict atau /predict-text)
        │
        ▼
Redis cek cache (hash konten) -> kalau ada, langsung return hasil lama
        │ (kalau belum ada di cache)
        ▼
Suara: librosa ekstrak fitur (MFCC, pitch, RMS energy, dll) -> array angka
Teks:  preprocessing (Sastrawi) -> TF-IDF -> array angka
        │
        ▼
Fitur masuk ke model scikit-learn (RandomForest / LogisticRegression)
Teks juga dicek Redis keyword set (kata kekerasan eksplisit) secara paralel
        │
        ▼
Model keluarkan prediksi "kdrt"/"normal" + confidence score
Hasil disimpan ke Redis cache (TTL 24 jam)
        │
        ▼
(opsional) Hasil disimpan ke Firebase Firestore
```

## Next steps yang perlu dipikirin
- **Dataset asli**: ini bottleneck terbesar, dataset dummy di repo ini cuma buat validasi pipeline. Prioritaskan kumpulin data spesifik konteks (dengan persetujuan & anonimisasi ketat) sebelum model dipakai lebih serius.
- **Kelas label**: sekarang cuma 2 kelas (`kdrt`/`normal`). Bisa dikembangin jadi lebih granular (marah, panik, teriak, ancaman verbal, dll).
- **Real-time detection**: kalau nanti mau deteksi dari stream suara langsung (bukan upload file), butuh pendekatan chunking audio, beda dari yang sekarang.
- **Kombinasi suara + teks**: sekarang dua model terpisah — bisa dikembangin jadi 1 hasil gabungan (mis. skor tertimbang dari kedua model) buat keputusan akhir.
- **Privasi & keamanan data**: karena ini data sensitif (suara/teks korban KDRT), pastikan Firestore rules ketat, enkripsi data, TTL cache Redis wajar (jangan simpan konten sensitif tanpa batas waktu), dan compliance ke UU PDP.
