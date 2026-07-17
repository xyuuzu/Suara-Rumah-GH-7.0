package com.example.suararumah.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.suararumah.data.local.EmergencyContactEntity
import com.example.suararumah.ui.theme.AlertDanger
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.ui.theme.TextSecondary
import com.example.suararumah.viewmodel.ContactViewModel

/**
 * Screen untuk mendaftarkan dan mengelola kontak darurat.
 *
 * Layout:
 * 1. Form input (nama, nomor telepon, relasi)
 * 2. Tombol "Tambah Kontak"
 * 3. Daftar kontak yang sudah terdaftar (swipe/tap to delete)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupContactScreen(
    viewModel: ContactViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val nameInput by viewModel.nameInput.collectAsState()
    val phoneInput by viewModel.phoneInput.collectAsState()
    val relationshipInput by viewModel.relationshipInput.collectAsState()
    val showError by viewModel.showError.collectAsState()
    val showSuccess by viewModel.showSuccess.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Tampilkan snackbar saat berhasil/error
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            snackbarHostState.showSnackbar("Kontak berhasil ditambahkan")
            viewModel.dismissSuccess()
        }
    }
    LaunchedEffect(showError) {
        showError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kontak Darurat",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Form Input ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tambah Kontak Darurat",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Kontak ini akan menerima pesan darurat saat anomali terdeteksi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Nama
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { viewModel.updateName(it) },
                            label = { Text("Nama") },
                            placeholder = { Text("Contoh: Ibu Ani") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary
                            ),
                            singleLine = true
                        )

                        // Nomor Telepon
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { viewModel.updatePhone(it) },
                            label = { Text("Nomor Telepon") },
                            placeholder = { Text("+628123456789") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )

                        // Relasi (opsional)
                        OutlinedTextField(
                            value = relationshipInput,
                            onValueChange = { viewModel.updateRelationship(it) },
                            label = { Text("Hubungan (opsional)") },
                            placeholder = { Text("Contoh: Keluarga, Teman, Tetangga") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Tombol Tambah
                        Button(
                            onClick = { viewModel.addContact() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                "Tambah Kontak",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // ── Header daftar kontak ──
            item {
                if (contacts.isNotEmpty()) {
                    Text(
                        text = "Kontak Terdaftar (${contacts.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ── Daftar kontak ──
            items(contacts, key = { it.id }) { contact ->
                ContactCard(
                    contact = contact,
                    onDelete = { viewModel.deleteContact(contact.id) }
                )
            }

            // ── Empty state ──
            if (contacts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📋",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada kontak darurat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "Tambahkan minimal 1 kontak untuk menerima pesan darurat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * Card untuk menampilkan satu kontak darurat.
 */
@Composable
private fun ContactCard(
    contact: EmergencyContactEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Avatar
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                tint = Primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (contact.relationship.isNotBlank()) {
                    Text(
                        text = contact.relationship,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus kontak",
                    tint = AlertDanger.copy(alpha = 0.7f)
                )
            }
        }
    }
}
