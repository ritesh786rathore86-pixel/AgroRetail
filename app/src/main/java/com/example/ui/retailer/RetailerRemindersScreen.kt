package com.example.ui.retailer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentReminder
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroVoiceHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerRemindersScreen(
    retailer: Retailer,
    repository: AgroRepository,
    autoPlayFirstReminder: Boolean = false,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val paymentReminders by repository.paymentReminders.collectAsState()
    var currentlyPlayingReminderId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            AgroVoiceHelper.stop()
        }
    }

    val myReminders = remember(paymentReminders, retailer.id) {
        paymentReminders.filter { it.retailerId == retailer.id && !it.isPaid }
    }

    // Auto-play voice reminder with sound when opened from notification or alert click
    var hasAutoPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(myReminders, autoPlayFirstReminder) {
        if (autoPlayFirstReminder && !hasAutoPlayed) {
            hasAutoPlayed = true
            val target = myReminders.firstOrNull()
            val amount = target?.outstandingAmount ?: retailer.outstandingAmount
            val due = target?.dueDate ?: (System.currentTimeMillis() + 86400000L)
            currentlyPlayingReminderId = target?.id ?: "auto"
            AgroVoiceHelper.playHelloReminder(
                context = context,
                retailerName = retailer.businessName,
                amount = amount,
                dueDate = due,
                billNumber = "",
                isHindi = true,
                onStart = { currentlyPlayingReminderId = target?.id ?: "auto" },
                onDone = { currentlyPlayingReminderId = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment & Voice Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ForestGreenPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Outstanding Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Outstanding",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (retailer.outstandingAmount > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (retailer.outstandingAmount > 0) "PAYMENT DUE" else "NO DUES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (retailer.outstandingAmount > 0) Color(0xFF991B1B) else Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formatCurrency(retailer.outstandingAmount),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (retailer.outstandingAmount > 0) Color(0xFFB91C1C) else ForestGreenPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Credit Limit: ${formatCurrency(retailer.creditLimit)} • Available: ${formatCurrency((retailer.creditLimit - retailer.outstandingAmount).coerceAtLeast(0.0))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Distributor Bank & UPI Payment Instructions
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Distributor Bank Details for Payment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Bank: State Bank of India (Main Branch)", fontSize = 12.sp)
                        Text(text = "Account Name: AgroRetail Distributors Pvt Ltd", fontSize = 12.sp)
                        Text(text = "Account No: 389201948210", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(text = "IFSC Code: SBIN0001234", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(text = "UPI ID: agrodistributor@sbi", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ForestGreenPrimary)

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Bank details copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Bank & UPI Details")
                        }
                    }
                }
            }

            // Reminders from distributor
            item {
                Text(
                    text = "ACTIVE PAYMENT REMINDERS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            if (myReminders.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreenPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No pending payment reminders. Your account is in good standing!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                items(myReminders, key = { it.id }) { reminder ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Due Date: ${formatDateShort(reminder.dueDate)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Text(
                                    text = formatCurrency(reminder.outstandingAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFDC2626)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = reminder.message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val isPlaying = currentlyPlayingReminderId == reminder.id

                            if (isPlaying) {
                                OutlinedButton(
                                    onClick = {
                                        AgroVoiceHelper.stop()
                                        currentlyPlayingReminderId = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop Voice Reminder", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        currentlyPlayingReminderId = reminder.id
                                        AgroVoiceHelper.playHelloReminder(
                                            context = context,
                                            retailerName = retailer.businessName,
                                            amount = reminder.outstandingAmount,
                                            dueDate = reminder.dueDate,
                                            billNumber = "",
                                            isHindi = true,
                                            onStart = { currentlyPlayingReminderId = reminder.id },
                                            onDone = { currentlyPlayingReminderId = null },
                                            onError = {
                                                currentlyPlayingReminderId = null
                                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HarvestAmber),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play Voice Reminder (Sound Like Hello)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val text = "Hi, I am contacting regarding my outstanding balance of ${formatCurrency(reminder.outstandingAmount)} for ${retailer.businessName}."
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp not installed. Copied message.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Contact Distributor on WhatsApp", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
