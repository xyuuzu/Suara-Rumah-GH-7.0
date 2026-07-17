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
                text = "Analisis & Histori Suara",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            // ── Grafik Fluktuasi Suara (AudioChart) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Grafik Aktivitas Suara Ruangan (60 Detik)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Memantau kestabilan gelombang suara secara langsung di sekitar perangkat.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AudioChart(
                        dataPoints = rmsHistory,
                        isAnomalyDetected = lastClassification?.hasAnomaly == true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(if (lastClassification?.hasAnomaly == true) AlertDanger else Primary, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lastClassification?.hasAnomaly == true) "⚠️ Terdeteksi fluktuasi suara ekstrem (Anomali)" else "🟢 Aktivitas suara normal & aman",
                            style = MaterialTheme.typography.labelSmall.copy(color = if (lastClassification?.hasAnomaly == true) AlertDanger else Primary, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // ── Data Fitur Terakhir (Ramah Orang Umum) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ringkasan Pembacaan Suara",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Indikator akustik yang dipahami oleh sistem AI lokal:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(
                            title = "Kekerasan",
                            subtitle = "Loudness (RMS)",
                            value = String.format("%.3f", latestFeatures?.rms ?: 0f),
                            color = Primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatBox(
                            title = "Frekuensi",
                            subtitle = "Pitch (ZCR)",
                            value = String.format("%.3f", latestFeatures?.zcr ?: 0f),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatBox(
                            title = "Lonjakan Max",
                            subtitle = "Peak Sound",
                            value = String.format("%.3f", latestFeatures?.peakAmplitude ?: 0f),
                            color = AlertWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Label Klasifikasi Ramah Umum
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (lastClassification?.hasAnomaly == true) AlertDanger.copy(alpha = 0.12f) else Primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status Ruangan:",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = if (lastClassification?.hasAnomaly == true) "ANOMALI TERDETEKSI" else "AMAN DAN KONDUSIF",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (lastClassification?.hasAnomaly == true) AlertDanger else Primary
                                )
                            )
                        }
                        Badge(
                            containerColor = if (lastClassification?.hasAnomaly == true) AlertDanger else Primary,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = lastClassification?.label?.uppercase() ?: "NORMAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── Histori Alert Card & Laporan Alarm Palsu ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Histori Peringatan Darurat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (alertHistory.isEmpty()) {
                        Text(
                            text = "✅ Belum ada insiden atau suara darurat yang tercatat.",
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
