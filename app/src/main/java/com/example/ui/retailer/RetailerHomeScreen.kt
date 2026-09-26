package com.example.ui.retailer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.*

@Composable
fun RetailerHomeScreen(
    retailer: Retailer,
    repository: AgroRepository,
    onNavigateToProducts: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReminders: (Boolean) -> Unit = {},
    onSelectProductCategory: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val orders by repository.orders.collectAsState()
    val products by repository.products.collectAsState()
    val posters by repository.posters.collectAsState()
    val cart by repository.cart.collectAsState()
    val manualDocs by repository.manualDocuments.collectAsState()
    val paymentReminders by repository.paymentReminders.collectAsState()
    val allRetailers by repository.retailers.collectAsState()
    val registeredCompanies by repository.companies.collectAsState()
    val chatMessages by repository.chatMessages.collectAsState()
    val notifications by repository.notifications.collectAsState()

    val myUnreadChatCount = remember(chatMessages, retailer.id) {
        repository.getUnreadChatCountForRetailer(retailer.id)
    }
    val myUnreadNotifsCount = remember(notifications, retailer.id) {
        notifications.count { !it.isRead && (it.targetRetailerId == retailer.id || it.targetRetailerId == "ALL") }
    }

    val liveRetailer = remember(allRetailers, retailer) {
        allRetailers.firstOrNull { it.id == retailer.id } ?: retailer
    }

    val retailerOrders = remember(orders, retailer.id) { orders.filter { it.retailerId == retailer.id } }
    val myDocs = remember(manualDocs, retailer.id) { manualDocs.filter { it.retailerId == retailer.id } }
    val myReminders = remember(paymentReminders, retailer.id) { paymentReminders.filter { it.retailerId == retailer.id && !it.isPaid } }
    val totalCartItems = remember(cart) { cart.values.sumOf { it.quantity } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedCompanyFilter by remember { mutableStateOf("All Companies") }

    val categories = remember(products) {
        listOf("All") + products.filter { !it.isDelisted }.map { it.category.trim() }.distinct().filter { it.isNotBlank() }
    }

    val companies = remember(products, registeredCompanies) {
        val fromProds = products.map { it.company.trim() }.filter { it.isNotBlank() }
        val fromReg = registeredCompanies.map { it.name.trim() }.filter { it.isNotBlank() }
        listOf("All Companies") + (fromProds + fromReg).distinct().sorted()
    }

    val searchTokens = remember(searchQuery) {
        searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    val displayedProducts = remember(products, searchTokens, selectedCategoryFilter, selectedCompanyFilter) {
        products.filter { p ->
            !p.isDelisted &&
            (selectedCategoryFilter == "All" || p.category.equals(selectedCategoryFilter, ignoreCase = true)) &&
            (selectedCompanyFilter == "All Companies" || p.company.equals(selectedCompanyFilter, ignoreCase = true)) &&
            (searchTokens.isEmpty() || searchTokens.all { token ->
                p.itemName.lowercase().contains(token) ||
                p.company.lowercase().contains(token) ||
                p.alias.lowercase().contains(token) ||
                p.category.lowercase().contains(token) ||
                p.itemCode.lowercase().contains(token) ||
                p.packSize.lowercase().contains(token) ||
                p.unit.lowercase().contains(token) ||
                p.batch.lowercase().contains(token) ||
                p.description.lowercase().contains(token)
            })
        }
    }

    val latestOrder = remember(retailerOrders) { retailerOrders.maxByOrNull { it.dateTime } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = GoldenSun,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = retailer.businessName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (retailer.category.lowercase()) {
                                    "platinum" -> Color(0xFFEDE9FE)
                                    "gold" -> Color(0xFFFEF3C7)
                                    else -> MintLight
                                }
                            ) {
                                Text(
                                    text = "${retailer.category.uppercase()} TIER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (retailer.category.lowercase()) {
                                        "platinum" -> Color(0xFF7C3AED)
                                        "gold" -> GoldenSun
                                        else -> ForestGreenPrimary
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = retailer.city,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Header Action Buttons: Chat, Notifications, Cart
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier.testTag("home_chat_button")
                    ) {
                        if (myUnreadChatCount > 0) {
                            BadgedBox(badge = { Badge { Text("$myUnreadChatCount") } }) {
                                Icon(Icons.Default.Chat, contentDescription = "Distributor Chat", tint = ForestGreenPrimary)
                            }
                        } else {
                            Icon(Icons.Default.Chat, contentDescription = "Distributor Chat", tint = ForestGreenPrimary)
                        }
                    }

                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("home_notif_button")
                    ) {
                        if (myUnreadNotifsCount > 0) {
                            BadgedBox(badge = { Badge { Text("$myUnreadNotifsCount") } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ForestGreenPrimary)
                            }
                        } else {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ForestGreenPrimary)
                        }
                    }

                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.testTag("home_cart_button")
                    ) {
                        if (totalCartItems > 0) {
                            BadgedBox(badge = { Badge { Text("$totalCartItems") } }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = ForestGreenPrimary)
                            }
                        } else {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = ForestGreenPrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Payment & Voice Reminder Alert Banner (shown prominently if dues or reminders exist)
        if (liveRetailer.outstandingAmount > 0 || myReminders.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onNavigateToReminders(true) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(HarvestAmber.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment Due: ${formatCurrency(liveRetailer.outstandingAmount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFB45309)
                            ) {
                                Text(
                                    text = "VOICE ALERT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🔊 Tap to play Voice Reminder & check invoice details",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Box with Intelligent Company & Multi-token Search
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Product, Company, Pack Size, Salt...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreenPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreenPrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input")
            )

            // Intelligent Company Filter Row directly below search input
            if (companies.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(companies) { comp ->
                        val isSelected = selectedCompanyFilter == comp
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCompanyFilter = if (isSelected && comp != "All Companies") "All Companies" else comp
                            },
                            label = {
                                Text(
                                    text = comp,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Real-time Search Result Feedback Banner
            if (searchQuery.isNotBlank() || selectedCompanyFilter != "All Companies") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Found ${displayedProducts.size} matching items" +
                                if (selectedCompanyFilter != "All Companies") " in $selectedCompanyFilter" else "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenPrimary
                    )
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedCompanyFilter = "All Companies"
                            selectedCategoryFilter = "All"
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset Search", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // If actively searching, show instant search results right here!
        if (searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            if (displayedProducts.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No products found for \"$searchQuery\"", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Try searching with company name (e.g. Bayer, UPL) or pack size.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Instant Search Results (${displayedProducts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenPrimary)
                    displayedProducts.take(8).forEach { product ->
                        val cartItem = cart[product.id]
                        RetailerHomeProductRow(
                            product = product,
                            cartQuantity = cartItem?.quantity ?: 0,
                            onAddToCart = { repository.addToCart(product, 1) },
                            onIncrease = { repository.addToCart(product, 1) },
                            onDecrease = {
                                val current = cartItem?.quantity ?: 0
                                if (current > 1) {
                                    repository.setItemQuantity(product.id, current - 1)
                                } else {
                                    repository.removeFromCart(product.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Promotional Banners / Posters (LARGE FORMAT: PORTER BADA KARO)
        val activePosters = remember(posters) { posters.filter { it.isActive } }
        if (activePosters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(activePosters, key = { it.id }) { poster ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .width(340.dp)
                            .height(210.dp)
                            .clickable { onNavigateToProducts() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (poster.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = poster.imageUrl,
                                    contentDescription = poster.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(ForestGreenPrimary, ForestGreenDark)
                                            )
                                        )
                                )
                            }
                            // Deep gradient overlay for crystal clear text readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.15f),
                                                Color.Black.copy(alpha = 0.50f),
                                                Color.Black.copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldenSun
                                    ) {
                                        Text(
                                            text = "SPECIAL SCHEME",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (poster.notificationTitle.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = poster.notificationTitle,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = poster.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (poster.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = poster.description,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.90f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Tap to view scheme products",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenSun
                                    )
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = GoldenSun,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Action Shortcuts Grid (Products, My Orders, Documents, Cart)
        Text(
            text = "Quick Actions",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                title = "Catalog",
                subtitle = "${products.size} Items",
                icon = Icons.Default.Inventory2,
                color = ForestGreenPrimary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProducts
            )
            QuickActionTile(
                title = "Cart",
                subtitle = if (totalCartItems > 0) "$totalCartItems items" else "Empty",
                icon = Icons.Default.ShoppingCart,
                color = HarvestAmber,
                badgeCount = totalCartItems,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCart
            )
            QuickActionTile(
                title = "My Orders",
                subtitle = "${retailerOrders.size} Placed",
                icon = Icons.Default.ReceiptLong,
                color = Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToOrders
            )
            QuickActionTile(
                title = "Documents",
                subtitle = "${myDocs.size} PDFs",
                icon = Icons.Default.Description,
                color = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDocuments
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                title = "Live Chat",
                subtitle = if (myUnreadChatCount > 0) "$myUnreadChatCount New" else "Support",
                icon = Icons.Default.Chat,
                color = Color(0xFF059669),
                badgeCount = myUnreadChatCount,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToChat
            )
            QuickActionTile(
                title = "Alerts",
                subtitle = if (myUnreadNotifsCount > 0) "$myUnreadNotifsCount Unread" else "Updates",
                icon = Icons.Default.Notifications,
                color = Color(0xFFD97706),
                badgeCount = myUnreadNotifsCount,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToNotifications
            )
            QuickActionTile(
                title = "Voice Reminder",
                subtitle = if (liveRetailer.outstandingAmount > 0) formatCurrency(liveRetailer.outstandingAmount) else "Cleared",
                icon = Icons.Default.RecordVoiceOver,
                color = HarvestAmber,
                badgeCount = if (liveRetailer.outstandingAmount > 0) 1 else 0,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToReminders(true) }
            )
            QuickActionTile(
                title = "My Profile",
                subtitle = "Ledger & PIN",
                icon = Icons.Default.Storefront,
                color = ForestGreenPrimary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProfile
            )
        }

        // Latest Order Status Alert Banner (if any)
        latestOrder?.let { order ->
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onNavigateToOrders() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latest Order #${order.id.takeLast(6)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            OrderStatusBadge(status = order.status)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${order.items.size} items • ${formatCurrency(order.grandTotal)} • ${formatDate(order.dateTime)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Products Catalog",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onNavigateToProducts) {
                Text("View Full Catalog (${products.size})", fontSize = 12.sp, color = ForestGreenPrimary)
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = {
                        selectedCategoryFilter = cat
                        onSelectProductCategory(cat)
                    },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Product Cards
        if (displayedProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products match your search or filter.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayedProducts.take(15).forEach { product ->
                    val cartItem = cart[product.id]
                    RetailerHomeProductRow(
                        product = product,
                        cartQuantity = cartItem?.quantity ?: 0,
                        onAddToCart = { repository.addToCart(product, 1) },
                        onIncrease = { repository.addToCart(product, 1) },
                        onDecrease = {
                            val current = cartItem?.quantity ?: 0
                            if (current > 1) {
                                repository.setItemQuantity(product.id, current - 1)
                            } else {
                                repository.removeFromCart(product.id)
                            }
                        }
                    )
                }

                if (displayedProducts.size > 15) {
                    Button(
                        onClick = onNavigateToProducts,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary.copy(alpha = 0.12f), contentColor = ForestGreenPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View all ${displayedProducts.size} products")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (badgeCount > 0) {
                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                        Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RetailerHomeProductRow(
    product: Product,
    cartQuantity: Int,
    onAddToCart: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image or placeholder
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MintLight),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.itemName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Agriculture,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.alias.isNotBlank()) {
                    Text(
                        text = product.alias,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${product.company} • ${product.packSize}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(product.sellingRate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.packSize.ifBlank { product.unit },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cart Add / Stepper
            if (cartQuantity == 0) {
                Button(
                    onClick = onAddToCart,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForestGreenPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = ForestGreenPrimary, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "$cartQuantity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ForestGreenPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = ForestGreenPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
