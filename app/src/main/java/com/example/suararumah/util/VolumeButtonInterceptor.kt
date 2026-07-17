package com.example.suararumah.util

import android.view.KeyEvent

/**
 * Interceptor untuk tombol volume fisik.
 *
 * Desain:
 * - Hanya aktif selama grace period berjalan (bukan terus-menerus)
 * - Menangkap VOLUME_UP atau VOLUME_DOWN sebagai sinyal pembatalan
 * - Di luar grace period, tombol volume berfungsi normal
 *
 * Catatan SPIKE (Fase 1):
 * - Kondisi (a) App di foreground: ditangkap via Activity.dispatchKeyEvent() — WORKS
 * - Kondisi (b) App di background: membutuhkan MediaSession — SPIKE REQUIRED
 *   Jika MediaSession terlalu kompleks, fallback ke foreground-only
 */
object VolumeButtonInterceptor {

    /** Apakah interceptor sedang aktif (hanya saat grace period) */
    @Volatile
    var isActive: Boolean = false
        private set

    /** Callback yang dipanggil saat tombol volume ditekan selama grace period */
    var onVolumePressed: (() -> Unit)? = null

    /**
     * Aktifkan interceptor — panggil saat grace period dimulai.
     */
    fun activate(callback: () -> Unit) {
        onVolumePressed = callback
        isActive = true
    }

    /**
     * Nonaktifkan interceptor — panggil saat grace period berakhir atau dibatalkan.
     */
    fun deactivate() {
        isActive = false
        onVolumePressed = null
    }

    /**
     * Proses key event dari Activity.dispatchKeyEvent().
     * Return true jika event dikonsumsi (tombol volume ditangkap),
     * false jika harus diteruskan ke behavior default.
     *
     * @param event KeyEvent dari dispatchKeyEvent
     * @return true jika event ditangkap, false jika diteruskan
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!isActive) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    onVolumePressed?.invoke()
                    return true // Konsumsi event, volume tidak berubah
                }
            }
        }

        // Untuk ACTION_UP dari volume keys saat aktif, tetap konsumsi
        // agar tidak trigger volume change
        if (event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN -> return true
            }
        }

        return false
    }
}
