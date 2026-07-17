package com.example.suararumah.data.remote.dto

/**
 * DTO generic response dari backend untuk operasi non-klasifikasi.
 * Digunakan sebagai response dari trigger-alert, cancel-alert, report-safe.
 */
data class ApiResponse(
    /** Apakah operasi berhasil */
    val success: Boolean,

    /** Pesan deskriptif */
    val message: String,

    /** ID referensi alert (untuk tracking) */
    val alertId: String? = null
)
