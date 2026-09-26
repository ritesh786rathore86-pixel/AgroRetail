package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminUser
import com.example.data.model.OrderStatus
import com.example.data.repository.AgroRepository
import com.example.ui.chat.AdminChatScreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import kotlinx.coroutines.launch

enum class AdminNavigationSection(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    ORDERS("Orders & PDFs", Icons.Default.LocalShipping),
    CHAT("Retailer Chat", Icons.Default.Chat),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    REMINDERS("Payment Reminders", Icons.Default.RecordVoiceOver),
    POSTERS("Posters & Schemes", Icons.Default.Campaign),
    RETAILERS("Retailers", Icons.Default.Storefront),
    PRODUCTS("Products", Icons.Default.Inventory2),
    COMPANIES("Companies", Icons.Default.Business),
    DOCUMENTS("Documents", Icons.Default.Description),
    CATEGORIES("Retailer Categories", Icons.Default.WorkspacePremium),
    EXCEL_IMPORT("Excel Import", Icons.Default.UploadFile),
    RECYCLE_BIN("Recycle Bin", Icons.Default.DeleteSweep),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    admin: AdminUser,
    repository: AgroRepository,
    onLogout: () -> Unit
) {
    val orders by repository.orders.collectAsState()
    val pendingOrdersCount = remember(orders) { orders.count { it.status == OrderStatus.Pending } }
    val manualDocs by repository.manualDocuments.collectAsState()
    val paymentReminders by repository.paymentReminders.collectAsState()
    val posters by repository.posters.collectAsState()
    val recycleBinItems by repository.recycleBinItems.collectAsState()
    val chatMessages by repository.chatMessages.collectAsState()
    val notifications by repository.notifications.collectAsState()

    val activePostersCount = remember(posters) { posters.count { it.isActive } }
    val dueRemindersCount = remember(paymentReminders) { paymentReminders.count { !it.isPaid } }
    val unreadChatCount = remember(chatMessages) { repository.getUnreadChatCountForAdmin() }
    val unreadNotifCount = remember(notifications) {
        notifications.count { !it.isRead && (it.targetRetailerId == "ADMIN" || it.targetRetailerId == "ALL") }
    }

    var currentSection by remember { mutableStateOf(AdminNavigationSection.DASHBOARD) }
    var selectedChatRetailerId by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Admin Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ForestGreenPrimary)
                        .padding(20.dp)
                ) {
                    Column {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldenSun, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = admin.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text(text = "Administrator • ${admin.email}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable {
                                currentSection = AdminNavigationSection.SETTINGS
                                coroutineScope.launch { drawerState.close() }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = GoldenSun, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Firm Profile", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val drawerSections = AdminNavigationSection.values()

                drawerSections.forEach { section ->
                    val isSelected = currentSection == section
                    NavigationDrawerItem(
                        icon = {
                            when (section) {
                                AdminNavigationSection.ORDERS -> {
                                    if (pendingOrdersCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$pendingOrdersCount") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                AdminNavigationSection.CHAT -> {
                                    if (unreadChatCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$unreadChatCount") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                AdminNavigationSection.NOTIFICATIONS -> {
                                    if (unreadNotifCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$unreadNotifCount") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                AdminNavigationSection.REMINDERS -> {
                                    if (dueRemindersCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$dueRemindersCount") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                AdminNavigationSection.POSTERS -> {
                                    if (activePostersCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$activePostersCount") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                AdminNavigationSection.DOCUMENTS -> {
                                    if (manualDocs.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${manualDocs.size}") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                AdminNavigationSection.RECYCLE_BIN -> {
                                    if (recycleBinItems.isNotEmpty()) {
                                        BadgedBox(badge = { Badge(containerColor = Color(0xFFDC2626)) { Text("${recycleBinItems.size}") } }) {
                                            Icon(section.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(section.icon, contentDescription = null)
                                    }
                                }
                                else -> Icon(section.icon, contentDescription = null)
                            }
                        },
                        label = { Text(section.title) },
                        selected = isSelected,
                        onClick = {
                            if (section == AdminNavigationSection.CHAT) {
                                selectedChatRetailerId = null
                            }
                            currentSection = section
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ForestGreenPrimary.copy(alpha = 0.12f),
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentSection.title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = ForestGreenPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = ForestGreenPrimary
                    ),
                    actions = {
                        IconButton(onClick = {
                            selectedChatRetailerId = null
                            currentSection = AdminNavigationSection.CHAT
                        }) {
                            if (unreadChatCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadChatCount") } }) {
                                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = ForestGreenPrimary)
                                }
                            } else {
                                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = ForestGreenPrimary)
                            }
                        }
                        IconButton(onClick = { currentSection = AdminNavigationSection.NOTIFICATIONS }) {
                            if (unreadNotifCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadNotifCount") } }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ForestGreenPrimary)
                                }
                            } else {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ForestGreenPrimary)
                            }
                        }
                        IconButton(onClick = { currentSection = AdminNavigationSection.REMINDERS }) {
                            if (dueRemindersCount > 0) {
                                BadgedBox(badge = { Badge { Text("$dueRemindersCount") } }) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice Reminders", tint = ForestGreenPrimary)
                                }
                            } else {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice Reminders", tint = ForestGreenPrimary)
                            }
                        }
                        IconButton(onClick = { currentSection = AdminNavigationSection.SETTINGS }) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = "Admin Profile", tint = ForestGreenPrimary)
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = ForestGreenPrimary
                ) {
                    val primaryTabs = listOf(
                        AdminNavigationSection.DASHBOARD,
                        AdminNavigationSection.ORDERS,
                        AdminNavigationSection.CHAT,
                        AdminNavigationSection.RETAILERS,
                        AdminNavigationSection.PRODUCTS
                    )

                    primaryTabs.forEach { tab ->
                        val isSelected = currentSection == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (tab == AdminNavigationSection.CHAT) {
                                    selectedChatRetailerId = null
                                }
                                currentSection = tab
                            },
                            icon = {
                                when (tab) {
                                    AdminNavigationSection.ORDERS -> {
                                        if (pendingOrdersCount > 0) {
                                            BadgedBox(badge = { Badge { Text("$pendingOrdersCount") } }) {
                                                Icon(tab.icon, contentDescription = null)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = null)
                                        }
                                    }
                                    AdminNavigationSection.CHAT -> {
                                        if (unreadChatCount > 0) {
                                            BadgedBox(badge = { Badge { Text("$unreadChatCount") } }) {
                                                Icon(tab.icon, contentDescription = null)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = null)
                                        }
                                    }
                                    else -> Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.title, fontSize = 9.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ForestGreenPrimary,
                                selectedTextColor = ForestGreenPrimary,
                                indicatorColor = ForestGreenPrimary.copy(alpha = 0.12f)
                            )
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
                when (currentSection) {
                    AdminNavigationSection.DASHBOARD -> AdminDashboardScreen(
                        repository = repository,
                        onNavigateToProducts = { currentSection = AdminNavigationSection.PRODUCTS },
                        onNavigateToOrders = { currentSection = AdminNavigationSection.ORDERS },
                        onNavigateToRetailers = { currentSection = AdminNavigationSection.RETAILERS },
                        onNavigateToDocuments = { currentSection = AdminNavigationSection.DOCUMENTS },
                        onNavigateToCategories = { currentSection = AdminNavigationSection.CATEGORIES },
                        onNavigateToCompanies = { currentSection = AdminNavigationSection.COMPANIES },
                        onNavigateToExcelImport = { currentSection = AdminNavigationSection.EXCEL_IMPORT },
                        onNavigateToPosters = { currentSection = AdminNavigationSection.POSTERS },
                        onNavigateToReminders = { currentSection = AdminNavigationSection.REMINDERS },
                        onNavigateToNotifications = { currentSection = AdminNavigationSection.NOTIFICATIONS },
                        onNavigateToChat = {
                            selectedChatRetailerId = null
                            currentSection = AdminNavigationSection.CHAT
                        },
                        onNavigateToSettings = { currentSection = AdminNavigationSection.SETTINGS }
                    )
                    AdminNavigationSection.ORDERS -> AdminOrdersScreen(repository = repository)
                    AdminNavigationSection.CHAT -> AdminChatScreen(
                        repository = repository,
                        initialRetailerId = selectedChatRetailerId
                    )
                    AdminNavigationSection.NOTIFICATIONS -> AdminNotificationsScreen(repository = repository)
                    AdminNavigationSection.REMINDERS -> AdminPaymentRemindersScreen(repository = repository)
                    AdminNavigationSection.POSTERS -> AdminPostersScreen(repository = repository)
                    AdminNavigationSection.RETAILERS -> AdminRetailersScreen(
                        repository = repository,
                        onNavigateToExcelImport = { currentSection = AdminNavigationSection.EXCEL_IMPORT },
                        onNavigateToRecycleBin = { currentSection = AdminNavigationSection.RECYCLE_BIN },
                        onOpenChatWithRetailer = { retailerId ->
                            selectedChatRetailerId = retailerId
                            currentSection = AdminNavigationSection.CHAT
                        }
                    )
                    AdminNavigationSection.PRODUCTS -> AdminProductsScreen(
                        repository = repository,
                        onNavigateToExcelImport = { currentSection = AdminNavigationSection.EXCEL_IMPORT },
                        onNavigateToRecycleBin = { currentSection = AdminNavigationSection.RECYCLE_BIN }
                    )
                    AdminNavigationSection.COMPANIES -> AdminCompaniesScreen(repository = repository)
                    AdminNavigationSection.DOCUMENTS -> AdminDocumentsScreen(repository = repository)
                    AdminNavigationSection.CATEGORIES -> AdminRetailerCategoriesScreen(repository = repository)
                    AdminNavigationSection.EXCEL_IMPORT -> AdminExcelImportScreen(
                        repository = repository,
                        onNavigateToHistory = { /* Stay on import */ }
                    )
                    AdminNavigationSection.RECYCLE_BIN -> AdminRecycleBinScreen(repository = repository)
                    AdminNavigationSection.SETTINGS -> AdminSettingsScreen(
                        repository = repository,
                        onNavigateToRecycleBin = { currentSection = AdminNavigationSection.RECYCLE_BIN },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
