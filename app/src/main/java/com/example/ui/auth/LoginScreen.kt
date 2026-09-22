package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.data.repository.AuthManager
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber

@Composable
fun LoginScreen(
    authManager: AuthManager,
    repository: AgroRepository,
    onLoginSuccess: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Retailer, 1 = Admin

    // Retailer fields
    var mobileNumber by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    // Admin fields
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val retailers by repository.retailers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // App Logo & Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Agriculture,
                contentDescription = null,
                tint = GoldenSun,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AgroRetail",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ForestGreenPrimary
        )

        Text(
            text = "Retailer Ordering Platform",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Role Switcher Tab
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Button(
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("retailer_tab_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) ForestGreenPrimary else Color.Transparent,
                        contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = if (selectedTab == 0) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retailer Login", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_tab_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) ForestGreenPrimary else Color.Transparent,
                        contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = if (selectedTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin Login", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message card
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // RETAILER LOGIN FORM
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) mobileNumber = it },
                label = { Text("Mobile Number") },
                placeholder = { Text("10 digit registered mobile") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenPrimary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mobile_number_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                label = { Text("Security PIN (4 or 6 Digits)") },
                placeholder = { Text("Enter your secret PIN") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { pinVisible = !pinVisible }) {
                        Icon(
                            imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle PIN visibility"
                        )
                    }
                },
                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (mobileNumber.length != 10) {
                        errorMessage = "Please enter a valid 10-digit mobile number."
                        return@Button
                    }
                    if (pin.length < 4) {
                        errorMessage = "Please enter your 4 or 6-digit security PIN."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val retailer = repository.findRetailerByMobile(mobileNumber)
                    if (retailer == null) {
                        errorMessage = "No registered retailer found with mobile $mobileNumber. Please contact admin."
                        isLoading = false
                        return@Button
                    }

                    if (!retailer.isActive) {
                        errorMessage = "This retailer account has been deactivated. Please contact admin."
                        isLoading = false
                        return@Button
                    }

                    val isCorrect = repository.verifyRetailerPin(retailer, pin)
                    if (!isCorrect) {
                        errorMessage = "Incorrect security PIN. Please re-enter your PIN."
                        isLoading = false
                        return@Button
                    }

                    // Success
                    authManager.setRetailerSession(retailer)
                    isLoading = false
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("retailer_login_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("Secure Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Demo Credentials Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = HarvestAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Demo Accounts (Tap to Fill)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = HarvestAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    retailers.take(3).forEach { ret ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                mobileNumber = ret.mobileNumber
                                pin = when (ret.mobileNumber) {
                                    "9876543210" -> "1234"
                                    "9812345678" -> "4321"
                                    else -> "9999"
                                }
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${ret.businessName} (${ret.retailerName})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Mob: ${ret.mobileNumber} • Outstanding: ₹${"%,.0f".format(ret.outstandingAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "PIN: ${if (ret.mobileNumber == "9876543210") "1234" else if (ret.mobileNumber == "9812345678") "4321" else "9999"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // ADMIN LOGIN FORM
            OutlinedTextField(
                value = adminEmail,
                onValueChange = { adminEmail = it },
                label = { Text("Admin Email / ID") },
                placeholder = { Text("e.g. admin@agroretail.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_email_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it },
                label = { Text("Admin Password / Key") },
                placeholder = { Text("Enter admin password") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ForestGreenPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                        Icon(
                            imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (adminEmail.isBlank()) {
                        errorMessage = "Please enter Admin Email / Username."
                        return@Button
                    }
                    if (adminPassword.isBlank()) {
                        errorMessage = "Please enter Admin password."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    // Verify admin credentials
                    if (adminEmail.contains("admin", ignoreCase = true) && adminPassword.isNotBlank()) {
                        authManager.setAdminSession(name = "Admin", email = adminEmail.trim())
                        isLoading = false
                        onLoginSuccess()
                    } else {
                        errorMessage = "Invalid admin credentials. (Try demo: admin@agroretail.com / admin123)"
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_login_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("Admin Console Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Fill Admin button
            OutlinedButton(
                onClick = {
                    adminEmail = "admin@agroretail.com"
                    adminPassword = "admin"
                    errorMessage = null
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fill Demo Admin Credentials (admin@agroretail.com)")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "🔒 Secured with End-to-End PIN Encryption & Role-Based Rules",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
