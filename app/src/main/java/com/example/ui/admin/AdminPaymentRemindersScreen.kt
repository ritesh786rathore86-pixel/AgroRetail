package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PaymentReminder
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroVoiceHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentRemindersScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val retailers by repository.retailers.collectAsState()
    val reminders by repository.paymentReminders.collectAsState()

    var isCreatingReminder by remember { mutableStateOf(false) }
    var initialTargetMode by remember { mutableStateOf("ALL_DUE") }
    var currentlyPlayingReminderId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            AgroVoiceHelper.stop()
        }
    }

    val dueRetailers = remember(retailers) { retailers.filter { it.outstandingAmount > 0 } }
    val totalPendingAmount = remember(dueRetailers) { dueRetailers.sumOf { it.outstandingAmount } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    initialTargetMode = if (dueRetailers.isNotEmpty()) "ALL_DUE" else "SINGLE"
                    isCreatingReminder = true
                },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.NotificationAdd, contentDescription = "Send Reminder")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Broadcast Banner for All Parties
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pending Receivables",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatCurrency(totalPendingAmount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPendingAmount > 0) Color(0xFFDC2626) else ForestGreenPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (dueRetailers.isNotEmpty()) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "${dueRetailers.size} Parties Due",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dueRetailers.isNotEmpty()) Color(0xFF991B1B) else Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            initialTargetMode = "ALL_DUE"
                            isCreatingReminder = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestAmber),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remind All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (reminders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.NotificationsActive,
                    title = "No Reminders Sent",
                    message = "Notify retailers with pending balances via push notification and WhatsApp.",
                    actionButtonText = "+ Send Payment Reminder",
                    onActionClick = {
                        initialTargetMode = if (dueRetailers.isNotEmpty()) "ALL_DUE" else "SINGLE"
                        isCreatingReminder = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
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
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.retailerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Due: ${formatDateShort(reminder.dueDate)} • Sent: ${formatDateShort(reminder.createdAt)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = formatCurrency(reminder.outstandingAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = reminder.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isPlaying = currentlyPlayingReminderId == reminder.id

                                    if (isPlaying) {
                                        OutlinedButton(
                                            onClick = {
                                                AgroVoiceHelper.stop()
                                                currentlyPlayingReminderId = null
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Stop Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                currentlyPlayingReminderId = reminder.id
                                                AgroVoiceHelper.playHelloReminder(
                                                    context = context,
                                                    retailerName = reminder.retailerName,
                                                    amount = reminder.outstandingAmount,
                                                    dueDate = reminder.dueDate,
                                                    billNumber = "",
                                                    isHindi = true,
                                                    onStart = { currentlyPlayingReminderId = reminder.id },
                                                    onDone = { currentlyPlayingReminderId = null },
                                                    onError = { err ->
                                                        currentlyPlayingReminderId = null
                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = HarvestAmber),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play Hello Sound", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val text = "Dear ${reminder.retailerName}, gentle reminder regarding your outstanding balance of ${formatCurrency(reminder.outstandingAmount)} due on ${formatDateShort(reminder.dueDate)}. Please clear at your earliest convenience."
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=+91${reminder.retailerMobile}&text=${Uri.encode(text)}")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                                    }

                                    FilledTonalIconButton(
                                        onClick = {
                                            AgroVoiceHelper.synthesizeAndShareHelloVoice(
                                                context = context,
                                                retailerName = reminder.retailerName,
                                                amount = reminder.outstandingAmount,
                                                dueDate = reminder.dueDate,
                                                isHindi = true
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Share Voice Audio", modifier = Modifier.size(16.dp), tint = ForestGreenPrimary)
                                    }

                                    IconButton(
                                        onClick = {
                                            repository.deletePaymentReminder(reminder.id)
                                            Toast.makeText(context, "Reminder dismissed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreatingReminder) {
        CreateReminderDialog(
            retailers = dueRetailers,
            allRetailers = retailers,
            initialTargetMode = initialTargetMode,
            onDismiss = { isCreatingReminder = false },
            onSendBulk = { targetParties, msg, dueDays ->
                val due = System.currentTimeMillis() + (dueDays * 86400000L)
                repository.sendBulkPaymentReminders(targetParties, msg, due)
                Toast.makeText(
                    context,
                    "Payment reminder broadcast sent to ${targetParties.size} party(s)!",
                    Toast.LENGTH_LONG
                ).show()
                isCreatingReminder = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderDialog(
    retailers: List<Retailer>,
    allRetailers: List<Retailer>,
    initialTargetMode: String = "ALL_DUE",
    onDismiss: () -> Unit,
    onSendBulk: (List<Retailer>, String, Int) -> Unit
) {
    val context = LocalContext.current
    var targetMode by remember { mutableStateOf(initialTargetMode) } // "ALL_DUE", "ALL_RETAILERS", "SINGLE"
    var selectedRetailer by remember { mutableStateOf<Retailer?>(retailers.firstOrNull() ?: allRetailers.firstOrNull()) }
    var dueDays by remember { mutableIntStateOf(3) }
    var message by remember {
        mutableStateOf(
            "Gentle reminder regarding your pending invoice outstanding balance. Please deposit the amount to avoid dispatch delays."
        )
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    val resolvedTargetList = remember(targetMode, selectedRetailer, retailers, allRetailers) {
        when (targetMode) {
            "ALL_DUE" -> if (retailers.isNotEmpty()) retailers else allRetailers
            "ALL_RETAILERS" -> allRetailers
            else -> listOfNotNull(selectedRetailer)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Send Payment Reminder", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ForestGreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Select Party / Target:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetMode == "ALL_DUE",
                        onClick = { targetMode = "ALL_DUE" },
                        label = { Text("All Due (${retailers.size})", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = targetMode == "ALL_RETAILERS",
                        onClick = { targetMode = "ALL_RETAILERS" },
                        label = { Text("All (${allRetailers.size})", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = targetMode == "SINGLE",
                        onClick = { targetMode = "SINGLE" },
                        label = { Text("Single Party", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (targetMode == "SINGLE") {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRetailer?.let { "${it.businessName} (₹${it.outstandingAmount.toInt()})" } ?: "Select Retailer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Retailer *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            allRetailers.forEach { ret ->
                                DropdownMenuItem(
                                    text = { Text("${ret.businessName} (Due: ${formatCurrency(ret.outstandingAmount)})") },
                                    onClick = {
                                        selectedRetailer = ret
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (targetMode == "ALL_DUE") "Sending to all ${resolvedTargetList.size} parties with pending dues" else "Sending broadcast to all ${resolvedTargetList.size} registered retailers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary
                            )
                            val totalDues = resolvedTargetList.sumOf { it.outstandingAmount }
                            Text(
                                text = "Combined Total Outstandings: ${formatCurrency(totalDues)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Payment Due Within:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(2, 3, 5, 7).forEach { days ->
                        FilterChip(
                            selected = dueDays == days,
                            onClick = { dueDays = days },
                            label = { Text("$days Days") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Reminder Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                val samplePartyName = selectedRetailer?.businessName ?: "Kisan Agro Agency"
                val sampleAmount = selectedRetailer?.outstandingAmount ?: 15400.0
                val sampleDue = System.currentTimeMillis() + (dueDays * 86400000L)
                var isTestPlaying by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HarvestAmber.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hello Sound Greeting Ready",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (isTestPlaying) {
                                    AgroVoiceHelper.stop()
                                    isTestPlaying = false
                                } else {
                                    isTestPlaying = true
                                    AgroVoiceHelper.playHelloReminder(
                                        context = context,
                                        retailerName = samplePartyName,
                                        amount = sampleAmount,
                                        dueDate = sampleDue,
                                        isHindi = true,
                                        onStart = { isTestPlaying = true },
                                        onDone = { isTestPlaying = false },
                                        onError = {
                                            isTestPlaying = false
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                if (isTestPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTestPlaying) "Stop" else "Test Hello", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (resolvedTargetList.isNotEmpty() && message.isNotBlank()) {
                                onSendBulk(resolvedTargetList, message, dueDays)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text(
                            text = if (resolvedTargetList.size > 1) "Send (${resolvedTargetList.size})" else "Send Push"
                        )
                    }
                }
            }
        }
    }
}
