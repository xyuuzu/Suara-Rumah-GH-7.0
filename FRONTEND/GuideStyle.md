# UI/UX Style Guide - Suara Rumah v2
**Tema Desain:** Minimalist, Discreet (Terselubung), & Calming.
**Konsep Visual:** Menyerupai aplikasi *wellness* atau *smart home utility* agar tidak mencurigakan.
*Revisi: klarifikasi komponen status monitoring & relasi kanal pembatalan.*

## 1. Color Palette (Warna Utama)
*(Tidak berubah dari versi sebelumnya — sudah konsisten dengan prinsip discreet)*

* **Primary Color (Hijau Kalem):** `#2A9D8F` — brand utama, tombol "Aman", toggle aktif, ikon utama.
* **Background Color (Off-White):** `#F8F9FA` — latar seluruh layar.
* **Surface/Card Color (Putih Bersih):** `#FFFFFF` — latar card/widget/wadah grafik.
* **Text Primary (Dark Slate):** `#2B2D42` — judul, body text.
* **Text Secondary (Muted Grey):** `#8D99AE` — caption, label, status "Tidak memantau".
* **Alert/Danger (Muted Coral):** `#E76F51` — HANYA untuk garis grafik saat anomali dan state tombol "Aman" saat grace period aktif (lihat Bagian 3). Jangan dipakai untuk elemen besar/full-screen.

## 2. Typography (Tipografi)
*(Tidak berubah)*

* **Font Family:** `Roboto` (default Jetpack Compose) atau `Inter`.
* **H1:** Bold, 24sp — judul halaman ("Status Pemantauan").
* **H2:** Semi-Bold, 18sp — judul card/section.
* **Body Text:** Regular, 14sp — penjelasan, nama kontak darurat.
* **Caption:** Regular, 12sp — timestamp, status log.

## 3. UI Components — Klarifikasi Relasi Antar-Kanal Pembatalan

Ini bagian yang diperjelas dari versi sebelumnya, supaya tidak ada ambiguitas implementasi:

### 3.1 Indikator Status Monitoring (baru)
* Komponen kecil di bagian atas layar utama (bukan notifikasi sistem) berupa **dot + label teks**:
  - Hijau (`#2A9D8F`) + "Memantau aktif" — saat foreground service berjalan normal dan request terakhir ke backend berhasil.
  - Abu-abu (`#8D99AE`) + "Tidak memantau" — saat service mati atau tidak ada respons dari backend dalam jangka waktu tertentu.
* Ukuran dot: 8dp, tanpa animasi berkedip (sesuai prinsip "tanpa animasi lebay").

### 3.2 Tombol "Aman" — Satu Komponen, Dua State (bukan dua komponen terpisah)
Untuk menjaga prinsip *discreet* (tidak memunculkan dialog/popup baru saat grace period aktif), tombol "Aman" dirancang sebagai **satu komponen persisten** di layar utama dengan dua tampilan state:

| State | Warna Latar | Label | Kapan Muncul |
|---|---|---|---|
| Normal | `#2A9D8F` (hijau) | "Aman" | Sepanjang mode Aman menyala, kondisi normal |
| Grace Period Aktif | `#E76F51` (coral, muted) | "Aman — Batalkan Peringatan" | Hanya selama window grace period berjalan |

Tombol ini **tidak berpindah posisi, tidak muncul sebagai overlay/dialog baru** — hanya warna dan label yang berubah di tempat yang sama. Ini menghindari kesan "ada sesuatu yang mencolok muncul tiba-tiba" kalau layar terlihat orang lain.

* Touch target: minimal `48x48dp` (mudah ditekan tangan gemetar/panik), tapi visual tetap subtil — tidak membesar drastis saat berubah state.

### 3.3 Tombol Volume Fisik — Kanal Utama, Tanpa Elemen Visual
Tombol volume fisik **tidak memiliki representasi visual apa pun** di UI — ini justru nilai jualnya (paling senyap). Yang perlu dipastikan dari sisi desain:
* Getaran konfirmasi pembatalan (kalau berhasil dibatalkan lewat volume button) **harus berbeda pola** dari getaran deteksi awal — mis. 1 getar pendek saja — supaya user tahu pembatalan berhasil tanpa perlu melihat layar.
* Kanal ini **hanya aktif selama grace period berjalan** (lihat PRD v2 Bagian 5) — di luar window itu, tombol volume berfungsi normal untuk kontrol volume media/panggilan seperti biasa.

### 3.4 Cards (Wadah Konten)
*(Tidak berubah)* — Corner radius `16dp`, elevation tipis (`2dp`).

### 3.5 Grafik Audio (Real-time Chart)
*(Tidak berubah)* — line/bar chart minimalis, warna primary saat normal, alert color hanya saat anomali.

### 3.6 Toggle / Switch
*(Tidak berubah)* — Material 3 default untuk mode "Aman".

## 4. Interaksi & UX (Penting untuk Kondisi Darurat)
* **Tanpa Animasi Lebay:** Transisi antar layar cepat dan instan, termasuk perubahan state tombol "Aman" (Bagian 3.2) — perubahan warna/label instan, tanpa transisi fade/slide yang lambat.
* **Ukuran Area Sentuh:** Tombol "Aman" minimal `48x48dp` (lihat 3.2).
* **Dark Mode:** Direkomendasikan (`#121212` sebagai dasar) untuk penggunaan malam hari — **diputuskan sebagai *nice-to-have*, bukan MVP wajib**, dikerjakan hanya jika waktu di Fase 5 memungkinkan.
