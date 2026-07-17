# Product Requirements Document (PRD) - Suara Rumah v2
**Track:** Safety | **Event:** Garuda Hacks 7.0
*Revisi: mengintegrasikan hasil evaluasi alur/desain sebelumnya*

## 1. Problem Statement & Context
Aplikasi *panic button* konvensional gagal melindungi korban Kekerasan Dalam Rumah Tangga (KDRT) karena mengasumsikan korban memiliki kesempatan dan keberanian untuk bertindak aktif di saat genting. Mayoritas KDRT berbentuk kekerasan verbal dan psikologis, namun prototipe ini difokuskan secara spesifik pada skenario kekerasan fisik akut, yaitu situasi yang paling membutuhkan respons darurat. Hal ini menggeser paradigma "Safety" dari sekadar ancaman eksternal (bencana alam/kriminalitas jalanan) ke ranah domestik.

## 2. Product Solution & Concept
"Suara Rumah" adalah aplikasi Android yang beroperasi di latar belakang (*background*) untuk mendengarkan pola suara *ambient* secara pasif. Sistem akan mendeteksi pola akustik yang mengindikasikan kekerasan fisik akut, seperti teriakan berkepanjangan atau suara benda pecah/jatuh yang berulang.
* **Privacy-First Architecture:** Audio mentah tidak pernah direkam, disimpan, atau dikirim keluar dari perangkat; hanya ekstraksi fitur numerik (amplitudo, frekuensi, MFCC dasar) yang diproses lalu dibuang.
* **Discreet Alerting:** Mengirimkan peringatan senyap (*silent alert*) berisi lokasi dan waktu kejadian ke kontak darurat yang dipilih sendiri oleh pengguna, dengan interaksi korban dibuat seminimal dan sesenyap mungkin (lihat Bagian 5).

## 3. System Architecture & Data Flow

Pendekatan hibrida (*on-device vs server-side processing*), dengan beberapa penambahan ringan dari versi sebelumnya untuk menutup celah keandalan dasar:

1. **Mobile (Kotlin):** `AudioRecord API` native untuk *rolling buffer* 5-10 detik, ekstraksi fitur *on-device* (amplitudo/RMS, zero-crossing rate) — **tanpa library DSP/ML tambahan**, konsisten dengan prinsip menghindari dependency baru yang belum familiar di waktu terbatas. Data numerik dikirim via HTTPS (Retrofit/OkHttp) disertai **API key statis per-device** di header request (autentikasi minimal, bukan solusi keamanan penuh — cukup menutup celah "siapa saja bisa kirim payload palsu ke endpoint publik").
2. **Backend (Python FastAPI):** Klasifikasi via *threshold rule* atau model ringan (SVM/Random Forest). *Sliding-window tracker* anomali disimpan **in-memory (dictionary per user-id)** untuk skala MVP — didokumentasikan eksplisit sebagai batasan yang perlu diganti *proper state store* (mis. Redis) kalau lanjut ke produksi.
3. **Reliability dasar:** Kalau request dari mobile ke backend gagal, Kotlin melakukan **retry maksimal 3x** dengan jeda singkat sebelum buffer dibuang; kalau tetap gagal, dicatat ke Room DB lokal sebagai log "sistem sempat offline" — supaya tidak ada kegagalan yang sepenuhnya senyap tanpa jejak.
4. **Alert Pipeline:** Jika anomali eskalatif terdeteksi dan *grace period* terlewati tanpa pembatalan, aplikasi mengambil koordinat GPS (`FusedLocationProviderClient`) dan mengirim pesan darurat via Twilio API. Histori dicatat di Firebase Firestore.
5. **Status Monitoring (baru):** Layar utama aplikasi menampilkan indikator sederhana **"Memantau aktif" (hijau) / "Tidak memantau" (abu-abu)**, diambil dari status foreground service + timestamp request terakhir ke backend — supaya pengguna tahu kalau sistem sedang tidak berjalan, bukan diam-diam berhenti tanpa indikasi apa pun.
6. **Follow-up Alarm Palsu (baru):** Setelah alert terkirim, jika pengguna membuka app dan menandai kejadian sebagai "Aman, ini alarm palsu", sistem mengirim **satu pesan susulan otomatis** ke kontak darurat: *"Update: [nama] menandai kondisi sudah aman."* — mencegah kontak darurat menghubungi berulang untuk kasus salah deteksi.

## 4. MVP Scope (Target Waktu: 25 Jam)

### Must-Have Features (Demonstrasi Juri):
* Rekam/unggah *audio clip* (simulasi) di aplikasi.
* Ekstraksi fitur audio secara lokal di perangkat (native Kotlin, tanpa library tambahan).
* Pengiriman data fitur ke backend (dengan API key header) beserta klasifikasi ringan.
* Pemicu notifikasi darurat nyata via Twilio.
* UI Dashboard: grafik real-time, status monitoring (aktif/tidak), dan histori alert.
* Konfigurasi setup minimal 1 kontak darurat.
* Grace period dengan getar pola khusus + pembatalan (lihat Bagian 5).
* Retry dasar (3x) + log lokal saat request gagal terkirim.

### Out of Scope (Jangan Dikerjakan):
* Pemrosesan *Speech-to-text/NLP* untuk isi percakapan.
* Sistem autentikasi pengguna yang kompleks (API key statis sudah cukup untuk MVP).
* Pengembangan versi iOS.
* Model *deep learning* berat (CNN spectrogram).
* State store terdistribusi (Redis dsb.) — cukup in-memory untuk skala demo.

### Secara Sadar Di-drop untuk 25 Jam (bukan terlewat, tapi keputusan eksplisit):
* Verifikasi tambahan/biometrik untuk mengubah kontak darurat (roadmap pasca-hackathon).
* Audit keamanan penuh terhadap endpoint (API key statis dianggap cukup untuk MVP).
* Deteksi multi-sumber suara bercampur (noise kompleks) — diuji hanya dengan clip terkontrol.

## 5. False Positive Mitigation & UI Implementation

**Mekanisme Deteksi → Grace Period:**
Saat anomali eskalatif terdeteksi, HP bergetar dengan pola khusus (2 getar pendek + 1 panjang) — **tanpa dialog box besar/mencolok**, konsisten dengan prinsip *discreet*.

**Kanal Pembatalan (dua jalur, saling melengkapi):**
1. **Tombol volume fisik** — jalur utama, paling senyap. **Aktif hanya selama window grace period berjalan** (bukan terus-menerus saat mode Aman menyala), supaya tidak mengganggu kontrol volume normal sehari-hari.
2. **Tombol "Aman" di layar utama app** — jalur cadangan kalau tombol volume gagal ditangkap atau pengguna sempat membuka app. Tombol ini **selalu ada** di layar utama (bukan popup baru yang muncul dadakan) — saat grace period aktif, tombol ini cukup berubah warna/label sementara, bukan memunculkan elemen UI baru.

**⚠️ Risiko Teknis & Fallback (wajib divalidasi di awal development, lihat Task.md Fase 1):**
Menangkap tombol volume fisik secara andal saat app di background dan layar terkunci **tidak selalu bisa dilakukan dengan listener sederhana** di Android — bisa membutuhkan `MediaSession` atau *Accessibility Service* yang kompleks dan berisiko dianggap perilaku *stalkerware* oleh sistem. Sebelum fitur ini dikerjakan penuh:
- **Fallback yang diterima**: kalau tombol volume hanya bisa ditangkap saat app di foreground, klaim produk direvisi menjadi "pembatalan cepat begitu app dibuka", bukan "tanpa buka app sama sekali" — jalur cadangan (tombol "Aman") tetap memenuhi kebutuhan MVP.
- Kondisi uji untuk validasi (lihat Task.md): app di foreground vs app di background/layar terkunci, dicatat secara terpisah.

**Durasi Grace Period:**
60 detik untuk kondisi nyata/produk. **Dipercepat menjadi 8-10 detik khusus untuk demo di depan juri**, dengan disclosure terbuka: *"Untuk efisiensi waktu presentasi kami percepat grace period jadi 10 detik — di produk nyata nilainya 60 detik."*
