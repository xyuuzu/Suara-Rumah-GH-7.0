package com.example.suararumah.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionConsentDialog(
    permissions: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Persetujuan Izin") },
        text = {
            Text(
                text = buildString {
                    append("Aplikasi membutuhkan izin berikut agar fitur pemantauan berjalan:\n\n")
                    permissions.forEach { permission ->
                        append("• ")
                        append(permission)
                        append('\n')
                    }
                    append("\nSetuju untuk melanjutkan?")
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Setuju")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal")
            }
        }
    )
}
