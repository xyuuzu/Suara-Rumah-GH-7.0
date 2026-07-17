package com.example.suararumah.service

import android.os.CountDownTimer
import com.example.suararumah.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine untuk grace period.
 */
enum class GracePeriodState {
    /** Tidak ada grace period aktif */
    IDLE,
    /** Countdown sedang berjalan — menunggu pembatalan dari user */
    COUNTING_DOWN,
    /** User berhasil membatalkan sebelum timer habis */
    CANCELLED,
    /** Timer habis tanpa pembatalan — alert harus dikirim */
    TRIGGERED
}

/**
 * Manager grace period antara deteksi anomali eskalatif dan pengiriman alert.
 *
 * Flow (dari PRD Bagian 5):
 * 1. Anomali eskalatif terdeteksi → start grace period + vibrate detection pattern
 * 2. Selama countdown: user bisa cancel via volume button atau tombol "Aman"
 * 3a. Jika cancel → state = CANCELLED, vibrate cancellation, reset
 * 3b. Jika timer habis → state = TRIGGERED, alert pipeline dipanggil
 *
 * Durasi configurable:
 * - Demo: 10 detik (untuk presentasi ke juri)
 * - Produksi: 60 detik
 */
class GracePeriodManager {

    companion object {
        val globalState: MutableStateFlow<GracePeriodState> = MutableStateFlow(GracePeriodState.IDLE)
        var globalManager: GracePeriodManager? = null
    }

    init {
        globalManager = this
    }

    private val _state = MutableStateFlow(GracePeriodState.IDLE)
    val state: StateFlow<GracePeriodState> = _state.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    /** Callback saat grace period habis tanpa pembatalan */
    var onTriggered: (() -> Unit)? = null

    /** Callback saat grace period dimulai (untuk trigger vibration) */
    var onStarted: (() -> Unit)? = null

    /** Callback saat grace period dibatalkan (untuk trigger vibration) */
    var onCancelled: (() -> Unit)? = null

    /**
     * Mulai grace period countdown.
     * Durasi otomatis berdasarkan Constants.IS_DEMO_MODE.
     */
    fun start() {
        if (_state.value == GracePeriodState.COUNTING_DOWN) return // Sudah berjalan

        val duration = if (Constants.IS_DEMO_MODE) {
            Constants.GRACE_PERIOD_DEMO_MS
        } else {
            Constants.GRACE_PERIOD_PROD_MS
        }

        _state.value = GracePeriodState.COUNTING_DOWN
        globalState.value = GracePeriodState.COUNTING_DOWN
        _remainingSeconds.value = duration / 1000

        onStarted?.invoke()

        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = (millisUntilFinished / 1000) + 1
            }

            override fun onFinish() {
                _remainingSeconds.value = 0
                _state.value = GracePeriodState.TRIGGERED
                globalState.value = GracePeriodState.TRIGGERED
                onTriggered?.invoke()
            }
        }.start()
    }

    /**
     * Batalkan grace period — dipanggil saat user menekan volume atau tombol "Aman".
     */
    fun cancel() {
        if (_state.value != GracePeriodState.COUNTING_DOWN) return

        countDownTimer?.cancel()
        countDownTimer = null
        _remainingSeconds.value = 0
        _state.value = GracePeriodState.CANCELLED
        globalState.value = GracePeriodState.CANCELLED

        onCancelled?.invoke()
    }

    /**
     * Reset state ke IDLE — dipanggil setelah alert terkirim atau setelah cancel.
     */
    fun reset() {
        countDownTimer?.cancel()
        countDownTimer = null
        _remainingSeconds.value = 0
        _state.value = GracePeriodState.IDLE
        globalState.value = GracePeriodState.IDLE
    }

    /**
     * Apakah grace period sedang aktif (countdown berjalan).
     */
    fun isActive(): Boolean = _state.value == GracePeriodState.COUNTING_DOWN
}
