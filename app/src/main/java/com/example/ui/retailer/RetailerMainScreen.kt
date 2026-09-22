package com.example.ui.retailer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.theme.ForestGreenPrimary

enum class RetailerTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    PRODUCTS("Products", Icons.Default.Inventory2),
    CART("Cart", Icons.Default.ShoppingCart),
    ORDERS("My Orders", Icons.Default.ReceiptLong),
    REMINDERS("Reminders", Icons.Default.RecordVoiceOver),
    DOCUMENTS("Documents", Icons.Default.Description),
    PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerMainScreen(
    retailer: Retailer,
    repository: AgroRepository,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(RetailerTab.HOME) }
    var initialCatalogCompanyFilter by remember { mutableStateOf<String?>(null) }

    val cart by repository.cart.collectAsState()
    val totalCartItems = remember(cart) { cart.values.sumOf { it.quantity } }

    val manualDocs by repository.manualDocuments.collectAsState()
    val myDocsCount = remember(manualDocs, retailer.id) {
        manualDocs.count { it.retailerId == retailer.id }
    }

    val paymentReminders by repository.paymentReminders.collectAsState()
    val myRemindersCount = remember(paymentReminders, retailer.id) {
        paymentReminders.count { it.retailerId == retailer.id && !it.isPaid }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreenPrimary
            ) {
                RetailerTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == RetailerTab.CART && totalCartItems > 0) {
                                BadgedBox(badge = { Badge { Text("$totalCartItems") } }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else if (tab == RetailerTab.REMINDERS && myRemindersCount > 0) {
                                BadgedBox(badge = { Badge { Text("$myRemindersCount") } }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = ForestGreenPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                RetailerTab.HOME -> {
                    RetailerHomeScreen(
                        retailer = retailer,
                        repository = repository,
                        onNavigateToProducts = {
                            initialCatalogCompanyFilter = null
                            selectedTab = RetailerTab.PRODUCTS
                        },
                        onNavigateToCart = { selectedTab = RetailerTab.CART },
                        onNavigateToOrders = { selectedTab = RetailerTab.ORDERS },
                        onNavigateToDocuments = { selectedTab = RetailerTab.DOCUMENTS },
                        onNavigateToProfile = { selectedTab = RetailerTab.PROFILE },
                        onNavigateToReminders = { selectedTab = RetailerTab.REMINDERS },
                        onSelectProductCategory = { _ ->
                            selectedTab = RetailerTab.PRODUCTS
                        }
                    )
                }
                RetailerTab.PRODUCTS -> {
                    RetailerProductCatalogScreen(
                        repository = repository,
                        initialCompanyFilter = initialCatalogCompanyFilter,
                        onNavigateToCart = { selectedTab = RetailerTab.CART }
                    )
                }
                RetailerTab.CART -> {
                    CartScreen(
                        retailer = retailer,
                        repository = repository,
                        onNavigateBack = { selectedTab = RetailerTab.PRODUCTS },
                        onOrderPlaced = { _ ->
                            selectedTab = RetailerTab.ORDERS
                        }
                    )
                }
                RetailerTab.ORDERS -> {
                    RetailerOrdersScreen(
                        retailer = retailer,
                        repository = repository,
                        onNavigateToCatalog = { selectedTab = RetailerTab.PRODUCTS }
                    )
                }
                RetailerTab.REMINDERS -> {
                    RetailerRemindersScreen(
                        retailer = retailer,
                        repository = repository,
                        onNavigateBack = { selectedTab = RetailerTab.HOME }
                    )
                }
                RetailerTab.DOCUMENTS -> {
                    RetailerDocumentsScreen(
                        retailer = retailer,
                        repository = repository
                    )
                }
                RetailerTab.PROFILE -> {
                    RetailerProfileScreen(
                        retailer = retailer,
                        repository = repository,
                        onLogout = onLogout,
                        onNavigateToReminders = { selectedTab = RetailerTab.REMINDERS }
                    )
                }
            }
        }
    }
}
