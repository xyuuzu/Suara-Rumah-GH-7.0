package com.example.suararumah.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.suararumah.data.local.EmergencyContactEntity
import com.example.suararumah.data.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel untuk mengelola kontak darurat.
 * Digunakan oleh SetupContactScreen dan bagian kontak di DashboardScreen.
 */
class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)

    /** Daftar kontak darurat (reactive) */
    val contacts: StateFlow<List<EmergencyContactEntity>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Form State ──
    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _phoneInput = MutableStateFlow("")
    val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

    private val _relationshipInput = MutableStateFlow("")
    val relationshipInput: StateFlow<String> = _relationshipInput.asStateFlow()

    private val _showError = MutableStateFlow<String?>(null)
    val showError: StateFlow<String?> = _showError.asStateFlow()

    private val _showSuccess = MutableStateFlow(false)
    val showSuccess: StateFlow<Boolean> = _showSuccess.asStateFlow()

    fun updateName(value: String) { _nameInput.value = value }
    fun updatePhone(value: String) { _phoneInput.value = value }
    fun updateRelationship(value: String) { _relationshipInput.value = value }

    /**
     * Tambah kontak darurat baru setelah validasi.
     */
    fun addContact() {
        val name = _nameInput.value.trim()
        val phone = _phoneInput.value.trim()
        val relationship = _relationshipInput.value.trim()

        // Validasi
        if (name.isBlank()) {
            _showError.value = "Nama tidak boleh kosong"
            return
        }
        if (phone.isBlank()) {
            _showError.value = "Nomor telepon tidak boleh kosong"
            return
        }
        if (!phone.startsWith("+") && !phone.startsWith("0")) {
            _showError.value = "Format nomor: +62xxx atau 08xxx"
            return
        }

        viewModelScope.launch {
            repository.addContact(name, phone, relationship)
            // Reset form
            _nameInput.value = ""
            _phoneInput.value = ""
            _relationshipInput.value = ""
            _showError.value = null
            _showSuccess.value = true
        }
    }

    /**
     * Hapus kontak berdasarkan ID.
     */
    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
        }
    }

    fun dismissSuccess() {
        _showSuccess.value = false
    }

    fun dismissError() {
        _showError.value = null
    }
}
