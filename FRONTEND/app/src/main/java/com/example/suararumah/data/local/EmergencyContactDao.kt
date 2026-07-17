package com.example.suararumah.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi CRUD pada tabel emergency_contacts.
 */
@Dao
interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: EmergencyContactEntity)

    @Delete
    suspend fun delete(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Ambil semua kontak, urut berdasarkan nama */
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAll(): Flow<List<EmergencyContactEntity>>

    /** Ambil semua nomor telepon (untuk dikirim ke backend saat alert) */
    @Query("SELECT phoneNumber FROM emergency_contacts")
    suspend fun getAllPhoneNumbers(): List<String>

    /** Cek apakah sudah ada kontak terdaftar */
    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getCount(): Int
}
