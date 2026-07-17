package com.example.suararumah.data.repository

import android.content.Context
import android.util.Log
import com.example.suararumah.audio.AudioFeatures
import com.example.suararumah.data.local.AppDatabase
import com.example.suararumah.data.local.FailedRequestEntity
import com.example.suararumah.data.remote.ApiClient
import com.example.suararumah.data.remote.dto.AudioFeatureRequest
import com.example.suararumah.data.remote.dto.ClassificationResponse
import com.example.suararumah.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Repository utama: orchestrator antara AudioCaptureService ↔ Backend ↔ Room DB.
 *
 * Flow Hybrid:
 * 1. Terima fitur audio dari service
 * 2. Coba kirim ke backend aktual via Retrofit (/predict) dengan timeout cepat (3s)
 * 3. Jika gagal → retry maksimal 3x dengan jeda
 * 4. Jika tetap gagal/timeout → simpan ke Room DB (log "sistem sempat offline")
 * 5. Mulus beralih ke On-Device Threshold Classifier agar pemantauan & alarm darurat tetap aktif 100%
 */
class AudioRepository(context: Context) {

    companion object {
        private const val TAG = "AudioRepository"

        /**
         * Toggle Hybrid Mode: memprioritaskan rute server /predict terlebih dahulu.
         * Jika server tidak merespons (atau sedang under construction), otomatis fallback ke on-device.
         */
        const val USE_HYBRID_BACKEND = true
        const val USE_DUMMY_ONLY = false
    }

    private val apiService = ApiClient.getApiService()
    private val failedRequestDao = AppDatabase.getInstance(context).failedRequestDao()
    private val gson = Gson()

    // ── Dummy classification state (simulasi sliding window) ──
    private var dummyAnomalyCount = 0

    /**
     * Kirim fitur audio ke backend untuk klasifikasi (/predict).
     * Jika server aktual merespons, gunakan hasilnya. Jika offline/error, gunakan hybrid fallback.
     */
    suspend fun analyzeAudio(features: AudioFeatures): ClassificationResponse? {
        if (USE_DUMMY_ONLY) {
            return getDummyClassification(features)
        }

        val request = AudioFeatureRequest(
            rms = features.rms,
            zcr = features.zcr,
            peakAmplitude = features.peakAmplitude,
            timestamp = features.timestamp,
            deviceId = Constants.DEFAULT_DEVICE_ID,
            userId = Constants.DEFAULT_USER_ID,
            latitude = Constants.DEFAULT_LATITUDE,
            longitude = Constants.DEFAULT_LONGITUDE
        )

        if (USE_HYBRID_BACKEND) {
            var lastError: Exception? = null
            for (attempt in 1..Constants.MAX_RETRY) {
                try {
                    // Coba panggil rute resmi predict-features terlebih dahulu
                    var response = withTimeoutOrNull(Constants.HYBRID_TIMEOUT_MS) {
                        apiService.predictFeatures(Constants.DEFAULT_USER_ID, Constants.API_KEY, request)
                    }
                    // Jika rute predict-features belum ada di server (404), fallback ke rute /predict
                    if (response != null && response.code() == 404) {
                        response = withTimeoutOrNull(Constants.HYBRID_TIMEOUT_MS) {
                            apiService.predictAudio(request)
                        }
                    }

                    if (response != null && response.isSuccessful && response.body() != null) {
                        Log.d(TAG, "Hybrid classification OK: ${response.body()}")
                        return response.body()!!
                    } else if (response != null) {
                        val code = response.code()
                        Log.w(TAG, "Hybrid classification HTTP error code: $code")
                        // Jika server mengembalikan 422 (Unprocessable Entity/File required), 400, atau 404,
                        // artinya spesifikasi rute backend sedang diproses (minta UploadFile, bukan JSON).
                        // Jangan di-retry karena akan menghasilkan error yang sama. Langsung gunakan klasifikasi lokal seketika!
                        if (code == 422 || code == 400 || code == 404) {
                            Log.w(TAG, "[HYBRID FALLBACK] Server schema mismatch ($code), using immediate on-device classification.")
                            val fallbackResult = getDummyClassification(features)
                            return fallbackResult.copy(
                                message = "[LOCAL ON-DEVICE] Backend endpoint on progress ($code)"
                            )
                        }
                    } else {
                        Log.w(TAG, "Hybrid classification timeout, attempt $attempt")
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Hybrid network error attempt $attempt: ${e.message}")
                }

                if (attempt < Constants.MAX_RETRY) {
                    delay(Constants.RETRY_DELAY_MS * attempt)
                }
            }

            // Semua retry jaringan gagal — log ke Room DB & gunakan on-device fallback
            logFailedRequest(request, lastError?.message ?: "Hybrid classification timeout or unreachable")
            val fallbackResult = getDummyClassification(features)
            return fallbackResult.copy(
                message = "[HYBRID FALLBACK] Local threshold classification after network error"
            )
        }

        return getDummyClassification(features)
    }

    /**
     * Cek apakah backend bisa dijangkau (untuk StatusIndicator).
     * Dalam mode hybrid, kita kembalikan true karena sistem selalu mampu melakukan pemantauan on-device.
     */
    suspend fun isBackendReachable(): Boolean {
        if (USE_DUMMY_ONLY || USE_HYBRID_BACKEND) return true

        return try {
            val response = withTimeoutOrNull(Constants.HYBRID_TIMEOUT_MS) {
                apiService.healthCheck()
            }
            response != null && response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Simpan request yang gagal ke Room DB.
     */
    private suspend fun logFailedRequest(request: AudioFeatureRequest, error: String) {
        try {
            failedRequestDao.insert(
                FailedRequestEntity(
                    timestamp = System.currentTimeMillis(),
                    payload = gson.toJson(request),
                    errorMessage = error,
                    retryCount = Constants.MAX_RETRY
                )
            )
            Log.d(TAG, "Failed request logged to Room DB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log request to Room DB", e)
        }
    }

    // ══════════════════════════════════════════════
    //  DUMMY BACKEND — Hapus saat backend siap
    // ══════════════════════════════════════════════

    /**
     * Simulasi response backend dengan threshold rules lokal.
     * Logic sama dengan yang akan dijalankan backend asli.
     */
    private fun getDummyClassification(features: AudioFeatures): ClassificationResponse {
        var isAnomaly = false
        var confidence = 0f
        var label = "normal"

        // Rule 1: Suara impulsif (benda pecah)
        if (features.peakAmplitude >= 0.70f) {
            isAnomaly = true
            confidence = features.peakAmplitude.coerceAtMost(1f)
            label = "crash"
        }
        // Rule 2: Teriakan (RMS tinggi + ZCR tinggi)
        else if (features.rms >= 0.35f && features.zcr >= 0.15f) {
            isAnomaly = true
            confidence = ((features.rms + features.zcr) / 2).coerceAtMost(1f)
            label = "scream"
        }
        // Rule 3: Suara sangat keras
        else if (features.rms >= 0.55f) {
            isAnomaly = true
            confidence = features.rms.coerceAtMost(1f)
            label = "loud_noise"
        }
        // Normal
        else {
            confidence = 1f - features.rms
            label = "normal"
        }

        // Sliding window simulasi
        if (isAnomaly) {
            dummyAnomalyCount++
        } else {
            dummyAnomalyCount = 0
        }

        val isEscalating = dummyAnomalyCount >= 3

        return ClassificationResponse(
            isAnomaly = isAnomaly,
            confidence = confidence,
            label = label,
            isEscalating = isEscalating,
            consecutiveAnomalies = dummyAnomalyCount,
            message = "[DUMMY] Local classification"
        )
    }

    /** Reset sliding window dummy (saat alert dibatalkan) */
    fun resetDummyTracker() {
        dummyAnomalyCount = 0
    }
}
