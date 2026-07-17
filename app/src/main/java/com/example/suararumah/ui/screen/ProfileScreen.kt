package com.example.suararumah.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.suararumah.data.local.UserPreferences
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.AlertWarning
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary
import com.example.suararumah.viewmodel.DashboardViewModel

/**
 * Halaman Profil & Pengaturan (Tab 3: Profil).
 * Fokus pada informasi akun pribadi pengguna dan tombol-tombol pengaturan penting,
 * tanpa menampilkan informasi teknis/perangkat yang membingungkan.
 */
@Composable
fun ProfileScreen(
    userPreferences: UserPreferences,
    dashboardViewModel: DashboardViewModel,
    onNavigateToContacts: () -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val userInfo by userPreferences.userInfoFlow.collectAsState()
    var showDemoPanel by remember { mutableStateOf(false) }
    var demoTapCount by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Profil & Pengaturan",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        // ── Kartu Identitas Pengguna (Tanpa Tampilan NIK demi Privasi & Keamanan) ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = userInfo?.name ?: "Warga Sangatta Utara",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Akun Lokal Terverifikasi",
                                style = MaterialTheme.typography.bodySmall.copy(color = Primary, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Terverifikasi",
                        tint = Primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status Proteksi:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text("🛡️ Aktif Menjaga Rumah", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Primary))
                }
            }
        }

        // ── Menu Pengaturan & Fitur Utama ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Orang yang Kupercaya (Kontak Darurat)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToContacts() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Orang yang Kupercaya", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Daftar penerima pesan pertolongan otomatis", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Notifikasi & Getaran
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Notifikasi & Getaran", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Aktif otomatis saat terdeteksi suara ekstrem", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        }
                    }
                    Text("Aktif", style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontWeight = FontWeight.Bold))
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Privasi & Keamanan Lokal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Privasi & Keamanan Lokal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Suaramu tidak pernah direkam atau disimpan", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        }
                    }
                    Text("Terjamin", style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontWeight = FontWeight.Bold))
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Versi Aplikasi (Tekan 5x untuk membuka panel demo juri)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            demoTapCount++
                            if (demoTapCount >= 5) {
                                showDemoPanel = !showDemoPanel
                                demoTapCount = 0
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Versi Aplikasi Suara Rumah v2.0", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    }
                    if (demoTapCount in 1..4) {
                        Text("${5 - demoTapCount}x lagi untuk demo", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    }
                }
            }
        }

        // ── Tombol Logout (Keluar & Hapus Data Lokal) ──
        OutlinedButton(
            onClick = { userPreferences.logout() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertDanger),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertDanger)
        ) {
            Text("Keluar Akun (Hapus Data Lokal)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Panel Simulasi Audio (Tersembunyi, hanya aktif jika tap versi 5 kali untuk Juri) ──
        AnimatedVisibility(
            visible = showDemoPanel,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ Panel Simulasi (Khusus Presentasi/Juri)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Primary)
                            )
                        }
                        TextButton(onClick = { showDemoPanel = false }) {
                            Text("Tutup", style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simulasikan pemicu suara untuk pengujian tanpa bersuara keras:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { dashboardViewModel.simulateNormalAudio() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Suara Normal", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { dashboardViewModel.simulateScreamAudio() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertWarning),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simulasi Teriakan", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { dashboardViewModel.simulateCrashAudio() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertDanger),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simulasi Bantingan", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { dashboardViewModel.simulateEscalatingAnomaly() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertDanger),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simulasi SOS Darurat", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
