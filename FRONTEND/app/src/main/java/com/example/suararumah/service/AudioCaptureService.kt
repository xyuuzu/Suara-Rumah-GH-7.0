package com.example.suararumah.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioRecord
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.suararumah.MainActivity
import com.example.suararumah.R
import com.example.suararumah.SuaraRumahApp
import com.example.suararumah.audio.AudioConfig
import com.example.suararumah.audio.AudioFeatureExtractor
import com.example.suararumah.audio.AudioFeatures
import com.example.suararumah.util.VibrationHelper
import com.example.suararumah.util.hasMicrophonePermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground Service untuk menangkap audio ambient secara pasif.
 * Dilengkapi MediaSession global volume interceptor saat Grace Period aktif.
 */
class AudioCaptureService : Service() {

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _latestFeatures = MutableStateFlow<AudioFeatures?>(null)
        val latestFeatures: StateFlow<AudioFeatures?> = _latestFeatures.asStateFlow()

        private val _rmsHistory = MutableStateFlow<List<Float>>(emptyList())
        val rmsHistory: StateFlow<List<Float>> = _rmsHistory.asStateFlow()
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var stateObserverJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // MediaSession untuk menangkap tombol volume secara global (bahkan saat di background/lockscreen)
    private var mediaSession: MediaSession? = null

    // Rolling buffer untuk menyimpan audio samples terbaru
    private val rollingBuffer = ShortArray(AudioConfig.totalBufferSamples())
    private var bufferWritePosition = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        setupMediaSession()
        observeGracePeriodState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopCapture()
                stopSelf()
            }
            else -> {
                if (!hasMicrophonePermission(this)) {
                    Log.e(TAG, "Izin mikrofon belum diberikan; menghentikan service")
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (!startForegroundWithNotification()) {
                    return START_NOT_STICKY
                }

                startCapture()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        stateObserverJob?.cancel()
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    /**
     * Setup MediaSession & VolumeProvider agar saat anomali terdeteksi,
     * tombol volume fisik di HP langsung membatalkan darurat meskipun aplikasi di latar belakang/locked.
     */
    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(this, "SuaraRumahVolumeInterceptor").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                val stateBuilder = PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                    .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                setPlaybackState(stateBuilder.build())

                val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 10, 5) {
                    override fun onAdjustVolume(direction: Int) {
                        Log.d(TAG, "Global volume key pressed! Direction: $direction")
                        if (GracePeriodManager.globalState.value == GracePeriodState.COUNTING_DOWN) {
                            GracePeriodManager.globalManager?.cancel()
                            VibrationHelper.vibrateCancellation(this@AudioCaptureService)
                        }
                    }
                }
                setPlaybackToRemote(volumeProvider)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menginisialisasi MediaSession global", e)
        }
    }

    /**
     * Pantau perubahan GracePeriodManager.globalState:
     * - Jika COUNTING_DOWN (Anomali terdeteksi): aktifkan MediaSession & tampilkan notifikasi darurat (IMPORTANCE_HIGH).
     * - Jika IDLE/CANCELLED: matikan MediaSession (volume HP normal kembali) & kembalikan notifikasi senyap.
     */
    private fun observeGracePeriodState() {
        stateObserverJob = serviceScope.launch {
            GracePeriodManager.globalState.collect { state ->
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (state == GracePeriodState.COUNTING_DOWN) {
                    Log.d(TAG, "Anomali terdeteksi! Mengaktifkan MediaSession volume interceptor & notifikasi darurat")
                    mediaSession?.isActive = true
                    notificationManager.notify(NOTIFICATION_ID, buildAlertNotification())
                } else {
                    mediaSession?.isActive = false
                    if (_isRunning.value) {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification())
                    }
                }
            }
        }
    }

    private fun startForegroundWithNotification(): Boolean {
        val notification = buildNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Gagal masuk foreground dengan type microphone", e)
            stopSelf()
            false
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SuaraRumahApp.CHANNEL_ID_MONITORING)
            .setContentTitle("Memantau Aktif")
            .setContentText("Suara Rumah sedang memantau audio ambient secara pasif")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun buildAlertNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SuaraRumahApp.CHANNEL_ID_ALERT)
            .setContentTitle("🚨 ANOMALI SUARA TERDETEKSI!")
            .setContentText("Tekan tombol Volume Fisik atau buka aplikasi sekarang untuk Batal!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    @Suppress("MissingPermission") // Permission dicek sebelum start service
    private fun startCapture() {
        if (_isRunning.value) return

        if (!hasMicrophonePermission(this)) {
            Log.e(TAG, "Izin mikrofon hilang sebelum capture dimulai")
            stopSelf()
            return
        }

        try {
            val bufferSize = AudioConfig.getMinBufferSize()
            audioRecord = AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                AudioConfig.SAMPLE_RATE,
                AudioConfig.CHANNEL_CONFIG,
                AudioConfig.AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord gagal diinisialisasi")
                stopSelf()
                return
            }

            audioRecord?.startRecording()
            _isRunning.value = true

            captureJob = serviceScope.launch {
                val readBuffer = ShortArray(AudioConfig.chunkSamples()) // ~150ms data responsif
                var rmsHistoryList = mutableListOf<Float>()

                while (isActive && _isRunning.value) {
                    val readCount = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1

                    if (readCount > 0) {
                        // Tulis ke rolling buffer
                        synchronized(rollingBuffer) {
                            for (i in 0 until readCount) {
                                rollingBuffer[bufferWritePosition % rollingBuffer.size] = readBuffer[i]
                                bufferWritePosition++
                            }
                        }

                        // Ekstraksi fitur dari data yang baru dibaca
                        val features = AudioFeatureExtractor.extractFeatures(
                            readBuffer.copyOf(readCount)
                        )
                        _latestFeatures.value = features

                        // Simpan RMS history untuk grafik (maks 60 data points)
                        rmsHistoryList.add(features.rms)
                        if (rmsHistoryList.size > 60) {
                            rmsHistoryList = rmsHistoryList.drop(1).toMutableList()
                        }
                        _rmsHistory.value = rmsHistoryList.toList()
                    }

                    // Interval antar pembacaan cepat 150ms
                    delay(AudioConfig.EXTRACTION_INTERVAL_MS)
                }
            }

            Log.d(TAG, "Audio capture dimulai responsif 150ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error memulai audio capture", e)
            _isRunning.value = false
            stopSelf()
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error menghentikan AudioRecord", e)
        }
        audioRecord = null
        _isRunning.value = false
        bufferWritePosition = 0

        Log.d(TAG, "Audio capture dihentikan")
    }
}
