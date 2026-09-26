package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.Retailer
import com.example.data.model.RetailerStatus
import com.example.data.repository.AgroRepository
import com.example.data.repository.AuthManager
import com.example.ui.components.GanpatiAgroBrandLogo
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authManager: AuthManager,
    repository: AgroRepository,
    onLoginSuccess: () -> Unit
) {
    var selectedRoleTab by remember { mutableIntStateOf(0) } // 0 = Retailer, 1 = Admin

    // Retailer login state
    var retailerLoginInput by remember { mutableStateOf("") } // Mobile or Party Code
    var retailerPassword by remember { mutableStateOf("") }
    var retailerPasswordVisible by remember { mutableStateOf(false) }

    // Admin login state
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dialog states
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showPendingApprovalDialog by remember { mutableStateOf<Retailer?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Sacred & Auspicious Header Banner
        Text(
            text = "॥ श्री गणेशाय नमः ॥",
            color = HarvestAmber,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Official Ganpati-inspired agriculture logo
        GanpatiAgroBrandLogo(
            size = 84.dp,
            showBorder = true,
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SV AGRO SHOPE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = ForestGreenPrimary,
            letterSpacing = 0.5.sp
        )

        Text(
            text = "Retailer Ordering Platform",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Role Switcher Tab: Retailer vs Admin
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Button(
                    onClick = {
                        selectedRoleTab = 0
                        errorMessage = null
                        successNotice = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("retailer_tab_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRoleTab == 0) ForestGreenPrimary else Color.Transparent,
                        contentColor = if (selectedRoleTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = if (selectedRoleTab == 0) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retailer Login", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        selectedRoleTab = 1
                        errorMessage = null
                        successNotice = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_tab_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRoleTab == 1) ForestGreenPrimary else Color.Transparent,
                        contentColor = if (selectedRoleTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = if (selectedRoleTab == 1) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin Login", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Success Alert
        if (successNotice != null) {
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = successNotice ?: "", color = Color(0xFF15803D), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Error message card
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (selectedRoleTab == 0) {
            // ==========================================
            // RETAILER LOGIN FORM
            // ==========================================
            OutlinedTextField(
                value = retailerLoginInput,
                onValueChange = { retailerLoginInput = it },
                label = { Text("Mobile Number / Retailer ID") },
                placeholder = { Text("10-digit mobile number") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenPrimary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mobile_number_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = retailerPassword,
                onValueChange = { retailerPassword = it },
                label = { Text("Password / PIN") },
                placeholder = { Text("Enter your account password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { retailerPasswordVisible = !retailerPasswordVisible }) {
                        Icon(
                            imageVector = if (retailerPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (retailerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Forgot Password Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        errorMessage = null
                        showForgotPasswordDialog = true
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = ForestGreenPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (retailerLoginInput.isBlank()) {
                        errorMessage = "Please enter your registered 10-digit mobile number."
                        return@Button
                    }
                    if (retailerPassword.isBlank()) {
                        errorMessage = "Please enter your account password."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val retailer = repository.findRetailerByLogin(retailerLoginInput)
                    if (retailer == null) {
                        errorMessage = "No registered account found with '$retailerLoginInput'. Please tap 'Register New Retailer' below."
                        isLoading = false
                        return@Button
                    }

                    // Check approval status
                    when (retailer.status) {
                        RetailerStatus.PENDING -> {
                            isLoading = false
                            showPendingApprovalDialog = retailer
                            return@Button
                        }
                        RetailerStatus.REJECTED -> {
                            isLoading = false
                            val reason = retailer.rejectionReason.ifBlank { "Verification details did not match distributor criteria." }
                            errorMessage = "Account Registration Rejected: $reason. Please contact distributor support."
                            return@Button
                        }
                        RetailerStatus.DISABLED -> {
                            isLoading = false
                            errorMessage = "This account has been disabled by distributor. Please contact admin."
                            return@Button
                        }
                        RetailerStatus.ACTIVE -> {
                            if (!retailer.isActive) {
                                isLoading = false
                                errorMessage = "Account is currently suspended. Please contact distributor office."
                                return@Button
                            }
                        }
                    }

                    val isCorrect = repository.verifyRetailerLogin(retailer, retailerPassword)
                    if (!isCorrect) {
                        isLoading = false
                        errorMessage = "Incorrect password. Please verify and retry or use Forgot Password."
                        return@Button
                    }

                    // Login successful
                    authManager.setRetailerSession(retailer)
                    isLoading = false
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("retailer_login_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Login to My Shop", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Create Retailer Account Divider & Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  NEW RETAILER?  ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    errorMessage = null
                    showRegistrationDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_retailer_account_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ForestGreenPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ForestGreenPrimary)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Retailer Account", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

        } else {
            // ==========================================
            // ADMIN LOGIN FORM
            // ==========================================
            OutlinedTextField(
                value = adminEmail,
                onValueChange = { adminEmail = it },
                label = { Text("Admin Email / Username") },
                placeholder = { Text("Enter distributor admin username") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_email_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it },
                label = { Text("Admin Password") },
                placeholder = { Text("Enter admin access key") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ForestGreenPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                        Icon(
                            imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (adminEmail.isBlank()) {
                        errorMessage = "Please enter Admin username or email."
                        return@Button
                    }
                    if (adminPassword.isBlank()) {
                        errorMessage = "Please enter Admin password."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    if ((adminEmail.contains("admin", ignoreCase = true) || adminEmail.contains("distributor", ignoreCase = true)) && adminPassword.isNotBlank()) {
                        authManager.setAdminSession(name = "SV Agro Distributor Admin", email = adminEmail.trim())
                        isLoading = false
                        onLoginSuccess()
                    } else {
                        errorMessage = "Invalid admin credentials. Please enter valid distributor authorization."
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_login_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Console Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Security assurance footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Protected B2B Agriculture Ordering Network",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // ==========================================
    // RETAILER REGISTRATION DIALOG
    // ==========================================
    if (showRegistrationDialog) {
        var regName by remember { mutableStateOf("") }
        var regMobile by remember { mutableStateOf("") }
        var regBusinessName by remember { mutableStateOf("") }
        var regAddress by remember { mutableStateOf("") }
        var regLicense by remember { mutableStateOf("") }
        var regPassword by remember { mutableStateOf("") }
        var regConfirmPassword by remember { mutableStateOf("") }
        var regError by remember { mutableStateOf<String?>(null) }
        var isSubmittingReg by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isSubmittingReg) showRegistrationDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Create Retailer Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = "Subject to Distributor Approval",
                                fontSize = 12.sp,
                                color = HarvestAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { showRegistrationDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (regError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = regError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Owner / Contact Person Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regMobile,
                        onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) regMobile = it },
                        label = { Text("10-Digit Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regBusinessName,
                        onValueChange = { regBusinessName = it },
                        label = { Text("Shop / Krishi Kendra Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regAddress,
                        onValueChange = { regAddress = it },
                        label = { Text("Shop Address / Town / Village *") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regLicense,
                        onValueChange = { regLicense = it },
                        label = { Text("Fertilizer / Pesticide License No. (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Choose Password (Min 4 chars) *") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = { regConfirmPassword = it },
                        label = { Text("Confirm Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (regName.isBlank()) {
                                regError = "Please enter owner name."
                                return@Button
                            }
                            if (regMobile.length != 10) {
                                regError = "Please enter a valid 10-digit mobile number."
                                return@Button
                            }
                            if (regBusinessName.isBlank()) {
                                regError = "Please enter shop/firm name."
                                return@Button
                            }
                            if (regAddress.isBlank()) {
                                regError = "Please enter shop address."
                                return@Button
                            }
                            if (regPassword.length < 4) {
                                regError = "Password must be at least 4 characters."
                                return@Button
                            }
                            if (regPassword != regConfirmPassword) {
                                regError = "Passwords do not match."
                                return@Button
                            }

                            isSubmittingReg = true
                            regError = null

                            val (success, msg) = repository.registerRetailer(
                                name = regName,
                                mobile = regMobile,
                                businessName = regBusinessName,
                                address = regAddress,
                                password = regPassword,
                                licenseNumber = regLicense
                            )

                            isSubmittingReg = false
                            if (success) {
                                showRegistrationDialog = false
                                successNotice = msg
                                retailerLoginInput = regMobile
                            } else {
                                regError = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmittingReg
                    ) {
                        if (isSubmittingReg) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Submit for Approval", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // FORGOT PASSWORD DIALOG
    // ==========================================
    if (showForgotPasswordDialog) {
        var forgotMobile by remember { mutableStateOf(retailerLoginInput) }
        var forgotReason by remember { mutableStateOf("") }
        var forgotStatus by remember { mutableStateOf<String?>(null) }
        var isSubmittingForgot by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reset Password Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                        IconButton(onClick = { showForgotPasswordDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Submit a request to Distributor Admin to reset your account password. Admin will verify your shop details.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (forgotStatus != null) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = forgotStatus ?: "",
                                color = Color(0xFF15803D),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = forgotMobile,
                        onValueChange = { forgotMobile = it },
                        label = { Text("Registered Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = forgotReason,
                        onValueChange = { forgotReason = it },
                        label = { Text("Note / Shop Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (forgotMobile.length != 10) {
                                forgotStatus = "Please enter valid 10-digit registered mobile."
                                return@Button
                            }
                            isSubmittingForgot = true
                            val (ok, resMsg) = repository.requestPasswordReset(forgotMobile, forgotReason)
                            isSubmittingForgot = false
                            forgotStatus = resMsg
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        enabled = !isSubmittingForgot
                    ) {
                        Text("Send Request to Admin", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ==========================================
    // PENDING APPROVAL MODAL
    // ==========================================
    showPendingApprovalDialog?.let { ret ->
        Dialog(onDismissRequest = { showPendingApprovalDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF9C3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = HarvestAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Account Pending Approval",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Namaste ${ret.retailerName} ji,\nYour registration for '${ret.businessName}' has been received and is currently waiting for Distributor Admin approval.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Shop: ${ret.businessName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Mobile: ${ret.mobileNumber}", fontSize = 12.sp)
                            Text(text = "Status: Pending Review", fontSize = 12.sp, color = HarvestAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showPendingApprovalDialog = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text("Understood")
                    }
                }
            }
        }
    }
}
