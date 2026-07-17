package com.example.suararumah.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk kontak darurat.
 * MVP: minimal 1 kontak darurat yang akan menerima alert.
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Nama kontak darurat */
    val name: String,

    /** Nomor telepon (format: +62xxx) */
    val phoneNumber: String,

    /** Relasi dengan pengguna (opsional: "Keluarga", "Teman", dll) */
    val relationship: String = ""
)
