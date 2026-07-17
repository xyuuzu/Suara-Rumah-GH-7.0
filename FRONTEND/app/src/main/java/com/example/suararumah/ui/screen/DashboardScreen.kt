package com.example.suararumah.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.suararumah.service.GracePeriodState
import com.example.suararumah.ui.component.PermissionConsentDialog
import com.example.suararumah.ui.component.AudioChart
import com.example.suararumah.ui.component.SafeButton
import com.example.suararumah.ui.component.StatusIndicator
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary
import com.example.suararumah.util.hasMicrophonePermission
import com.example.suararumah.viewmodel.ContactViewModel
import com.example.suararumah.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard utama Suara Rumah — terhubung penuh dengan DashboardViewModel dan ContactViewModel.
 *
 * Layout:
 * 1. StatusIndicator (atas) & Toggle mode Aman (start/stop monitoring)
 * 2. Banner Grace Period (jika countdown aktif)
 * 3. Card grafik audio real-time (AudioChart)
 * 4. Card info fitur audio terakhir (RMS, ZCR, Peak, Label Klasifikasi)
 * 5. Card kontak darurat (tap untuk navigasi ke SetupContactScreen)
 * 6. Card histori alert (tap untuk navigasi ke AlertHistoryScreen)
 * 7. SafeButton (bawah) — tombol "Aman" dual-state untuk cancel grace period atau info aman
 */
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    contactViewModel: ContactViewModel,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val isMonitoring by dashboardViewModel.isMonitoring.collectAsState()
    val rmsHistory by dashboardViewModel.rmsHistory.collectAsState()
    val latestFeatures by dashboardViewModel.latestFeatures.collectAsState()
    val lastClassification by dashboardViewModel.lastClassification.collectAsState()
    val isAnomalyDetected by dashboardViewModel.isAnomalyDetected.collectAsState()
    val gracePeriodState by dashboardViewModel.gracePeriodState.collectAsState()
    val remainingSeconds by dashboardViewModel.gracePeriodRemainingSeconds.collectAsState()
    val statusMessage by dashboardViewModel.statusMessage.collectAsState()

    val contacts by contactViewModel.contacts.collectAsState()
    val alertHistory by dashboardViewModel.alertHistory.collectAsState()
    val context = LocalContext.current
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dashboardViewModel.toggleMonitoring(true)
        } else {
            dashboardViewModel.handleMicrophonePermissionDenied()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            dashboardViewModel.clearStatusMessage()
        }
    }

    fun startMonitoringIfAllowed() {
        if (hasMicrophonePermission(context)) {
            dashboardViewModel.toggleMonitoring(true)
        } else {
            showMicPermissionDialog = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Header: Status + Toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(isMonitoring = isMonitoring)

                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { checked ->
                        if (checked) {
                            startMonitoringIfAllowed()
                        } else {
                            dashboardViewModel.toggleMonitoring(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Judul Halaman ──
            Text(
                text = "Status Pemantauan",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isMonitoring)
                    "Suara Rumah sedang mendengarkan pola suara ambient di sekitar Anda."
                else
                    "Aktifkan toggle di atas untuk memulai pemantauan.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Banner Peringatan Grace Period ──
            if (gracePeriodState == GracePeriodState.COUNTING_DOWN) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertDanger.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ ANOMALI SUARA ESKALATIF TERDETEKSI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AlertDanger
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Peringatan darurat akan dikirim dalam $remainingSeconds detik.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tekan tombol volume fisik atau tombol AMAN di bawah untuk membatalkan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Card: Grafik Audio Real-time ──
            AudioChart(
                dataPoints = rmsHistory,
                isAnomalyDetected = isAnomalyDetected || gracePeriodState == GracePeriodState.COUNTING_DOWN
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Card: Info Fitur Terakhir ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Data Terakhir",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (lastClassification != null) {
                            val labelColor = if (lastClassification!!.hasAnomaly) AlertDanger else Primary
                            Text(
                                text = lastClassification!!.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = labelColor,
                                modifier = Modifier
                                    .background(labelColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val features = latestFeatures
                    if (features != null) {
                        FeatureRow(label = "RMS (Loudness)", value = String.format(Locale.US, "%.3f", features.rms))
                        FeatureRow(label = "Zero-Crossing Rate", value = String.format(Locale.US, "%.3f", features.zcr))
                        FeatureRow(label = "Peak Amplitude", value = String.format(Locale.US, "%.3f", features.peakAmplitude))
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date(features.timestamp))
                        FeatureRow(label = "Terakhir diproses", value = timeStr)
                    } else {
                        FeatureRow(label = "RMS (Loudness)", value = "—")
                        FeatureRow(label = "Zero-Crossing Rate", value = "—")
                        FeatureRow(label = "Peak Amplitude", value = "—")
                        FeatureRow(label = "Terakhir diproses", value = "—")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Card: Kontak Darurat (tap untuk navigasi) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToContacts() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kontak Darurat",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (contacts.isEmpty()) {
                            Text(
                                text = "Belum ada kontak. Tap untuk menambahkan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            contacts.take(3).forEach { contact ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Primary.copy(alpha = 0.1f))
                                            .padding(2.dp),
                                        tint = Primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${contact.name} · ${contact.phoneNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (contacts.size > 3) {
                                Text(
                                    text = "+${contacts.size - 3} lainnya",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Kelola kontak",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Card: Histori Alert (tap untuk navigasi) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHistory() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f))
                            .padding(8.dp),
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Histori Alert (${alertHistory.size})",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = if (alertHistory.isEmpty()) "Belum ada insiden terdeteksi"
                            else "Terakhir: ${alertHistory.first().label} (${alertHistory.first().status})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Lihat histori",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Panel Uji Coba / Simulasi (Tahap 5 Demo Mode) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧪",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Simulasi Audio (Demo Fase 5)",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Uji 4 pola suara tanpa mikrofon riil / backend",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dashboardViewModel.simulateAudioClip("normal") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                        ) {
                            Text("💬 Normal", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { dashboardViewModel.simulateAudioClip("scream") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertDanger)
                        ) {
                            Text("🗣️ Teriakan", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dashboardViewModel.simulateAudioClip("crash") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertDanger)
                        ) {
                            Text("💥 Pecah/Crash", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { dashboardViewModel.simulateAudioClip("escalation") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertDanger)
                        ) {
                            Text("🚨 Eskalatif (3x)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SafeButton (selalu ada di layar utama) ──
            SafeButton(
                isGracePeriodActive = gracePeriodState == GracePeriodState.COUNTING_DOWN,
                onClick = {
                    if (gracePeriodState == GracePeriodState.COUNTING_DOWN) {
                        dashboardViewModel.cancelGracePeriod()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(innerPadding)
        )

        if (showMicPermissionDialog) {
            PermissionConsentDialog(
                permissions = listOf(Manifest.permission.RECORD_AUDIO),
                onConfirm = {
                    showMicPermissionDialog = false
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onDismiss = {
                    showMicPermissionDialog = false
                    dashboardViewModel.handleMicrophonePermissionDenied()
                }
            )
        }
    }
}

/**
 * Row untuk menampilkan label-value fitur audio.
 */
@Composable
private fun FeatureRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
