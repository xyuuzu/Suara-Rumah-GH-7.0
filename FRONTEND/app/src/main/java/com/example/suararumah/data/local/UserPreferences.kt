package com.example.suararumah.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Penyimpanan lokal untuk sesi pengguna (Login KTP formalitas & kredensial backend).
 * Menyimpan status masuk secara presisten di SharedPreferences dan terhapus saat logout.
 */
class UserPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "suara_rumah_user_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_KTP_NAME = "ktp_name"
        private const val KEY_KTP_NIK = "ktp_nik"
        private const val KEY_PHONE = "phone"
        private const val KEY_PASSWORD = "password"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

        // StateFlow global untuk reaktif diperbarui di UI
        private val _isLoggedInFlow = MutableStateFlow(false)
        val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

        private val _userInfoFlow = MutableStateFlow<UserInfo?>(null)
        val userInfoFlow: StateFlow<UserInfo?> = _userInfoFlow.asStateFlow()

        private val _hasSeenOnboardingFlow = MutableStateFlow(true)
        val hasSeenOnboardingFlow: StateFlow<Boolean> = _hasSeenOnboardingFlow.asStateFlow()
    }

    data class UserInfo(
        val nik: String,
        val phone: String,
        val name: String,
        val deviceId: String,
        val apiKey: String
    )

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isLoggedInFlow: StateFlow<Boolean>
        get() = UserPreferences.isLoggedInFlow

    val userInfoFlow: StateFlow<UserInfo?>
        get() = UserPreferences.userInfoFlow

    val hasSeenOnboardingFlow: StateFlow<Boolean>
        get() = UserPreferences.hasSeenOnboardingFlow

    init {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        _isLoggedInFlow.value = loggedIn
        _hasSeenOnboardingFlow.value = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
        if (loggedIn) {
            _userInfoFlow.value = UserInfo(
                nik = prefs.getString(KEY_KTP_NIK, "6408123456780001") ?: "6408123456780001",
                phone = prefs.getString(KEY_PHONE, "081234567890") ?: "081234567890",
                name = prefs.getString(KEY_KTP_NAME, "Warga Terverifikasi") ?: "Warga Terverifikasi",
                deviceId = prefs.getString(KEY_DEVICE_ID, "device-001") ?: "device-001",
                apiKey = prefs.getString(KEY_API_KEY, "suara-rumah-dummy-key") ?: "suara-rumah-dummy-key"
            )
        }
    }

    /**
     * Masuk dengan NIK, Nomor Handphone, dan Password.
     */
    fun login(nik: String, phone: String, password: String = "", name: String = "Warga Terverifikasi", deviceId: String = "device-001", apiKey: String = "suara-rumah-key-001") {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_HAS_SEEN_ONBOARDING, false)
            putString(KEY_KTP_NIK, nik)
            putString(KEY_PHONE, phone)
            putString(KEY_PASSWORD, password)
            putString(KEY_KTP_NAME, name)
            putString(KEY_DEVICE_ID, deviceId)
            putString(KEY_API_KEY, apiKey)
            apply()
        }
        _isLoggedInFlow.value = true
        _hasSeenOnboardingFlow.value = false
        _userInfoFlow.value = UserInfo(nik, phone, name, deviceId, apiKey)
    }

    fun markOnboardingSeen() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
        _hasSeenOnboardingFlow.value = true
    }

    /**
     * Update info device yang didapat dari endpoint /devices/register backend.
     */
    fun updateDeviceCredentials(deviceId: String, apiKey: String) {
        prefs.edit().apply {
            putString(KEY_DEVICE_ID, deviceId)
            putString(KEY_API_KEY, apiKey)
            apply()
        }
        val currentInfo = _userInfoFlow.value
        if (currentInfo != null) {
            _userInfoFlow.value = currentInfo.copy(deviceId = deviceId, apiKey = apiKey)
        }
    }

    /**
     * Keluar dari akun & hapus seluruh data dari local storage.
     */
    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_HAS_SEEN_ONBOARDING)
            remove(KEY_KTP_NAME)
            remove(KEY_KTP_NIK)
            remove(KEY_PHONE)
            remove(KEY_PASSWORD)
            apply()
        }
        _isLoggedInFlow.value = false
        _hasSeenOnboardingFlow.value = false
        _userInfoFlow.value = null
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getDeviceId(): String = prefs.getString(KEY_DEVICE_ID, "device-001") ?: "device-001"
    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
}
