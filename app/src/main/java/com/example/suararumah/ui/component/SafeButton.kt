package com.example.suararumah.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.Primary

/**
 * Tombol "Aman" — satu komponen persisten di layar utama dengan dua tampilan state.
 *
 * Desain (dari GuideStyle 3.2):
 * - TIDAK berpindah posisi, TIDAK muncul sebagai overlay/dialog baru
 * - Hanya warna dan label yang berubah di tempat yang sama
 * - Touch target minimal 48x48dp
 * - Perubahan state instan (tanpa animasi fade/slide)
 *
 * | State               | Warna Latar      | Label                         |
 * |---------------------|------------------|-------------------------------|
 * | Normal              | #2A9D8F (hijau)  | "Aman"                        |
 * | Grace Period Aktif  | #E76F51 (coral)  | "Aman — Batalkan Peringatan"  |
 */
@Composable
fun SafeButton(
    isGracePeriodActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isGracePeriodActive) AlertDanger else Primary
    val labelText = if (isGracePeriodActive) "Aman — Batalkan Peringatan" else "Aman"

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp), // Lebih dari 48dp minimum touch target
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}
