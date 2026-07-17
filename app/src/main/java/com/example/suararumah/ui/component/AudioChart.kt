package com.example.suararumah.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary

/**
 * Grafik audio real-time berbasis Canvas (native Compose, tanpa library charting).
 *
 * Dari GuideStyle 3.5:
 * - Line chart minimalis
 * - Warna primary (#2A9D8F) saat normal
 * - Warna alert (#E76F51) saat anomali
 * - Card putih, corner radius 16dp, elevation 2dp
 *
 * Menampilkan ~60 data points terakhir (scrolling window).
 */
@Composable
fun AudioChart(
    dataPoints: List<Float>,
    isAnomalyDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isAnomalyDetected) AlertDanger else Primary
    val gridColor = TextSecondary.copy(alpha = 0.15f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Fluktuasi Suara",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (dataPoints.isEmpty()) "Menunggu data..."
                else "RMS · ${dataPoints.size} sampel terakhir",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas untuk grafik
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 8f

                // Grid horizontal (3 garis)
                for (i in 1..3) {
                    val y = height * i / 4
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                if (dataPoints.size < 2) return@Canvas

                // Normalisasi data: ambil max untuk skala
                val maxVal = dataPoints.max().coerceAtLeast(0.01f)
                val effectiveHeight = height - padding * 2

                // Bangun path
                val path = Path()
                val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    // Invert Y (canvas origin di kiri-atas)
                    val normalizedValue = (value / maxVal).coerceIn(0f, 1f)
                    val y = padding + effectiveHeight * (1f - normalizedValue)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Gambar line
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Dot pada titik terakhir
                if (dataPoints.isNotEmpty()) {
                    val lastX = (dataPoints.size - 1) * stepX
                    val lastNorm = (dataPoints.last() / maxVal).coerceIn(0f, 1f)
                    val lastY = padding + effectiveHeight * (1f - lastNorm)

                    drawCircle(
                        color = lineColor,
                        radius = 5f,
                        center = Offset(lastX, lastY)
                    )
                    // Outer glow
                    drawCircle(
                        color = lineColor.copy(alpha = 0.3f),
                        radius = 10f,
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}
