package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.AppNotification
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatDate
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationsScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val notifications by repository.notifications.collectAsState()
    val retailers by repository.retailers.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Inbox & Alerts, 1 = Broadcast Push

    val adminInboxNotifs = remember(notifications) {
        notifications.filter { it.targetRetailerId == "ADMIN" }
            .sortedByDescending { it.timestamp }
    }
    val unreadInboxCount = remember(adminInboxNotifs) { adminInboxNotifs.count { !it.isRead } }

    val broadcastSentNotifs = remember(notifications) {
        notifications.filter { it.targetRetailerId != "ADMIN" }
            .sortedByDescending { it.timestamp }
    }

    // Broadcast composer state
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetMode by remember { mutableStateOf("ALL") } // "ALL" or "SPECIFIC"
    var selectedRetailer by remember { mutableStateOf<Retailer?>(retailers.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications & Push Broadcast", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                ),
                actions = {
                    if (selectedTab == 0 && unreadInboxCount > 0) {
                        TextButton(onClick = { repository.markAllNotificationsRead("ADMIN") }) {
                            Text("Mark All Read")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Row: Inbox Alerts vs Broadcast Push
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreenPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin Inbox (${adminInboxNotifs.size})")
                            if (unreadInboxCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$unreadInboxCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Broadcast Push")
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // ============================================================
                // TAB 0: ADMIN INBOX & SYSTEM ALERTS
                // ============================================================
                if (adminInboxNotifs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Default.NotificationsNone,
                            title = "No Incoming Alerts",
                            message = "Orders, retailer registration requests, and chat alerts will appear here."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(adminInboxNotifs, key = { it.id }) { notif ->
                            AdminInboxNotificationCard(
                                notification = notif,
                                onMarkRead = { repository.markNotificationRead(notif.id) },
                                onDelete = { repository.deleteNotification(notif.id) }
                            )
                        }
                    }
                }
            } else {
                // ============================================================
                // TAB 1: BROADCAST PUSH COMPOSER & SENT HISTORY
                // ============================================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
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
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
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
                                        if (title.isBlank() || message.isBlank()) {
                                            Toast.makeText(context, "Please provide title and message", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val targetId = if (targetMode == "ALL") "ALL" else selectedRetailer?.id ?: "ALL"
                                        repository.sendNotification(
                                            title = title.trim(),
                                            message = message.trim(),
                                            targetRetailerId = targetId,
                                            linkedType = "BROADCAST"
                                        )

                                        title = ""
                                        message = ""
                                        Toast.makeText(context, "Push notification sent successfully!", Toast.LENGTH_SHORT).show()
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
                    }

                    item {
                        Text(
                            text = "RECENT SENT BROADCASTS (${broadcastSentNotifs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(broadcastSentNotifs, key = { it.id }) { notif ->
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
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminInboxNotificationCard(
    notification: AppNotification,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface else ForestGreenPrimary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkRead() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val icon = when (notification.linkedType) {
                "ORDER" -> Icons.Default.LocalShipping
                "CHAT" -> Icons.Default.Chat
                "PASSWORD_RESET" -> Icons.Default.LockReset
                "REGISTRATION" -> Icons.Default.PersonAdd
                else -> Icons.Default.Notifications
            }
            val tint = when (notification.linkedType) {
                "ORDER" -> ForestGreenPrimary
                "CHAT" -> Color(0xFF2563EB)
                "PASSWORD_RESET" -> Color(0xFFDC2626)
                "REGISTRATION" -> GoldenSun
                else -> HarvestAmber
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (!notification.isRead) {
                        Surface(
                            color = ForestGreenPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(notification.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
