package com.example.suararumah.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * Halaman Analisis & Data Suara (Tab 2: Data & Histori).
 * Menampilkan statistik pembacaan terakhir, grafik audio real-time, dan histori alert.
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

            // ── Kartu Status Ketenangan Ruangan (Sederhana & Empatik untuk Orang Awam) ──
            var showTechnicalDetails by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            val isAnomaly = lastClassification?.hasAnomaly == true
            val isModerateActivity = !isAnomaly && (latestFeatures?.rms ?: 0f) > 0.04f

            val statusEmoji = when {
                isAnomaly -> "⚠️"
                isModerateActivity -> "😐"
                else -> "😌"
            }
            val statusTitle = when {
                isAnomaly -> "Perlu Perhatian Khusus"
                isModerateActivity -> "Ada Aktivitas Suara"
                else -> "Rumahmu Tenang Saat Ini"
            }
            val statusDesc = when {
                isAnomaly -> "Terdeteksi suara keras atau teriakan yang memerlukan pengecekan."
                isModerateActivity -> "Suara percakapan atau aktivitas normal terdeteksi di sekitar perangkat."
                else -> "Situasi aman dan kondusif. Tidak ada suara ekstrem yang mengganggu."
            }
            val statusColor = when {
                isAnomaly -> AlertDanger
                isModerateActivity -> AlertWarning
                else -> Primary
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = statusEmoji, style = MaterialTheme.typography.headlineLarge)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = statusColor)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Proteksi Aktif 24 Jam", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Text(
                            text = "🟢 Menjaga Ketenangan",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Primary)
                        )
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

            // ── Tombol Opsi Detail Teknis (Tersembunyi secara default untuk orang awam) ──
            TextButton(
                onClick = { showTechnicalDetails = !showTechnicalDetails },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (showTechnicalDetails) "Tutup Detail Akustik ▲" else "Lihat Detail Akustik (Opsional) ▼",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }

            if (showTechnicalDetails) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Grafik Gelombang & Angka Sensor",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AudioChart(
                            dataPoints = rmsHistory,
                            isAnomalyDetected = lastClassification?.hasAnomaly == true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("RMS: ${String.format("%.3f", latestFeatures?.rms ?: 0f)}", style = MaterialTheme.typography.labelSmall)
                            Text("ZCR: ${String.format("%.3f", latestFeatures?.zcr ?: 0f)}", style = MaterialTheme.typography.labelSmall)
                            Text("Peak: ${String.format("%.3f", latestFeatures?.peakAmplitude ?: 0f)}", style = MaterialTheme.typography.labelSmall)
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
        "false_alarm" -> "ALARM PALSU"
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
                Text("Tandai Alarm Palsu", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
