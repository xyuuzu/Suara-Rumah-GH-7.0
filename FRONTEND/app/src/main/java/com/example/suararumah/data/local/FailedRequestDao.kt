package com.example.suararumah.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi CRUD pada tabel failed_requests.
 */
@Dao
interface FailedRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: FailedRequestEntity)

    /** Ambil semua request yang belum resolved, urut dari terbaru */
    @Query("SELECT * FROM failed_requests WHERE resolved = 0 ORDER BY timestamp DESC")
    fun getUnresolved(): Flow<List<FailedRequestEntity>>

    /** Ambil semua log (termasuk resolved) */
    @Query("SELECT * FROM failed_requests ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FailedRequestEntity>>

    /** Tandai request sebagai resolved (berhasil di-retry) */
    @Query("UPDATE failed_requests SET resolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long)

    /** Hapus semua log yang sudah resolved */
    @Query("DELETE FROM failed_requests WHERE resolved = 1")
    suspend fun clearResolved()

    /** Jumlah request yang belum resolved */
    @Query("SELECT COUNT(*) FROM failed_requests WHERE resolved = 0")
    suspend fun getUnresolvedCount(): Int
}
