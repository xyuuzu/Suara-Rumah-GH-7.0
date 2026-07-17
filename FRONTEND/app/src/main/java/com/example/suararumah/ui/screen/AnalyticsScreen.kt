package com.example.suararumah.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.suararumah.ui.component.AudioChart
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.AlertWarning
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary
import com.example.suararumah.viewmodel.DashboardViewModel

/**
 * Halaman Aktivitas & Catatan Suara (Tab 2: Aktivitas).
 * Menghadirkan tampilan yang hangat, menenangkan, dan ramah untuk orang awam tanpa jargon teknis
 * (RMS/ZCR/Peak Sound & grafik gelombang disembunyikan di dalam menu opsional).
 */
@Composable
fun AnalyticsScreen(
    dashboardViewModel: DashboardViewModel,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val rmsHistory by dashboardViewModel.rmsHistory.collectAsState()
    val latestFeatures by dashboardViewModel.latestFeatures.collectAsState()
    val lastClassification by dashboardViewModel.lastClassification.collectAsState()
    val alertHistory by dashboardViewModel.alertHistory.collectAsState()

    var showTechnicalDetails by remember { mutableStateOf(false) }

    val isAnomaly = lastClassification?.hasAnomaly == true
    val currentRms = latestFeatures?.rms ?: 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Aktivitas & Situasi Rumah",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            // ── Indikator Sederhana Ketenangan Ruangan ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val statusEmoji = when {
                        isAnomaly -> "⚠️"
                        currentRms > 0.035f -> "😐"
                        else -> "😌"
                    }
                    val statusTitle = when {
                        isAnomaly -> "Perlu Perhatian Khusus"
                        currentRms > 0.035f -> "Ada Aktivitas Suara"
                        else -> "Rumahmu Tenang Saat Ini"
                    }
                    val statusColor = when {
                        isAnomaly -> AlertDanger
                        currentRms > 0.035f -> Color(0xFFE67E22)
                        else -> Primary
                    }
                    val statusMessage = when {
                        isAnomaly -> "Terdeteksi suara keras atau teriakan yang memerlukan pengecekan."
                        currentRms > 0.035f -> "Suara percakapan atau aktivitas normal terdeteksi di sekitar perangkat."
                        else -> "Situasi aman dan kondusif. Tidak ada suara ekstrem yang mengganggu."
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(38.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = statusEmoji, style = MaterialTheme.typography.headlineLarge)
                    }

                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    )

                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Proteksi Aktif 24 Jam",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Primary, CircleShape)
                            )
                            Text(
                                text = "Menjaga Ketenangan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // ── Catatan Kejadian (Histori) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Catatan Kejadian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (alertHistory.isEmpty()) {
                        Text(
                            text = "✅ Belum ada catatan kejadian. Situasi rumah tenang.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            alertHistory.take(5).forEach { alert ->
                                AlertHistoryItemRow(
                                    alert = alert,
                                    onReportSafe = { dashboardViewModel.reportFalseAlarm(alert) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Tombol Opsional Detail Teknis (Sembunyi by Default) ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = { showTechnicalDetails = !showTechnicalDetails }) {
                    Text(
                        text = if (showTechnicalDetails) "Sembunyikan Detail Akustik ▲" else "Lihat Detail Akustik (Opsional) ▼",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                }

                AnimatedVisibility(
                    visible = showTechnicalDetails,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Grafik Fluktuasi Suara
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Grafik Fluktuasi Suara (60 Detik)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                AudioChart(
                                    dataPoints = rmsHistory,
                                    isAnomalyDetected = isAnomaly,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                        }

                        // Data Fitur Teknis (RMS/ZCR/Peak)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Angka Pembacaan Sensor Audio",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatBox(
                                        title = "Kekerasan",
                                        subtitle = "RMS",
                                        value = String.format("%.3f", latestFeatures?.rms ?: 0f),
                                        color = Primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatBox(
                                        title = "Frekuensi",
                                        subtitle = "ZCR",
                                        value = String.format("%.3f", latestFeatures?.zcr ?: 0f),
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatBox(
                                        title = "Lonjakan",
                                        subtitle = "Peak",
                                        value = String.format("%.3f", latestFeatures?.peakAmplitude ?: 0f),
                                        color = AlertWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatBox(
    title: String,
    subtitle: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )
    }
}

@Composable
fun AlertHistoryItemRow(
    alert: com.example.suararumah.viewmodel.AlertRecord,
    onReportSafe: () -> Unit
) {
    val statusColor = when (alert.status) {
        "sent" -> AlertDanger
        "cancelled" -> Primary
        "false_alarm" -> AlertWarning
        else -> TextSecondary
    }

    val statusText = when (alert.status) {
        "sent" -> "TERKIRIM"
        "cancelled" -> "DIBATALKAN"
        "false_alarm" -> "AMAN (BUKAN BAHAYA)"
        else -> alert.status.uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = alert.label.uppercase(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(alert.timestamp)),
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )

        if (alert.status == "sent") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReportSafe,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertWarning)
            ) {
                Text("Tandai Bukan Bahaya (Aman)", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
