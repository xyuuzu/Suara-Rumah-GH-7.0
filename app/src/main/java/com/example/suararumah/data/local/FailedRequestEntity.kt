package com.example.suararumah.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk menyimpan log request yang gagal terkirim ke backend.
 * Sesuai PRD: "dicatat ke Room DB lokal sebagai log sistem sempat offline"
 */
@Entity(tableName = "failed_requests")
data class FailedRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Timestamp saat request gagal (epoch millis) */
    val timestamp: Long,

    /** JSON payload yang gagal dikirim */
    val payload: String,

    /** Pesan error dari exception */
    val errorMessage: String,

    /** Jumlah retry yang sudah dilakukan */
    val retryCount: Int,

    /** Apakah request ini sudah berhasil di-retry nanti */
    val resolved: Boolean = false
)
