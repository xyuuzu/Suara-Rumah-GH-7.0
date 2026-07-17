package com.example.suararumah.audio

import android.media.AudioFormat
import android.media.AudioRecord

/**
 * Konstanta konfigurasi audio untuk AudioRecord.
 * Semua parameter disesuaikan untuk deteksi anomali akustik
 * tanpa library DSP/ML tambahan.
 */
object AudioConfig {
    // Sample rate standar — cukup untuk menangkap frekuensi suara manusia (300-3400 Hz)
    const val SAMPLE_RATE = 44100

    // Mono — cukup untuk analisis amplitudo/frekuensi dasar
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

    // 16-bit PCM — kedalaman standar untuk analisis audio
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    // Durasi rolling buffer dalam detik
    const val BUFFER_DURATION_SEC = 5

    // Interval ekstraksi fitur dalam milidetik — dipercepat ke 150ms agar grafik & deteksi sangat responsif
    const val EXTRACTION_INTERVAL_MS = 150L

    /**
     * Hitung ukuran buffer minimum yang diperlukan AudioRecord.
     * Menggunakan nilai yang lebih besar antara minimum system dan kebutuhan durasi kita.
     */
    fun getMinBufferSize(): Int {
        val minSystemBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        val desiredBuffer = SAMPLE_RATE * BUFFER_DURATION_SEC * 2 // 2 bytes per sample (16-bit)
        return maxOf(minSystemBuffer, desiredBuffer)
    }

    /**
     * Jumlah sample per detik (untuk kalkulasi array size).
     */
    fun samplesPerSecond(): Int = SAMPLE_RATE

    /**
     * Jumlah sample per pembacaan chunk (~150ms) agar audio responsif seketika.
     */
    fun chunkSamples(): Int = (SAMPLE_RATE * (EXTRACTION_INTERVAL_MS / 1000f)).toInt()

    /**
     * Total sample dalam rolling buffer.
     */
    fun totalBufferSamples(): Int = SAMPLE_RATE * BUFFER_DURATION_SEC
}
