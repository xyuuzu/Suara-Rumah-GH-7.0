package com.example.suararumah.audio

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Kalkulator fitur audio on-device.
 * Semua kalkulasi dilakukan secara native Kotlin tanpa library DSP/ML tambahan.
 * Fitur yang diekstraksi:
 * - RMS (Root Mean Square): indikator loudness
 * - ZCR (Zero-Crossing Rate): karakter suara (teriakan vs percakapan normal)
 * - Peak Amplitude: deteksi suara impulsif (benda pecah)
 */
object AudioFeatureExtractor {

    /**
     * Hitung RMS (Root Mean Square) dari buffer audio.
     * RMS merepresentasikan "energi" atau loudness rata-rata sinyal.
     * Nilai tinggi = suara keras (teriakan, benda pecah).
     *
     * @param buffer Array of short audio samples (PCM 16-bit)
     * @return RMS value (0.0 - 1.0 dinormalisasi ke Short.MAX_VALUE)
     */
    fun calculateRMS(buffer: ShortArray): Float {
        if (buffer.isEmpty()) return 0f

        var sumOfSquares = 0.0
        for (sample in buffer) {
            val normalized = sample.toDouble() / Short.MAX_VALUE
            sumOfSquares += normalized * normalized
        }
        return sqrt(sumOfSquares / buffer.size).toFloat()
    }

    /**
     * Hitung Zero-Crossing Rate dari buffer audio.
     * ZCR tinggi → suara berisik/noise/teriakan.
     * ZCR rendah → suara tonal/percakapan normal.
     *
     * @param buffer Array of short audio samples
     * @return ZCR (0.0 - 1.0, proporsi zero crossings terhadap total samples)
     */
    fun calculateZCR(buffer: ShortArray): Float {
        if (buffer.size < 2) return 0f

        var crossings = 0
        for (i in 1 until buffer.size) {
            // Zero crossing terjadi saat tanda berubah (positif ke negatif atau sebaliknya)
            if ((buffer[i] >= 0 && buffer[i - 1] < 0) ||
                (buffer[i] < 0 && buffer[i - 1] >= 0)
            ) {
                crossings++
            }
        }
        return crossings.toFloat() / (buffer.size - 1)
    }

    /**
     * Hitung Peak Amplitude (nilai absolut tertinggi) dari buffer.
     * Berguna untuk mendeteksi suara impulsif (benda pecah/jatuh).
     *
     * @param buffer Array of short audio samples
     * @return Peak amplitude (0.0 - 1.0 dinormalisasi)
     */
    fun calculatePeakAmplitude(buffer: ShortArray): Float {
        if (buffer.isEmpty()) return 0f

        var maxAbs = 0
        for (sample in buffer) {
            val absVal = abs(sample.toInt())
            if (absVal > maxAbs) {
                maxAbs = absVal
            }
        }
        return maxAbs.toFloat() / Short.MAX_VALUE
    }

    /**
     * Ekstraksi semua fitur sekaligus dari buffer audio.
     * Digunakan sebagai payload yang dikirim ke backend.
     */
    fun extractFeatures(buffer: ShortArray): AudioFeatures {
        return AudioFeatures(
            rms = calculateRMS(buffer),
            zcr = calculateZCR(buffer),
            peakAmplitude = calculatePeakAmplitude(buffer),
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Data class untuk menyimpan hasil ekstraksi fitur audio.
 */
data class AudioFeatures(
    val rms: Float,
    val zcr: Float,
    val peakAmplitude: Float,
    val timestamp: Long
)
