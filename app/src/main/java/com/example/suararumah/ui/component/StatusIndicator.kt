package com.example.suararumah.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.TextSecondary

/**
 * Indikator status monitoring di bagian atas layar utama.
 *
 * Desain (dari GuideStyle 3.1):
 * - Dot 8dp + label teks
 * - Hijau (#2A9D8F) + "Memantau aktif" saat service berjalan & backend reachable
 * - Abu-abu (#8D99AE) + "Tidak memantau" saat service mati
 * - Tanpa animasi berkedip (sesuai prinsip "tanpa animasi lebay")
 */
@Composable
fun StatusIndicator(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isMonitoring) Primary else TextSecondary
    val labelText = if (isMonitoring) "Memantau aktif" else "Tidak memantau"
    val labelColor = if (isMonitoring) Primary else TextSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(dotColor.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dot indicator — 8dp, tanpa animasi
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        // Label text
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
    }
}
