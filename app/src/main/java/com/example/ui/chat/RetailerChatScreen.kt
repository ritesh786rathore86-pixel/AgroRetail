package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.ChatMessageType
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GanpatiAgroBrandLogo
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerChatScreen(
    retailer: Retailer,
    repository: AgroRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val allMessages by repository.chatMessages.collectAsState()
    val distributorProfile by repository.distributorProfile.collectAsState()

    // Filter messages for this retailer
    val conversation = remember(allMessages, retailer.id) {
        allMessages.filter {
            (it.senderId == retailer.id && it.receiverId == "ADMIN") ||
            (it.senderId == "ADMIN" && it.receiverId == retailer.id)
        }.sortedBy { it.timestamp }
    }

    val listState = rememberLazyListState()
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Mark as read when viewing
    LaunchedEffect(conversation.size) {
        repository.markChatAsRead(retailer.id, byAdmin = false)
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GanpatiAgroBrandLogo(size = 36.dp, showBorder = true)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = distributorProfile.companyName.ifBlank { "Distributor Admin" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Support Online • Fast Reply",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat History", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatComposer(
                onSendMessage = { text ->
                    repository.sendChatMessage(
                        senderId = retailer.id,
                        senderName = retailer.businessName.ifBlank { retailer.retailerName },
                        receiverId = "ADMIN",
                        message = text,
                        type = ChatMessageType.TEXT
                    )
                },
                onSendImage = { uri ->
                    repository.sendChatMessage(
                        senderId = retailer.id,
                        senderName = retailer.businessName.ifBlank { retailer.retailerName },
                        receiverId = "ADMIN",
                        message = "",
                        type = ChatMessageType.IMAGE,
                        mediaUri = uri.toString()
                    )
                },
                onSendVoiceNote = { filePath, durationMs ->
                    repository.sendChatMessage(
                        senderId = retailer.id,
                        senderName = retailer.businessName.ifBlank { retailer.retailerName },
                        receiverId = "ADMIN",
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
                    title = "No Messages Yet",
                    message = "Start a direct conversation with your distributor. Ask about rates, schemes, or send product photos & voice notes.",
                    actionButtonText = "Say Namaste 🙏",
                    onActionClick = {
                        repository.sendChatMessage(
                            senderId = retailer.id,
                            senderName = retailer.businessName.ifBlank { retailer.retailerName },
                            receiverId = "ADMIN",
                            message = "Namaste Sir, I need information regarding current crop protection rates and offers.",
                            type = ChatMessageType.TEXT
                        )
                    },
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
                        val isMy = msg.senderId == retailer.id
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

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Clear Chat History?") },
                    text = { Text("Are you sure you want to clear this entire chat conversation? This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showClearConfirm = false
                                repository.clearChatHistory(retailer.id)
                            }
                        ) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
