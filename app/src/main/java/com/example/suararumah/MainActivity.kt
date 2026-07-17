package com.example.suararumah

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.fil`led.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.suararumah.data.local.UserPreferences
import com.example.suararumah.ui.screen.AnalyticsScreen
import com.example.suararumah.ui.screen.LoginScreen
import com.example.suararumah.ui.screen.MonitoringScreen
import com.example.suararumah.ui.screen.ProfileScreen
import com.example.suararumah.ui.screen.SetupContactScreen
import com.example.suararumah.ui.theme.Primary
import com.example.suararumah.ui.theme.SuaraRumahTheme
import com.example.suararumah.ui.theme.Surface
import com.example.suararumah.util.VolumeButtonInterceptor
import com.example.suararumah.viewmodel.ContactViewModel
import com.example.suararumah.viewmodel.DashboardViewModel

/**
 * 4 Screen Utama aplikasi:
 * - Monitoring: Tab 1 (Beranda dengan Tombol Bulat Besar)
 * - Analytics: Tab 2 (Grafik & Histori Alert)
 * - Profile: Tab 3 (Login KTP & Info Sesi)
 * - SetupContact: Layar Atur Kontak Darurat
 */
enum class Screen {
    Monitoring,
    Analytics,
    Profile,
    SetupContact
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val userPreferences = UserPreferences(this)

        setContent {
            SuaraRumahTheme {
                val contactViewModel: ContactViewModel = viewModel()
                val dashboardViewModel: DashboardViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.Monitoring) }

                val isLoggedIn by userPreferences.isLoggedInFlow.collectAsState()
                val hasSeenOnboarding by userPreferences.hasSeenOnboardingFlow.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginSuccess = { name, nik ->
                            userPreferences.login(name = name, nik = nik)
                            currentScreen = Screen.Monitoring
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (currentScreen != Screen.SetupContact) {
                                NavigationBar(
                                    containerColor = Surface,
                                    contentColor = Primary,
                                    tonalElevation = 4.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Monitoring,
                                        onClick = { currentScreen = Screen.Monitoring },
                                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                        label = { Text("Memantau", style = MaterialTheme.typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Primary,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = Color(0xFF64748B),
                                            unselectedTextColor = Color(0xFF64748B)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Analytics,
                                        onClick = { currentScreen = Screen.Analytics },
                                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                        label = { Text("Analisis", style = MaterialTheme.typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Primary,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = Color(0xFF64748B),
                                            unselectedTextColor = Color(0xFF64748B)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Profile,
                                        onClick = { currentScreen = Screen.Profile },
                                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        label = { Text("Profil", style = MaterialTheme.typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Primary,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = Color(0xFF64748B),
                                            unselectedTextColor = Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        when (currentScreen) {
                            Screen.Monitoring -> MonitoringScreen(
                                dashboardViewModel = dashboardViewModel,
                                contactViewModel = contactViewModel,
                                onNavigateToContacts = { currentScreen = Screen.SetupContact },
                                hasSeenOnboarding = hasSeenOnboarding,
                                onDismissOnboarding = { userPreferences.markOnboardingSeen() },
                                innerPadding = innerPadding
                            )
                            Screen.Analytics -> AnalyticsScreen(
                                dashboardViewModel = dashboardViewModel,
                                innerPadding = innerPadding
                            )
                            Screen.Profile -> ProfileScreen(
                                userPreferences = userPreferences,
                                dashboardViewModel = dashboardViewModel,
                                onNavigateToContacts = { currentScreen = Screen.SetupContact },
                                innerPadding = innerPadding
                            )
                            Screen.SetupContact -> SetupContactScreen(
                                viewModel = contactViewModel,
                                onBack = { currentScreen = Screen.Monitoring }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Intercept tombol volume fisik.
     * Hanya aktif selama grace period berjalan — di luar itu,
     * tombol volume berfungsi normal.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (VolumeButtonInterceptor.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}