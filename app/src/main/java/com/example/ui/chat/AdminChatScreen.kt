package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMessageType
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GanpatiAgroBrandLogo
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatScreen(
    repository: AgroRepository,
    initialRetailerId: String? = null
) {
    val retailers by repository.retailers.collectAsState()
    val allMessages by repository.chatMessages.collectAsState()

    var selectedRetailerId by remember(initialRetailerId) {
        mutableStateOf(initialRetailerId)
    }

    val selectedRetailer = remember(retailers, selectedRetailerId) {
        retailers.find { it.id == selectedRetailerId }
    }

    var searchQuery by remember { mutableStateOf("") }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteConversationConfirm by remember { mutableStateOf(false) }

    if (selectedRetailer == null) {
        // ==========================================
        // RETAILER CONVERSATIONS LIST
        // ==========================================
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GanpatiAgroBrandLogo(size = 32.dp, showBorder = true)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retailer Direct Chats", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by shop, retailer name or mobile...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                val filteredRetailers = remember(retailers, searchQuery, allMessages) {
                    retailers.filter { ret ->
                        searchQuery.isBlank() ||
                        ret.businessName.contains(searchQuery, ignoreCase = true) ||
                        ret.retailerName.contains(searchQuery, ignoreCase = true) ||
                        ret.mobileNumber.contains(searchQuery, ignoreCase = true)
                    }.sortedByDescending { ret ->
                        allMessages.filter { it.senderId == ret.id || it.receiverId == ret.id }
                            .maxOfOrNull { it.timestamp } ?: ret.createdAt
                    }
                }

                if (filteredRetailers.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "No Retailers Found",
                        message = "No retailer matched your search criteria.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredRetailers, key = { it.id }) { ret ->
                            val conversation = allMessages.filter {
                                (it.senderId == ret.id && it.receiverId == "ADMIN") ||
                                (it.senderId == "ADMIN" && it.receiverId == ret.id)
                            }.sortedBy { it.timestamp }

                            val lastMsg = conversation.lastOrNull()
                            val unreadCount = conversation.count { it.senderId == ret.id && !it.isRead }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRetailerId = ret.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ret.businessName.take(1).uppercase().ifBlank { "R" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = ForestGreenPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ret.businessName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (lastMsg != null) {
                                                Text(
                                                    text = formatChatTime(lastMsg.timestamp),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (lastMsg?.type) {
                                                    ChatMessageType.AUDIO -> "🎤 Voice Note"
                                                    ChatMessageType.IMAGE -> "📷 Photo"
                                                    ChatMessageType.PRODUCT_LINK -> "📦 Product Inquiry"
                                                    else -> lastMsg?.message?.ifBlank { "No messages yet" } ?: "Tap to start conversation"
                                                },
                                                fontSize = 12.sp,
                                                color = if (unreadCount > 0) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (unreadCount > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = ForestGreenPrimary,
                                                    modifier = Modifier.padding(start = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "$unreadCount",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    } else {
        // ==========================================
        // 1-TO-1 CHAT WITH SELECTED RETAILER
        // ==========================================
        val conversation = remember(allMessages, selectedRetailer.id) {
            allMessages.filter {
                (it.senderId == selectedRetailer.id && it.receiverId == "ADMIN") ||
                (it.senderId == "ADMIN" && it.receiverId == selectedRetailer.id)
            }.sortedBy { it.timestamp }
        }

        val listState = rememberLazyListState()

        LaunchedEffect(conversation.size) {
            repository.markChatAsRead(selectedRetailer.id, byAdmin = true)
            if (conversation.isNotEmpty()) {
                listState.animateScrollToItem(conversation.size - 1)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = selectedRetailer.businessName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${selectedRetailer.retailerName} • ${selectedRetailer.mobileNumber}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedRetailerId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConversationConfirm = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                ChatComposer(
                    onSendMessage = { text ->
                        repository.sendChatMessage(
                            senderId = "ADMIN",
                            senderName = "Distributor Admin",
                            receiverId = selectedRetailer.id,
                            message = text,
                            type = ChatMessageType.TEXT
                        )
                    },
                    onSendImage = { uri ->
                        repository.sendChatMessage(
                            senderId = "ADMIN",
                            senderName = "Distributor Admin",
                            receiverId = selectedRetailer.id,
                            message = "",
                            type = ChatMessageType.IMAGE,
                            mediaUri = uri.toString()
                        )
                    },
                    onSendVoiceNote = { filePath, durationMs ->
                        repository.sendChatMessage(
                            senderId = "ADMIN",
                            senderName = "Distributor Admin",
                            receiverId = selectedRetailer.id,
                            message = "",
                            type = ChatMessageType.AUDIO,
                            mediaUri = filePath,
                            audioDurationMs = durationMs
                        )
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (conversation.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Chat,
                        title = "No Messages",
                        message = "Send a text, product details, photo or voice note to ${selectedRetailer.businessName}.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(conversation, key = { it.id }) { msg ->
                            val isMy = msg.senderId == "ADMIN"
                            ChatBubble(
                                message = msg,
                                isMyMessage = isMy,
                                onDeleteMessage = { repository.deleteChatMessage(msg.id) },
                                onImageClick = { fullScreenImageUrl = it }
                            )
                        }
                    }
                }

                fullScreenImageUrl?.let { url ->
                    FullScreenImageDialog(imageUrl = url, onDismiss = { fullScreenImageUrl = null })
                }

                if (showDeleteConversationConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConversationConfirm = false },
                        title = { Text("Delete Entire Conversation?") },
                        text = { Text("Are you sure you want to permanently clear the conversation with ${selectedRetailer.businessName}? This cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConversationConfirm = false
                                    repository.deleteConversation(selectedRetailer.id)
                                }
                            ) {
                                Text("Delete Conversation", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConversationConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
