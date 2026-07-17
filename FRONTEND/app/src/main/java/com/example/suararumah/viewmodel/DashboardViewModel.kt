package com.example.suararumah.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.suararumah.util.hasMicrophonePermission
import com.example.suararumah.audio.AudioFeatures
import com.example.suararumah.data.remote.dto.ClassificationResponse
import com.example.suararumah.data.repository.AudioRepository
import com.example.suararumah.data.repository.ContactRepository
import com.example.suararumah.service.AudioCaptureService
import com.example.suararumah.service.GracePeriodManager
import com.example.suararumah.service.GracePeriodState
import com.example.suararumah.util.VibrationHelper
import com.example.suararumah.util.VolumeButtonInterceptor
import com.example.suararumah.util.EmergencyAlertHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Data class untuk alert yang sudah terkirim (histori lokal).
 */
data class AlertRecord(
    val id: String,
    val timestamp: Long,
    val status: String, // "sent", "cancelled", "false_alarm"
    val label: String   // "scream", "crash", dll
)

/**
 * ViewModel utama Dashboard — mengorkestrasikan:
 * - Monitoring state (service on/off)
 * - Audio data (RMS history, fitur terakhir)
 * - Grace period (start, cancel, trigger)
 * - Klasifikasi (via AudioRepository — dummy atau real)
 * - Volume button interceptor wiring
 * - Alert history
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private val contactRepository = ContactRepository(application)
    private val gracePeriodManager = GracePeriodManager()

    // ── Monitoring State ──
    val isMonitoring: StateFlow<Boolean> = AudioCaptureService.isRunning

    // ── Audio Data ──
    val rmsHistory: StateFlow<List<Float>> = AudioCaptureService.rmsHistory
    val latestFeatures: StateFlow<AudioFeatures?> = AudioCaptureService.latestFeatures

    // ── Grace Period ──
    val gracePeriodState: StateFlow<GracePeriodState> = gracePeriodManager.state
    val gracePeriodRemainingSeconds: StateFlow<Long> = gracePeriodManager.remainingSeconds

    // ── Classification ──
    private val _lastClassification = MutableStateFlow<ClassificationResponse?>(null)
    val lastClassification: StateFlow<ClassificationResponse?> = _lastClassification.asStateFlow()

    private val _isAnomalyDetected = MutableStateFlow(false)
    val isAnomalyDetected: StateFlow<Boolean> = _isAnomalyDetected.asStateFlow()

    // ── Alert History (lokal) ──
    private val _alertHistory = MutableStateFlow<List<AlertRecord>>(emptyList())
    val alertHistory: StateFlow<List<AlertRecord>> = _alertHistory.asStateFlow()

    // ── Status Messages ──
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        setupGracePeriodCallbacks()
        observeAudioFeatures()
    }

    // ══════════════════════════════════════════════
    //  Monitoring Control
    // ══════════════════════════════════════════════

    /**
     * Start/stop audio monitoring service.
     */
    fun toggleMonitoring(enabled: Boolean = !isMonitoring.value) {
        val context = getApplication<Application>()
        val intent = Intent(context, AudioCaptureService::class.java)

        if (enabled) {
            if (!hasMicrophonePermission(context)) {
                _statusMessage.value = "Izin mikrofon diperlukan untuk memulai pemantauan"
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            intent.action = "STOP"
            context.startService(intent)
            // Reset semua state
            _isAnomalyDetected.value = false
            _lastClassification.value = null
            gracePeriodManager.reset()
            VolumeButtonInterceptor.deactivate()
        }
    }

    fun handleMicrophonePermissionDenied() {
        _statusMessage.value = "Pemantauan tidak bisa dimulai tanpa izin mikrofon"
    }

    // ══════════════════════════════════════════════
    //  Audio Feature → Classification Pipeline
    // ══════════════════════════════════════════════

    /**
     * Observe fitur audio dari service, kirim ke repository untuk klasifikasi.
     */
    private fun observeAudioFeatures() {
        viewModelScope.launch {
            latestFeatures.collect { features ->
                if (features != null && isMonitoring.value) {
                    processFeatures(features)
                }
            }
        }
    }

    /**
     * Proses fitur: klasifikasi → cek eskalasi → mulai grace period jika perlu.
     */
    private suspend fun processFeatures(features: AudioFeatures) {
        val result = audioRepository.analyzeAudio(features) ?: return

        _lastClassification.value = result
        _isAnomalyDetected.value = result.hasAnomaly || result.isEscalating

        // Jika eskalatif dan grace period belum jalan → mulai
        if (result.isEscalating && gracePeriodManager.state.value == GracePeriodState.IDLE) {
            gracePeriodManager.start()
        }
    }

    // ══════════════════════════════════════════════
    //  Simulasi Uji Coba (Fase 5 / Demo Mode)
    // ══════════════════════════════════════════════

    /**
     * Simulasi 4 jenis klip audio sesuai PRD & Task Fase 5:
     * 1. Normal
     * 2. Teriakan (Scream)
     * 3. Benda Pecah (Crash)
     * 4. Eskalatif (3x Anomali Berturut-turut)
     */
    fun simulateAudioClip(type: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            when (type) {
                "normal" -> {
                    audioRepository.resetDummyTracker()
                    val feat = AudioFeatures(rms = 0.05f, zcr = 0.04f, peakAmplitude = 0.12f, timestamp = now)
                    processFeatures(feat)
                    _statusMessage.value = "🧪 Simulasi: Suara Normal"
                }
                "scream" -> {
                    val feat = AudioFeatures(rms = 0.42f, zcr = 0.22f, peakAmplitude = 0.55f, timestamp = now)
                    processFeatures(feat)
                    _statusMessage.value = "🧪 Simulasi: Teriakan (Scream Terdeteksi)"
                }
                "crash" -> {
                    val feat = AudioFeatures(rms = 0.28f, zcr = 0.12f, peakAmplitude = 0.85f, timestamp = now)
                    processFeatures(feat)
                    _statusMessage.value = "🧪 Simulasi: Benda Pecah (Crash Terdeteksi)"
                }
                "escalation" -> {
                    // Kirim 3 anomali berturut-turut untuk memicu isEscalating = true -> Grace Period
                    for (i in 1..3) {
                        val feat = AudioFeatures(rms = 0.45f, zcr = 0.25f, peakAmplitude = 0.60f, timestamp = now + (i * 1000))
                        processFeatures(feat)
                    }
                    _statusMessage.value = "🧪 Simulasi: Anomali Eskalatif (Grace Period Dipicu!)"
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    //  Grace Period
    // ══════════════════════════════════════════════

    private fun setupGracePeriodCallbacks() {
        val context = getApplication<Application>()

        gracePeriodManager.onStarted = {
            // Vibrate detection pattern
            VibrationHelper.vibrateDetection(context)
            // Activate volume button interceptor
            VolumeButtonInterceptor.activate { cancelGracePeriod() }
            _statusMessage.value = "⚠️ Anomali terdeteksi — batalkan jika aman"
        }

        gracePeriodManager.onCancelled = {
            // Vibrate cancellation pattern
            VibrationHelper.vibrateCancellation(context)
            // Deactivate volume button
            VolumeButtonInterceptor.deactivate()
            // Reset anomaly tracker
            audioRepository.resetDummyTracker()
            _isAnomalyDetected.value = false
            _statusMessage.value = "Peringatan dibatalkan"

            // Log ke history
            addAlertRecord("cancelled", _lastClassification.value?.label ?: "unknown")
        }

        gracePeriodManager.onTriggered = {
            // Grace period habis — kirim alert!
            VolumeButtonInterceptor.deactivate()
            _statusMessage.value = "Peringatan darurat dikirim ke kontak darurat"

            addAlertRecord("sent", _lastClassification.value?.label ?: "unknown")
            sendEmergencyAlert()
        }
    }

    /**
     * Batalkan grace period — dipanggil dari tombol "Aman" atau volume button.
     */
    fun cancelGracePeriod() {
        gracePeriodManager.cancel()
    }

    /**
     * Reset grace period ke IDLE setelah handled.
     */
    fun resetGracePeriod() {
        gracePeriodManager.reset()
    }

    /**
     * Batalkan grace period — alias untuk tombol Aman di antarmuka.
     */
    fun cancelAlert() {
        cancelGracePeriod()
    }

    /**
     * ── Simulasi Audio untuk Pengujian / Demo Mode Fase 5 ──
     */
    fun simulateNormalAudio() {
        _isAnomalyDetected.value = false
        _lastClassification.value = ClassificationResponse(isAnomaly = false, confidence = 0.95f, label = "normal")
        gracePeriodManager.reset()
        _statusMessage.value = "Simulasi: Suara normal terdeteksi"
    }

    fun simulateScreamAudio() {
        _isAnomalyDetected.value = true
        _lastClassification.value = ClassificationResponse(isAnomaly = true, confidence = 0.88f, label = "scream")
        gracePeriodManager.start()
        _statusMessage.value = "Simulasi: Anomali teriakan terdeteksi (Masa tenggat aktif)"
    }

    fun simulateCrashAudio() {
        _isAnomalyDetected.value = true
        _lastClassification.value = ClassificationResponse(isAnomaly = true, confidence = 0.92f, label = "crash")
        gracePeriodManager.start()
        _statusMessage.value = "Simulasi: Suara bantingan terdeteksi (Masa tenggat aktif)"
    }

    fun simulateEscalatingAnomaly() {
        _isAnomalyDetected.value = true
        _lastClassification.value = ClassificationResponse(
            isAnomaly = true,
            confidence = 0.98f,
            label = "escalating",
            isEscalating = true,
            alertId = "alert-demo-${System.currentTimeMillis()}"
        )
        gracePeriodManager.start()
        _statusMessage.value = "Simulasi: Pola suara eskalatif SOS terdeteksi!"
    }

    // ══════════════════════════════════════════════
    //  Alert Pipeline
    // ══════════════════════════════════════════════

    private fun sendEmergencyAlert() {
        viewModelScope.launch {
            val phones = contactRepository.getAllPhoneNumbers()
            if (phones.isEmpty()) {
                _statusMessage.value = "Tidak ada kontak darurat terdaftar"
            } else {
                _statusMessage.value = "Peringatan dikirim ke ${phones.size} kontak beserta SMS SOS"
                // Kirim SMS darurat & koordinat GPS melalui EmergencyAlertHelper
                EmergencyAlertHelper.sendEmergencyAlert(getApplication(), phones)
            }
            // Reset setelah beberapa saat
            gracePeriodManager.reset()
        }
    }

    /**
     * Laporkan alarm palsu — kirim pesan susulan ke kontak darurat.
     */
    fun reportFalseAlarm(alertId: String? = null) {
        viewModelScope.launch {
            val phones = contactRepository.getAllPhoneNumbers()
            if (phones.isNotEmpty()) {
                EmergencyAlertHelper.sendFalseAlarmSms(getApplication(), phones)
            }
            _statusMessage.value = "Pesan kondisi aman telah dikirim ke kontak darurat via SMS"
            audioRepository.resetDummyTracker()
            _isAnomalyDetected.value = false

            // Update alert status di history
            val updated = _alertHistory.value.toMutableList()
            if (alertId != null) {
                val idx = updated.indexOfFirst { it.id == alertId }
                if (idx != -1) {
                    updated[idx] = updated[idx].copy(status = "false_alarm")
                }
            } else if (updated.isNotEmpty() && updated.first().status == "sent") {
                updated[0] = updated.first().copy(status = "false_alarm")
            }
            _alertHistory.value = updated
        }
    }

    fun reportFalseAlarm(alert: AlertRecord) {
        reportFalseAlarm(alert.id)
    }

    // ══════════════════════════════════════════════
    //  Alert History
    // ══════════════════════════════════════════════

    private fun addAlertRecord(status: String, label: String) {
        val record = AlertRecord(
            id = "alert-${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            status = status,
            label = label
        )
        _alertHistory.value = listOf(record) + _alertHistory.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        VolumeButtonInterceptor.deactivate()
        gracePeriodManager.reset()
    }
}
