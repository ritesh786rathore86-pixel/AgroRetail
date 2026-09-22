package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroPdfHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val orders by repository.orders.collectAsState()
    val retailers by repository.retailers.collectAsState()
    val distributorProfile by repository.distributorProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var selectedOrderForUpdate by remember { mutableStateOf<Order?>(null) }
    var orderForPdfPreview by remember { mutableStateOf<Order?>(null) }

    fun openOrderPdf(order: Order) {
        try {
            val retailer = retailers.find { it.id == order.retailerId }
            val pdfFile = AgroPdfHelper.getOrGenerateOrderPdf(
                context = context,
                order = order,
                retailer = retailer,
                distributor = distributorProfile,
                hideGst = true
            )
            AgroPdfHelper.openPdf(context, pdfFile, "Order_${order.id}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Order PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareOrderPdf(order: Order) {
        try {
            val retailer = retailers.find { it.id == order.retailerId }
            val pdfFile = AgroPdfHelper.getOrGenerateOrderPdf(
                context = context,
                order = order,
                retailer = retailer,
                distributor = distributorProfile,
                hideGst = true
            )
            AgroPdfHelper.sharePdf(context, pdfFile, "Order Confirmation #${order.id}")
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share Order PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredOrders = remember(orders, searchQuery, selectedStatusFilter) {
        orders.filter { order ->
            (selectedStatusFilter == null || order.status == selectedStatusFilter) &&
            (searchQuery.isBlank() ||
                order.id.contains(searchQuery, ignoreCase = true) ||
                order.retailerBusinessName.contains(searchQuery, ignoreCase = true) ||
                order.retailerName.contains(searchQuery, ignoreCase = true) ||
                order.retailerMobile.contains(searchQuery, ignoreCase = true)
            )
        }.sortedByDescending { it.dateTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Orders (${orders.size})", fontWeight = FontWeight.Bold) },
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
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Order ID, Dealer Name, Mobile...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreenPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Status Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All (${orders.size})") }
                    )
                }
                items(OrderStatus.values()) { status ->
                    val count = orders.count { it.status == status }
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text("${status.name} ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredOrders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "No Orders Match",
                    message = "No orders found matching the selected filter criteria.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOrderForUpdate = order }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = order.id, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreenPrimary)
                                        Text(text = formatDate(order.dateTime), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OrderStatusBadge(status = order.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${order.retailerBusinessName} (${order.retailerName}) • +91 ${order.retailerMobile}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = order.items.joinToString(", ") { "${it.quantity}x ${it.itemName}" },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )

                                if (order.notes.isNotBlank()) {
                                    Text(
                                        text = "Retailer Note: ${order.notes}",
                                        fontSize = 11.sp,
                                        color = HarvestAmber
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Grand Total",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatCurrency(order.grandTotal),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = ForestGreenPrimary
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalButton(
                                            onClick = { openOrderPdf(order) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = ForestGreenPrimary.copy(alpha = 0.12f),
                                                contentColor = ForestGreenPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalIconButton(
                                            onClick = { shareOrderPdf(order) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share PDF", modifier = Modifier.size(15.dp), tint = ForestGreenPrimary)
                                        }

                                        Button(
                                            onClick = { selectedOrderForUpdate = order },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Update Status & Notes Dialog
    selectedOrderForUpdate?.let { order ->
        var currentStatus by remember { mutableStateOf(order.status) }
        var adminNotes by remember { mutableStateOf(order.adminNotes) }

        Dialog(onDismissRequest = { selectedOrderForUpdate = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Update Order ${order.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { selectedOrderForUpdate = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Retailer: ${order.retailerBusinessName}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Amount: ${formatCurrency(order.grandTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ForestGreenPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Order PDF Document",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                                Text(
                                    text = "Official print copy with items & totals",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { openOrderPdf(order) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View PDF", fontSize = 11.sp)
                                }
                                FilledTonalIconButton(
                                    onClick = { shareOrderPdf(order) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(15.dp), tint = ForestGreenPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Order Items (${order.items.size}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            order.items.forEach { itm ->
                                Text(
                                    text = "• ${itm.itemName} (${itm.packSize}): ${itm.quantity} pcs @ ${formatCurrency(itm.rate)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Select Order Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    OrderStatus.values().forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentStatus = status }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentStatus == status,
                                onClick = { currentStatus = status }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OrderStatusBadge(status = status)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = adminNotes,
                        onValueChange = { adminNotes = it },
                        label = { Text("Admin Note / Remarks") },
                        placeholder = { Text("e.g. Order approved, delivery scheduled") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                repository.deleteOrder(order.id)
                                Toast.makeText(context, "Order ${order.id} deleted", Toast.LENGTH_SHORT).show()
                                selectedOrderForUpdate = null
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Order", tint = MaterialTheme.colorScheme.error)
                        }

                        OutlinedButton(
                            onClick = { selectedOrderForUpdate = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                repository.updateOrderStatus(order.id, currentStatus, adminNotes)
                                Toast.makeText(context, "Order status updated to ${currentStatus.name}!", Toast.LENGTH_SHORT).show()
                                selectedOrderForUpdate = null
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Text("Save Status")
                        }
                    }
                }
            }
        }
    }
}
