package com.example.suararumah.data.repository

import android.content.Context
import com.example.suararumah.data.local.AppDatabase
import com.example.suararumah.data.local.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk mengelola kontak darurat.
 * Data disimpan secara lokal di Room DB.
 */
class ContactRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).emergencyContactDao()

    /** Ambil semua kontak darurat (reactive) */
    fun getAllContacts(): Flow<List<EmergencyContactEntity>> = dao.getAll()

    /** Tambah kontak darurat baru */
    suspend fun addContact(name: String, phoneNumber: String, relationship: String = "") {
        dao.insert(
            EmergencyContactEntity(
                name = name,
                phoneNumber = phoneNumber,
                relationship = relationship
            )
        )
    }

    /** Hapus kontak berdasarkan ID */
    suspend fun deleteContact(id: Long) {
        dao.deleteById(id)
    }

    /** Ambil semua nomor telepon untuk dikirim ke backend saat alert */
    suspend fun getAllPhoneNumbers(): List<String> = dao.getAllPhoneNumbers()

    /** Cek apakah minimal 1 kontak sudah terdaftar */
    suspend fun hasContacts(): Boolean = dao.getCount() > 0
}
