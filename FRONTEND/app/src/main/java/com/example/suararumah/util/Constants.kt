package com.example.suararumah.util

/**
 * Konstanta global aplikasi Suara Rumah.
 */
object Constants {

    // ── Grace Period ──
    /** Grace period untuk demo di depan juri (8-10 detik) */
    const val GRACE_PERIOD_DEMO_MS = 10_000L

    /** Grace period untuk kondisi produk nyata (60 detik) */
    const val GRACE_PERIOD_PROD_MS = 60_000L

    /** Gunakan mode demo secara default — ubah ke false untuk produksi */
    const val IS_DEMO_MODE = true

    // ── Network ──
    /** Base URL backend FastAPI — rute aktual dari tim backend */
    const val API_BASE_URL = "http://172.25.126.138:63892/"

    /** API key statis per-device untuk autentikasi minimal */
    const val API_KEY = "test"

    /** Header name untuk API key */
    const val API_KEY_HEADER = "test"

    /** Maksimal retry saat request ke backend gagal */
    const val MAX_RETRY = 3

    /** Jeda antar retry dalam milidetik */
    const val RETRY_DELAY_MS = 1_000L

    /** Timeout cepat untuk pengecekan hybrid /predict (3 detik) */
    const val HYBRID_TIMEOUT_MS = 3_000L

    // ── Location Fallback (Studi Kasus: Sangatta Utara, Kutai Timur) ──
    const val DEFAULT_LATITUDE = -0.5022
    const val DEFAULT_LONGITUDE = 117.5504

    // ── Audio & Device ID ──
    /** Device ID & User ID default sesuai Postman Collection terbaru ("test-device") */
    const val DEFAULT_DEVICE_ID = "test-device"
    const val DEFAULT_USER_ID = "test-device"

    // ── Vibration Patterns (dalam milidetik) ──
    /** Pola getaran deteksi anomali (diperlama sesuai request user): 2 sedang + 2 panjang */
    val VIBRATION_PATTERN_DETECTION = longArrayOf(0, 400, 200, 400, 200, 800, 200, 800)

    /** Pola getaran konfirmasi pembatalan: 1 pendek */
    val VIBRATION_PATTERN_CANCELLATION = longArrayOf(0, 150)
}
