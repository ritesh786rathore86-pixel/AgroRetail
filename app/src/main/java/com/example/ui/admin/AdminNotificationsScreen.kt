package com.example.ui.admin

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.formatDate
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationsScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val notifications by repository.notifications.collectAsState()
    val retailers by repository.retailers.collectAsState()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetMode by remember { mutableStateOf("ALL") } // "ALL" or "SPECIFIC"
    var selectedRetailer by remember { mutableStateOf<Retailer?>(retailers.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcast Push Notifications", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // New Notification Form Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Send Instant Push Notification",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = targetMode == "ALL",
                            onClick = { targetMode = "ALL" },
                            label = { Text("All Retailers (${retailers.size})") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = targetMode == "SPECIFIC",
                            onClick = { targetMode = "SPECIFIC" },
                            label = { Text("Single Retailer") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (targetMode == "SPECIFIC") {
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedRetailer?.businessName ?: "Select Target Retailer",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                retailers.forEach { ret ->
                                    DropdownMenuItem(
                                        text = { Text(ret.businessName) },
                                        onClick = {
                                            selectedRetailer = ret
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title *") },
                        placeholder = { Text("e.g. New Bayer Stock Arrived / Price Drop Alert") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Notification Body *") },
                        placeholder = { Text("Enter the details of the update or offer...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (title.isBlank() || message.isBlank()) return@Button

                            if (targetMode == "ALL") {
                                repository.broadcastPushNotification(
                                    title = title.trim(),
                                    message = message.trim(),
                                    linkedType = "GENERAL"
                                )
                                Toast.makeText(context, "Push broadcasted to all ${retailers.size} retailers!", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedRetailer?.let { ret ->
                                    repository.sendPushNotificationToRetailer(
                                        retailerId = ret.id,
                                        title = title.trim(),
                                        message = message.trim(),
                                        linkedType = "GENERAL"
                                    )
                                    Toast.makeText(context, "Notification sent to ${ret.businessName}!", Toast.LENGTH_SHORT).show()
                                }
                            }

                            title = ""
                            message = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Notification Now")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RECENT SENT NOTIFICATIONS (${notifications.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Target: ${if (notif.targetRetailerId == "ALL") "All Retailers" else notif.targetRetailerId} • ${formatDate(notif.timestamp)}",
                                    fontSize = 10.sp,
                                    color = HarvestAmber
                                )
                            }
                            IconButton(onClick = { repository.deleteNotification(notif.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
