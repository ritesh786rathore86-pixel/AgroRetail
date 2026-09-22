package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.model.AdminUser
import com.example.data.model.UserRole
import com.example.data.repository.AgroRepository
import com.example.data.repository.AuthManager
import com.example.ui.admin.AdminMainScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.retailer.RetailerMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AgroNotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var repository: AgroRepository
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AgroRepository(applicationContext)
        authManager = AuthManager(applicationContext)
        AgroNotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* Handled */ }

                    LaunchedEffect(Unit) {
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by authManager.currentUser.collectAsState()
                    val currentRetailer by authManager.currentRetailer.collectAsState()
                    val retailers by repository.retailers.collectAsState()

                    // Ensure retailer object is restored if session was preserved
                    LaunchedEffect(currentUser, retailers) {
                        authManager.restoreRetailerObject(retailers)
                    }

                    // Also sync currentRetailer if retailer was updated in repository
                    LaunchedEffect(retailers) {
                        val current = currentRetailer
                        if (current != null) {
                            val refreshed = retailers.find { it.id == current.id }
                            if (refreshed != null && refreshed != current) {
                                authManager.updateCurrentRetailer(refreshed)
                            }
                        }
                    }

                    val user = currentUser
                    val retailer = currentRetailer

                    when {
                        user == null -> {
                            LoginScreen(
                                authManager = authManager,
                                repository = repository,
                                onLoginSuccess = { /* State updates trigger navigation automatically */ }
                            )
                        }
                        user.role == UserRole.ADMIN -> {
                            AdminMainScreen(
                                admin = AdminUser(
                                    id = user.id,
                                    email = user.email.ifBlank { "admin@agroretail.com" },
                                    name = user.name.ifBlank { "Distributor Admin" }
                                ),
                                repository = repository,
                                onLogout = { authManager.logout() }
                            )
                        }
                        user.role == UserRole.RETAILER -> {
                            if (retailer != null) {
                                RetailerMainScreen(
                                    retailer = retailer,
                                    repository = repository,
                                    onLogout = { authManager.logout() }
                                )
                            } else {
                                // In case retailer is still loading from state
                                LoginScreen(
                                    authManager = authManager,
                                    repository = repository,
                                    onLoginSuccess = { }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
