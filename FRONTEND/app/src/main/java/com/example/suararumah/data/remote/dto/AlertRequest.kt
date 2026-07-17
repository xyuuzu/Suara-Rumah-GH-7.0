package com.example.suararumah.data.remote.dto

/**
 * DTO untuk memicu atau membatalkan alert darurat.
 * Digunakan untuk:
 * - POST /api/v1/trigger-alert  → kirim pesan darurat via Twilio
 * - POST /api/v1/cancel-alert   → batalkan alert sebelum terkirim
 * - POST /api/v1/report-safe    → kirim pesan susulan "alarm palsu"
 */
data class AlertRequest(
    /** User/device identifier */
    val userId: String,

    /** Daftar nomor telepon kontak darurat */
    val contactNumbers: List<String>,

    /** Latitude lokasi saat alert dipicu */
    val latitude: Double?,

    /** Longitude lokasi saat alert dipicu */
    val longitude: Double?,

    /** Timestamp saat alert dipicu (epoch millis) */
    val timestamp: Long,

    /**
     * Tipe alert:
     * - "emergency"             → pesan darurat pertama
     * - "false_alarm_followup"  → pesan susulan "kondisi aman"
     * - "cancel"                → pembatalan sebelum terkirim
     */
    val alertType: String
)
