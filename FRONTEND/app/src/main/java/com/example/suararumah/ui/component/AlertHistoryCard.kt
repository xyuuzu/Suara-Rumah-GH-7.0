package com.example.suararumah.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary
import com.example.suararumah.viewmodel.AlertRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card untuk satu item histori alert.
 * Menampilkan: ikon status, timestamp, label klasifikasi, status.
 */
@Composable
fun AlertHistoryCard(
    record: AlertRecord,
    modifier: Modifier = Modifier,
    onReportFalseAlarm: (String) -> Unit = {}
) {
    val (emoji, statusLabel, statusColor) = when (record.status) {
        "sent" -> Triple("🚨", "Terkirim", AlertDanger)
        "cancelled" -> Triple("✅", "Dibatalkan", Primary)
        "false_alarm" -> Triple("ℹ️", "Alarm Palsu", TextSecondary)
        else -> Triple("❓", "Unknown", TextSecondary)
    }

    val classLabel = when (record.label) {
        "scream" -> "Teriakan"
        "crash" -> "Benda Pecah"
        "loud_noise" -> "Suara Keras"
        else -> record.label.replaceFirstChar { it.uppercase() }
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
    val timeStr = dateFormat.format(Date(record.timestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status emoji
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(6.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = classLabel,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (record.status == "sent") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onReportFalseAlarm(record.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Primary
                        )
                    ) {
                        Text(
                            text = "📢 Tandai Alarm Palsu (Kondisi Aman)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
