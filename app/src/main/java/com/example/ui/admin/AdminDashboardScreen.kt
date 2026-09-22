package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.data.repository.AgroRepository
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.MintLight

@Composable
fun AdminDashboardScreen(
    repository: AgroRepository,
    onNavigateToProducts: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToRetailers: () -> Unit,
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToCompanies: () -> Unit = {},
    onNavigateToExcelImport: () -> Unit,
    onNavigateToBills: () -> Unit = {},
    onNavigateToPosters: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val products by repository.products.collectAsState()
    val retailers by repository.retailers.collectAsState()
    val orders by repository.orders.collectAsState()
    val manualDocs by repository.manualDocuments.collectAsState()
    val posters by repository.posters.collectAsState()
    val distributorProfile by repository.distributorProfile.collectAsState()

    val totalOutstanding = remember(retailers) { retailers.sumOf { it.outstandingAmount } }
    val pendingOrdersCount = remember(orders) { orders.count { it.status == OrderStatus.Pending } }
    val activeRetailersCount = remember(retailers) { retailers.count { it.isActive } }
    val activeProductsCount = remember(products) { products.count { it.isActive } }
    val activePostersCount = remember(posters) { posters.count { it.isActive } }

    val recentOrders = remember(orders) { orders.take(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ForestGreenPrimary, Color(0xFF0F4E2B))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ADMIN CONSOLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldenSun,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = distributorProfile.companyName.ifBlank { "Siddhi Vinayak Krishi Vikas Kendra" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Market Outstanding: ${formatCurrency(totalOutstanding)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = GoldenSun, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prominent Firm Profile Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = distributorProfile.companyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Prop: ${distributorProfile.ownerName} • GST: ${distributorProfile.gstin}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Profile", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick KPI Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Pending Orders",
                value = "$pendingOrdersCount",
                subtext = "${orders.size} total orders",
                icon = Icons.Default.PendingActions,
                iconTint = if (pendingOrdersCount > 0) Color(0xFFD97706) else ForestGreenPrimary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToOrders
            )
            StatCard(
                title = "Active Retailers",
                value = "$activeRetailersCount / ${retailers.size}",
                subtext = "Registered dealers",
                icon = Icons.Default.Storefront,
                iconTint = Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToRetailers
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Products Master",
                value = "$activeProductsCount / ${products.size}",
                subtext = "Active items in catalog",
                icon = Icons.Default.Inventory2,
                iconTint = ForestGreenPrimary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProducts
            )
            StatCard(
                title = "Documents",
                value = "${manualDocs.size} Uploaded",
                subtext = "Bills & Statements",
                icon = Icons.Default.Description,
                iconTint = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDocuments
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val dueRetailersCount = remember(retailers) { retailers.count { it.outstandingAmount > 0 } }
            StatCard(
                title = "Payment Reminders",
                value = "$dueRetailersCount Retailers Due",
                subtext = "🔊 Voice 'Hello' & WhatsApp",
                icon = Icons.Default.RecordVoiceOver,
                iconTint = HarvestAmber,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToReminders
            )
            StatCard(
                title = "Posters & Schemes",
                value = "$activePostersCount Active",
                subtext = "${posters.size} total app banners",
                icon = Icons.Default.Campaign,
                iconTint = Color(0xFF059669),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToPosters
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Actions Row
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Featured Priority Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionChip(
                label = "🔊 Reminders",
                icon = Icons.Default.RecordVoiceOver,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToReminders
            )
            AdminActionChip(
                label = "📢 Posters",
                icon = Icons.Default.Campaign,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToPosters
            )
            AdminActionChip(
                label = "📄 PDF Orders",
                icon = Icons.Default.PictureAsPdf,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToOrders
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionChip(
                label = "+ Product",
                icon = Icons.Default.AddBox,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProducts
            )
            AdminActionChip(
                label = "Excel Import",
                icon = Icons.Default.UploadFile,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToExcelImport
            )
            AdminActionChip(
                label = "Documents",
                icon = Icons.Default.Upload,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDocuments
            )
            AdminActionChip(
                label = "Categories",
                icon = Icons.Default.WorkspacePremium,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCategories
            )
            AdminActionChip(
                label = "Companies",
                icon = Icons.Default.Business,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCompanies
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Orders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT INCOMING ORDERS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            TextButton(onClick = onNavigateToOrders) {
                Text("View All (${orders.size})")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (recentOrders.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No orders received yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp
                )
            }
        } else {
            recentOrders.forEach { order ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(onClick = onNavigateToOrders)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = order.id,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ForestGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OrderStatusBadge(status = order.status)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${order.retailerBusinessName} (${order.retailerName})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${order.items.size} item(s) • ${formatDateShort(order.dateTime)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = formatCurrency(order.grandTotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ForestGreenPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
