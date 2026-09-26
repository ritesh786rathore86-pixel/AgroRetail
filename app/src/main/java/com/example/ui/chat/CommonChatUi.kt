package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMessageType
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.util.VoiceChatHelper
import java.text.SimpleDateFormat
import java.util.*

fun formatChatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDurationSec(ms: Long): String {
    val sec = (ms / 1000).toInt()
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "Full Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isMyMessage: Boolean,
    onDeleteMessage: () -> Unit,
    onImageClick: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val playingMessageId by VoiceChatHelper.playingMessageId.collectAsState()
    val playbackProgress by VoiceChatHelper.playbackProgress.collectAsState()
    val isThisPlaying = playingMessageId == message.id

    val bubbleShape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = if (isMyMessage) ForestGreenPrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header (sender name in group/admin context)
                if (!isMyMessage && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMyMessage) GoldenSun else ForestGreenPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Media: IMAGE
                if (message.type == ChatMessageType.IMAGE && message.mediaUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(message.mediaUri) }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = message.mediaUri),
                            contentDescription = "Chat Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (message.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = message.message, color = textColor, fontSize = 14.sp)
                    }
                }

                // Media: AUDIO VOICE NOTE
                else if (message.type == ChatMessageType.AUDIO && message.mediaUri.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                if (isThisPlaying) {
                                    VoiceChatHelper.stopPlaying()
                                } else {
                                    VoiceChatHelper.playAudio(message.id, message.mediaUri)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isMyMessage) Color.White.copy(alpha = 0.25f) else ForestGreenPrimary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isThisPlaying) "Pause" else "Play",
                                tint = if (isMyMessage) Color.White else ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { if (isThisPlaying) playbackProgress else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isMyMessage) GoldenSun else ForestGreenPrimary,
                                trackColor = if (isMyMessage) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Voice Note",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formatDurationSec(message.audioDurationMs),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // PRODUCT LINK
                else if (message.type == ChatMessageType.PRODUCT_LINK) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMyMessage) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (isMyMessage) GoldenSun else ForestGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.linkedProductName.ifBlank { "Product Inquiry" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = textColor
                                )
                                if (message.linkedProductRate > 0) {
                                    Text(
                                        text = "Rate: ${formatCurrency(message.linkedProductRate)}",
                                        fontSize = 11.sp,
                                        color = if (isMyMessage) Color.White.copy(alpha = 0.9f) else ForestGreenPrimary
                                    )
                                }
                            }
                        }
                    }
                    if (message.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = message.message, color = textColor, fontSize = 14.sp)
                    }
                }

                // Plain Text
                else {
                    Text(
                        text = message.message,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp, Read tick & Delete icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatChatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.65f)
                    )

                    if (isMyMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = if (message.isRead) GoldenSun else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Message",
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Message?") },
            text = { Text("Are you sure you want to delete this message? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteMessage()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatComposer(
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit,
    onSendVoiceNote: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    val isRecording by VoiceChatHelper.isRecording.collectAsState()
    val recordingDurationSec by VoiceChatHelper.recordingDurationSec.collectAsState()

    // Permission launcher for Mic
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            VoiceChatHelper.startRecording(context)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendImage(uri)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                // Recording Controls Mode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording 0:${String.format("%02d", recordingDurationSec)}",
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cancel Recording
                IconButton(
                    onClick = { VoiceChatHelper.cancelRecording() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel Recording", tint = MaterialTheme.colorScheme.error)
                }

                // Send Recording
                IconButton(
                    onClick = {
                        val res = VoiceChatHelper.stopRecording()
                        if (res != null) {
                            onSendVoiceNote(res.file.absolutePath, res.durationMs)
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ForestGreenPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Send Voice Note", tint = Color.White)
                }

            } else {
                // Normal Text / Image / Mic Mode
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Photo",
                        tint = ForestGreenPrimary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type message...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreenPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = {
                            recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Record Voice Note", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
