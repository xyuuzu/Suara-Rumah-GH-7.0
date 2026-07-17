package com.example.suararumah.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suararumah.service.GracePeriodState
import com.example.suararumah.ui.component.PermissionConsentDialog
import com.example.suararumah.ui.component.SafeButton
import com.example.suararumah.ui.component.StatusIndicator
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.AlertWarning
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary
import com.example.suararumah.util.hasMicrophonePermission
import com.example.suararumah.viewmodel.ContactViewModel
import com.example.suararumah.viewmodel.DashboardViewModel

/**
 * Halaman Utama (Tab 1: Beranda / Monitoring).
 * Desain Modern Simpel Tanpa Gradasi dengan Tombol Bulat Besar di Tengah.
 */
@Composable
fun MonitoringScreen(
    dashboardViewModel: DashboardViewModel,
    contactViewModel: ContactViewModel,
    onNavigateToContacts: () -> Unit,
    hasSeenOnboarding: Boolean = true,
    onDismissOnboarding: () -> Unit = {},
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val isMonitoring by dashboardViewModel.isMonitoring.collectAsState()
    val isAnomalyDetected by dashboardViewModel.isAnomalyDetected.collectAsState()
    val gracePeriodState by dashboardViewModel.gracePeriodState.collectAsState()
    val remainingSeconds by dashboardViewModel.gracePeriodRemainingSeconds.collectAsState()
    val statusMessage by dashboardViewModel.statusMessage.collectAsState()
    val contacts by contactViewModel.contacts.collectAsState()
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
    var showDemoPanel by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            dashboardViewModel.clearStatusMessage()
        }
    }

    fun toggleMonitoringSafely() {
        if (isMonitoring) {
            dashboardViewModel.toggleMonitoring(false)
        } else if (hasMicrophonePermission(context)) {
            dashboardViewModel.toggleMonitoring(true)
        } else {
            showMicPermissionDialog = true
        }
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Top Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Suara Rumah",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Sistem Proteksi Akustik Lokal",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                StatusIndicator(isMonitoring = isMonitoring)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Tombol Bulat Besar di Tengah ──
            BigCenterMonitoringButton(
                isMonitoring = isMonitoring,
                isAnomalyDetected = isAnomalyDetected || gracePeriodState == GracePeriodState.COUNTING_DOWN,
                onToggle = { toggleMonitoringSafely() }
            )

            // ── Tombol Aman (SafeButton) & Grace Period Countdown (Muncul Saat Anomali) ──
            AnimatedVisibility(
                visible = isAnomalyDetected || gracePeriodState == GracePeriodState.COUNTING_DOWN,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AlertDanger.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertDanger)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertDanger,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ANOMALI ATAU SUARA DARURAT TERDETEKSI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AlertDanger
                                ),
                                textAlign = TextAlign.Center
                            )
                            if (gracePeriodState == GracePeriodState.COUNTING_DOWN) {
                                Text(
                                    text = "Mengirim pesan SOS & lokasi dalam $remainingSeconds detik",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    SafeButton(
                        isGracePeriodActive = isAnomalyDetected || gracePeriodState == GracePeriodState.COUNTING_DOWN,
                        onClick = { dashboardViewModel.cancelAlert() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Kontak Darurat Card ──
            EmergencyContactSummaryCard(
                contactsCount = contacts.size,
                firstContactName = contacts.firstOrNull()?.name,
                firstContactPhone = contacts.firstOrNull()?.phoneNumber,
                onClick = onNavigateToContacts
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
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

        // ── Popup Dialog Panduan Awal (Onboarding) ──
        if (!hasSeenOnboarding) {
            WelcomeOnboardingDialog(
                onNavigateToContacts = {
                    onDismissOnboarding()
                    onNavigateToContacts()
                },
                onDismiss = onDismissOnboarding
            )
        }
    }
}

/**
 * Tombol Bulat Besar Di Tengah — desain lebih besar dengan halo glassmorphism di belakangnya
 * dan diposisikan agak ke bawah tengah.
 */
@Composable
fun BigCenterMonitoringButton(
    isMonitoring: Boolean,
    isAnomalyDetected: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnomalyDetected) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isAnomalyDetected -> AlertDanger
            isMonitoring -> Primary
            else -> Color(0xFF64748B)
        },
        animationSpec = tween(300),
        label = "color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // ── Outer Glass Halo Ring (Lebih Besar & Elegan) ──
        Box(
            modifier = Modifier.size(310.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = 0.08f))
                    .border(1.5.dp, buttonColor.copy(alpha = 0.2f), CircleShape)
            )

            // Inner Glass Circle
            Box(
                modifier = Modifier
                    .size(276.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = 0.15f))
            )

            // ── Main Tactile Circular Button ──
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable { onToggle() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isMonitoring) Icons.Default.Notifications else Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when {
                                isAnomalyDetected -> "DARURAT"
                                isMonitoring -> "MEMANTAU"
                                else -> "NON-AKTIF"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.3.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMonitoring) "Ketuk untuk nonaktifkan" else "Ketuk untuk mengaktifkan",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu ringkas Kontak Darurat di beranda.
 */
@Composable
fun EmergencyContactSummaryCard(
    contactsCount: Int,
    firstContactName: String?,
    firstContactPhone: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (contactsCount > 0) Primary.copy(alpha = 0.15f) else AlertWarning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (contactsCount > 0) Primary else AlertWarning
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Kontak Darurat",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (contactsCount > 0 && firstContactName != null) {
                        Text(
                            text = "$firstContactName ($firstContactPhone)" + if (contactsCount > 1) " +${contactsCount - 1} lainnya" else "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Primary, fontWeight = FontWeight.SemiBold)
                        )
                    } else {
                        Text(
                            text = "⚠️ Belum diatur • Ketuk untuk mengatur",
                            style = MaterialTheme.typography.bodySmall.copy(color = AlertWarning, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Lihat Kontak",
                tint = TextSecondary
            )
        }
}
}

/**
 * Dialog Panduan Onboarding Ramah Pengguna — Muncul pertama kali setelah login
 * untuk mengarahkan pengguna mengatur kontak darurat & mengaktifkan proteksi.
 */
@Composable
fun WelcomeOnboardingDialog(
    onNavigateToContacts: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💡 Selamat Datang di Suara Rumah!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Primary)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Proteksi akustik pintar kini aktif di perangkat Anda. Untuk keamanan maksimal, ikuti 2 langkah cepat berikut:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1️⃣ Atur Kontak Darurat", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Primary))
                        Text("Daftarkan nomor keluarga/tetangga untuk menerima SMS & WA otomatis jika bahaya terdeteksi.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("2️⃣ Aktifkan Proteksi Suara", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Tekan tombol bulat besar di tengah beranda untuk mulai memantau suara ruangan 24/7.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNavigateToContacts,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Atur Kontak Sekarang", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mengerti, Nanti Saja", style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
