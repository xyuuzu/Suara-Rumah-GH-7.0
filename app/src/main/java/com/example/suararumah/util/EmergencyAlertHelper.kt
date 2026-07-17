package com.example.suararumah.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper untuk menangani pengiriman pesan darurat (SMS Fallback & GPS Location).
 * Sesuai Dokumen Final v3 (Bab 4.2 & Bab 4.5):
 * - Memiliki jalur independen dari backend (SmsManager native Android)
 * - Mengambil koordinat GPS nyata (FusedLocationProviderClient) atau fallback Sangatta Utara Kutai Timur (-0.5022, 117.5504)
 */
object EmergencyAlertHelper {

    private const val TAG = "EmergencyAlertHelper"

    /**
     * Kirim SMS darurat ke semua nomor terdaftar lengkap dengan tautan Google Maps koordinat lokasi.
     */
    suspend fun sendEmergencyAlert(context: Context, phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) {
            Log.w(TAG, "Tidak ada kontak darurat untuk dikirimi SMS.")
            return
        }

        val (lat, lng) = getCurrentLocationOrDefault(context)
        val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date())

        val message = "[SUARA RUMAH DARURAT 🚨] Terdeteksi anomali kekerasan fisik akut! " +
                "Lokasi: https://maps.google.com/?q=$lat,$lng " +
                "Waktu: $timeStr. Segera hubungi atau ambil tindakan!"

        Log.d(TAG, "Mencoba kirim SMS darurat ke ${phoneNumbers.size} nomor: $message")
        sendSmsToAll(context, phoneNumbers, message)
    }

    /**
     * Kirim SMS pesan susulan "kondisi aman / alarm palsu" ke semua nomor terdaftar.
     */
    fun sendFalseAlarmSms(context: Context, phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) return

        val message = "[SUARA RUMAH UPDATE ✅] Kondisi telah dinyatakan AMAN oleh pengguna. " +
                "Peringatan sebelumnya dibatalkan."

        Log.d(TAG, "Mencoba kirim SMS kondisi aman ke ${phoneNumbers.size} nomor: $message")
        sendSmsToAll(context, phoneNumbers, message)
    }

    /**
     * Ambil koordinat lokasi perangkat via FusedLocationProviderClient.
     * Jika izin GPS tidak diberikan atau lokasi null (misal di emulator baru),
     * otomatis beralih ke koordinat default Sangatta Utara (Kutai Timur) sesuai studi kasus PRD.
     */
    private suspend fun getCurrentLocationOrDefault(context: Context): Pair<Double, Double> {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w(TAG, "Izin lokasi tidak diberikan. Menggunakan koordinat default Sangatta Utara Kutai Timur.")
            return Pair(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location: Location? = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Log.d(TAG, "Lokasi GPS berhasil diambil: ${location.latitude}, ${location.longitude}")
                Pair(location.latitude, location.longitude)
            } else {
                Log.w(TAG, "lastLocation null. Menggunakan koordinat default Sangatta Utara.")
                Pair(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil lokasi GPS: ${e.message}. Menggunakan default Sangatta Utara.")
            Pair(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)
        }
    }

    /**
     * Kirim pesan teks menggunakan SmsManager native Android.
     */
    private fun sendSmsToAll(context: Context, phoneNumbers: List<String>, message: String) {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission) {
            Log.w(TAG, "Izin SEND_SMS belum diberikan di AndroidManifest/Runtime. SMS dicatat ke Logcat saja.")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            for (phone in phoneNumbers) {
                // Gunakan divideMessage jika pesan panjang
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(phone, null, message, null, null)
                }
                Log.d(TAG, "SMS terkirim ke $phone")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengirim SMS via SmsManager: ${e.message}", e)
        }
    }
}
