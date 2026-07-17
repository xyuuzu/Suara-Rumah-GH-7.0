package com.example.suararumah.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Helper untuk pola getaran.
 *
 * Dari GuideStyle & PRD:
 * - Deteksi anomali: 2 getar pendek + 1 panjang (supaya user tahu tanpa lihat layar)
 * - Konfirmasi pembatalan: 1 getar pendek (beda pola dari deteksi)
 */
object VibrationHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }

    /**
     * Getaran deteksi anomali: 2 pendek + 1 panjang.
     * Pattern: [delay, vibrate, pause, vibrate, pause, vibrate]
     *          [0,     100,     100,   100,     100,   400    ]
     */
    fun vibrateDetection(context: Context) {
        vibrate(context, Constants.VIBRATION_PATTERN_DETECTION)
    }

    /**
     * Getaran konfirmasi pembatalan: 1 pendek.
     * Pattern: [delay, vibrate]
     *          [0,     100    ]
     */
    fun vibrateCancellation(context: Context) {
        vibrate(context, Constants.VIBRATION_PATTERN_CANCELLATION)
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1) // -1 = no repeat
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
