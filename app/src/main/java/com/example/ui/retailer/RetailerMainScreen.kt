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
import com.example.ui.chat.RetailerChatScreen
import com.example.ui.theme.ForestGreenPrimary

enum class RetailerTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    PRODUCTS("Products", Icons.Default.Inventory2),
    ORDERS("My Orders", Icons.Default.ReceiptLong),
    CHAT("Chat", Icons.Default.Chat),
    PROFILE("Profile", Icons.Default.Person),
    CART("Cart", Icons.Default.ShoppingCart),
    NOTIFICATIONS("Alerts", Icons.Default.Notifications),
    REMINDERS("Reminders", Icons.Default.RecordVoiceOver),
    DOCUMENTS("Documents", Icons.Default.Description),
    BILLS("Bills", Icons.Default.Receipt),
    PASSBOOK("Passbook", Icons.Default.AccountBalance)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerMainScreen(
    retailer: Retailer,
    repository: AgroRepository,
    initialTab: RetailerTab = RetailerTab.HOME,
    autoPlayVoiceReminder: Boolean = false,
    onLogout: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var autoPlayReminderVoice by remember(autoPlayVoiceReminder) { mutableStateOf(autoPlayVoiceReminder) }
    var initialCatalogCompanyFilter by remember { mutableStateOf<String?>(null) }

    val cart by repository.cart.collectAsState()
    val totalCartItems = remember(cart) { cart.values.sumOf { it.quantity } }

    val chatMessages by repository.chatMessages.collectAsState()
    val unreadChatCount = remember(chatMessages, retailer.id) {
        repository.getUnreadChatCountForRetailer(retailer.id)
    }

    val notifications by repository.notifications.collectAsState()
    val unreadNotifsCount = remember(notifications, retailer.id) {
        notifications.count { !it.isRead && (it.targetRetailerId == retailer.id || it.targetRetailerId == "ALL") }
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
                // Primary 5 navigation items
                val bottomBarTabs = listOf(
                    RetailerTab.HOME,
                    RetailerTab.PRODUCTS,
                    RetailerTab.ORDERS,
                    RetailerTab.CHAT,
                    RetailerTab.PROFILE
                )

                bottomBarTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            when (tab) {
                                RetailerTab.CHAT -> {
                                    if (unreadChatCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$unreadChatCount") } }) {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                }
                                else -> Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, fontSize = 10.sp, maxLines = 1) },
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
                        onNavigateToReminders = { autoPlay ->
                            selectedTab = RetailerTab.REMINDERS
                            autoPlayReminderVoice = autoPlay
                        },
                        onSelectProductCategory = { _ ->
                            selectedTab = RetailerTab.PRODUCTS
                        },
                        onNavigateToChat = { selectedTab = RetailerTab.CHAT },
                        onNavigateToNotifications = { selectedTab = RetailerTab.NOTIFICATIONS }
                    )
                }
                RetailerTab.PRODUCTS -> {
                    RetailerProductCatalogScreen(
                        repository = repository,
                        initialCompanyFilter = initialCatalogCompanyFilter,
                        onNavigateToCart = { selectedTab = RetailerTab.CART }
                    )
                }
                RetailerTab.ORDERS -> {
                    RetailerOrdersScreen(
                        retailer = retailer,
                        repository = repository,
                        onNavigateToCatalog = { selectedTab = RetailerTab.PRODUCTS }
                    )
                }
                RetailerTab.CHAT -> {
                    RetailerChatScreen(
                        retailer = retailer,
                        repository = repository
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
                RetailerTab.NOTIFICATIONS -> {
                    RetailerNotificationsScreen(
                        retailer = retailer,
                        repository = repository,
                        onNavigateToOrders = { selectedTab = RetailerTab.ORDERS },
                        onNavigateToBills = { selectedTab = RetailerTab.BILLS },
                        onNavigateToPassbook = { selectedTab = RetailerTab.PASSBOOK },
                        onNavigateToReminders = { selectedTab = RetailerTab.REMINDERS }
                    )
                }
                RetailerTab.REMINDERS -> {
                    RetailerRemindersScreen(
                        retailer = retailer,
                        repository = repository,
                        autoPlayFirstReminder = autoPlayReminderVoice,
                        onNavigateBack = { selectedTab = RetailerTab.HOME }
                    )
                }
                RetailerTab.DOCUMENTS -> {
                    RetailerDocumentsScreen(
                        retailer = retailer,
                        repository = repository
                    )
                }
                RetailerTab.BILLS -> {
                    RetailerBillsScreen(
                        retailer = retailer,
                        repository = repository
                    )
                }
                RetailerTab.PASSBOOK -> {
                    RetailerPassbookScreen(
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
