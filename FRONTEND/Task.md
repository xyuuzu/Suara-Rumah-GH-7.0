# Task Management & Milestone - Suara Rumah v2
**Target Penyelesaian:** 25 Jam Hacking
*Revisi: validasi risiko teknis dipindah ke awal, success indicator diperjelas*

## 1. Pembagian Fase (Phases)

### Fase 1: Setup, Fondasi, & Validasi Risiko Teknis (Jam 0-4)
**Fokus:** Kerangka dasar aplikasi/server, DAN validasi dini titik-titik berisiko tinggi sebelum dikerjakan penuh.
* Inisiasi project Android dengan Jetpack Compose.
* Inisiasi project Backend menggunakan Python FastAPI.
* Struktur fungsi penangkap suara — **native `AudioRecord`, tanpa library DSP/ML tambahan** (TarsosDSP dihapus dari opsi; konsisten dengan prinsip hindari dependency baru yang belum familiar).
* Endpoint dasar backend untuk menerima data numerik, dengan **header API key statis** untuk autentikasi minimal.
* **🔴 SPIKE (prioritas tinggi, maksimal 1-2 jam): validasi penangkapan tombol volume fisik** dalam 2 kondisi terpisah — (a) app di foreground, (b) app di background/layar terkunci. Hasil spike menentukan apakah Fase 4 memakai pendekatan penuh atau fallback (lihat catatan Fase 4).

### Fase 2: Ekstraksi Data & Logika Deteksi (Jam 4-10)
**Fokus:** Pemrosesan audio di device dan klasifikasi data.
* Implementasi penangkapan audio real-time ke rolling buffer 5-10 detik.
* Fungsi kalkulasi fitur on-device (Amplitudo/RMS, Zero-Crossing Rate).
* Logika klasifikasi ringan (threshold rule) di backend untuk deteksi anomali.
* **Sliding-window tracker anomali disimpan in-memory (dictionary per user-id)** di backend — cukup untuk skala demo, dicatat sebagai batasan MVP.
* Pengujian endpoint klasifikasi menggunakan data dummy.

### Fase 3: Integrasi End-to-End & Pipeline Darurat (Jam 10-16)
**Fokus:** Menghubungkan aplikasi ke server dan sistem notifikasi.
* Retrofit/OkHttp untuk kirim data fitur dari app ke backend, **dengan retry maksimal 3x** sebelum buffer dibuang jika gagal (dicatat ke Room DB lokal kalau tetap gagal).
* UI untuk mendaftarkan 1 kontak darurat.
* Integrasi Twilio API (Sandbox) di backend untuk pengiriman alert (SMS/WhatsApp).
* Endpoint tambahan untuk **pesan susulan "alarm palsu"** (dipicu saat user menandai kejadian aman setelah alert terkirim).

### Fase 4: Mekanisme Grace Period & Dashboard (Jam 16-20)
**Fokus:** Perlindungan false positive dan visualisasi data.
* UI Dashboard utama: grafik fluktuasi suara real-time + **indikator status "Memantau aktif"/"Tidak memantau"**.
* Implementasi grace period (durasi 8-10 detik khusus demo, 60 detik untuk kondisi produk — dibuat *configurable*, bukan hardcoded).
* Logika getaran perangkat (pola khusus) saat anomali terdeteksi.
* Tombol "Aman" persisten di layar utama sebagai kanal pembatalan cadangan (berubah warna/label saat grace period aktif, bukan popup baru).
* **Implementasi tombol volume fisik SESUAI HASIL SPIKE Fase 1:**
  - Kalau spike berhasil untuk kondisi background/terkunci → implementasikan penuh, **listener aktif hanya selama window grace period berjalan** (bukan terus-menerus saat mode Aman menyala).
  - Kalau spike hanya berhasil untuk foreground → implementasikan versi foreground-only, dan sesuaikan narasi pitch ("pembatalan cepat begitu app dibuka").
* Penyimpanan log histori alert sederhana di Firestore.

### Fase 5: Polish & Testing (Jam 20-25)
**Fokus:** Stabilisasi sistem untuk demonstrasi.
* Perekaman dan pengujian 4 jenis klip audio simulasi (normal, teriakan, benda pecah, eskalatif).
* Penyelesaian bug sinkronisasi antara aplikasi dan server.
* Pengujian flow pembatalan (volume button sesuai hasil spike, dan tombol "Aman" sebagai cadangan).
* Pengujian follow-up "alarm palsu" dan indikator status monitoring.

---

## 2. Tujuan Akhir (Final Goal)
Menghasilkan prototipe aplikasi yang dapat mendeteksi klip audio berisi anomali kekerasan fisik secara pasif, mengirimkan fitur numerik ke server (dengan autentikasi dasar dan retry), menampilkan status monitoring yang jelas, menunggu selama durasi grace period demo, dan berhasil mengirimkan pesan peringatan nyata ke nomor tujuan jika tidak dibatalkan — lewat salah satu dari dua kanal pembatalan yang tersedia.

---

## 3. Indikator Keberhasilan (Success Indicators)

1. **Audio Processing:** 4 jenis klip audio simulasi berhasil diproses dengan benar di dalam pipeline.
2. **Real-time UI:** Grafik dashboard bergerak responsif dan sinkron dengan klip audio yang dimainkan, disertai indikator status monitoring yang akurat.
3. **Cancellation Channel — kondisi uji dipisah secara eksplisit:**
   - (a) **App di foreground**: tombol volume DAN tombol "Aman" on-screen terbukti berhasil membatalkan grace period.
   - (b) **App di background/layar terkunci**: dicatat hasilnya sesuai kemampuan riil dari spike Fase 1 — kalau tidak berhasil, tombol "Aman" saat app dibuka tetap dihitung sebagai kanal cadangan yang valid untuk demo.
4. **End-to-End Alerting:** Pesan darurat nyata (SMS/WhatsApp) terbukti terkirim ke nomor uji coba via Twilio jika tidak ada pembatalan.
5. **Reliability dasar:** Retry dan log lokal terbukti bekerja saat koneksi disimulasikan terputus (mis. mode pesawat sesaat).
6. **Follow-up Alarm Palsu:** Pesan susulan "kondisi aman" berhasil terkirim setelah user menandai alert sebagai salah deteksi.
