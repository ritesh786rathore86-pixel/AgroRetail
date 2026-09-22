package com.example.ui.retailer

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatDate
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerNotificationsScreen(
    retailer: Retailer,
    repository: AgroRepository,
    onNavigateToOrders: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToPassbook: () -> Unit,
    onNavigateToReminders: () -> Unit
) {
    val notifications by repository.notifications.collectAsState()

    val myNotifs = remember(notifications, retailer.id) {
        notifications.filter {
            it.targetRetailerId == retailer.id || it.targetRetailerId == "ALL"
        }.sortedByDescending { it.timestamp }
    }

    val unreadCount = remember(myNotifs) { myNotifs.count { !it.isRead } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications (${myNotifs.size})", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                ),
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { repository.markAllNotificationsRead(retailer.id) }) {
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
            if (myNotifs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.NotificationsNone,
                    title = "No Notifications",
                    message = "You are all caught up! Updates regarding orders, bills, and special offers will appear here.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(myNotifs, key = { it.id }) { notif ->
                        NotificationItemCard(
                            notification = notif,
                            onClick = {
                                repository.markNotificationRead(notif.id)
                                when (notif.linkedType) {
                                    "ORDER" -> onNavigateToOrders()
                                    "BILL" -> onNavigateToBills()
                                    "STATEMENT", "PASSBOOK" -> onNavigateToPassbook()
                                    "PAYMENT_REMINDER" -> onNavigateToReminders()
                                }
                            },
                            onDelete = { repository.deleteNotification(notif.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon based on notification type
            val (icon, tint) = when (notification.linkedType) {
                "ORDER" -> Icons.Default.LocalShipping to ForestGreenPrimary
                "BILL" -> Icons.Default.Receipt to Color(0xFF0284C7)
                "STATEMENT" -> Icons.Default.Description to Color(0xFF7C3AED)
                "POSTER" -> Icons.Default.Campaign to HarvestAmber
                "PAYMENT_REMINDER" -> Icons.Default.Warning to Color(0xFFDC2626)
                else -> Icons.Default.Notifications to ForestGreenPrimary
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
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
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(notification.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
