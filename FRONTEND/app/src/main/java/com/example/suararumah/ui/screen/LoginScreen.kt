package com.example.suararumah.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suararumah.R
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary

/**
 * Halaman Login / Masuk Akun yang didesain elegan dan premium namun tetap konsisten,
 * ramah pengguna (psikologis menenangkan), dan tanpa bahasa terlalu teknis.
 * Menampilkan logo aplikasi dari mipmap serta input NIK, Nomor Handphone, dan Kata Sandi.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (nik: String, phone: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nikInput by remember { mutableStateOf("6408123456780001") }
    var phoneInput by remember { mutableStateOf("081234567890") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header Logo & Judul Aplikasi ──
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Logo dengan Ring Halus Elegan
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(8.dp, CircleShape, spotColor = Primary.copy(alpha = 0.3f))
                            .background(Primary.copy(alpha = 0.12f), CircleShape)
                            .border(2.dp, Primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.logo),
                            contentDescription = "Logo Suara Rumah",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                        )
                    }

                    Text(
                        text = "Suara Rumah",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                    )

                    Surface(
                        color = Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "🛡️ Perlindungan Ketenangan Rumah 24/7",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Kartu Formulir Masuk Akun ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x0F000000)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Selamat Datang",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Masuk dengan data identitasmu untuk mengaktifkan penjagaan dan ketenangan rumah.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Input NIK
                        OutlinedTextField(
                            value = nikInput,
                            onValueChange = { nikInput = it },
                            label = { Text("NIK (Nomor Induk Kependudukan)") },
                            placeholder = { Text("Contoh: 6408123456780001") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary,
                                focusedLabelColor = Primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Input Nomor Handphone
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Nomor Handphone") },
                            placeholder = { Text("Contoh: 081234567890") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary,
                                focusedLabelColor = Primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )

                        // Input Kata Sandi
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Kata Sandi") },
                            placeholder = { Text("Masukkan kata sandi Anda") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            trailingIcon = {
                                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "Sembunyikan" else "Lihat",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary,
                                focusedLabelColor = Primary
                            ),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(color = AlertDanger, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Tombol Masuk Utama
                        Button(
                            onClick = {
                                if (nikInput.trim().length < 10 || phoneInput.trim().isBlank() || passwordInput.isBlank()) {
                                    errorMessage = "Mohon masukkan NIK, Nomor Handphone, dan Kata Sandi dengan benar."
                                } else {
                                    errorMessage = ""
                                    onLoginSuccess(nikInput.trim(), phoneInput.trim(), passwordInput)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Masuk & Aktifkan Perlindungan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // ── Jaminan Privasi & Keamanan Lokal ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.07f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Privasi & Keamanan 100% Terjamin",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Primary)
                            )
                            Text(
                                text = "Suaramu maupun data identitasmu tidak pernah dikirim ke luar perangkat. Seluruh pemantauan diproses secara lokal di HP kamu.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
