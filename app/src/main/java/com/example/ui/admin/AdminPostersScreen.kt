package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Poster
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroImagePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostersScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val posters by repository.posters.collectAsState()

    var isAddingPoster by remember { mutableStateOf(false) }
    var posterBeingEdited by remember { mutableStateOf<Poster?>(null) }
    var posterToDelete by remember { mutableStateOf<Poster?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "PAUSED"
    var replacingPhotoPosterId by remember { mutableStateOf<String?>(null) }

    val replacePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetId = replacingPhotoPosterId
        if (uri != null && targetId != null) {
            repository.updatePosterImage(targetId, uri.toString())
            Toast.makeText(context, "Poster photo updated successfully!", Toast.LENGTH_SHORT).show()
        }
        replacingPhotoPosterId = null
    }

    val displayedPosters = remember(posters, statusFilter) {
        val filtered = when (statusFilter) {
            "ACTIVE" -> posters.filter { it.isActive }
            "PAUSED" -> posters.filter { !it.isActive }
            else -> posters
        }
        filtered.sortedBy { it.priority }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingPoster = true },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                text = { Text("New Poster", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Controls & Filter Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Promotional Banners (${posters.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = "Admin Poster & Carousel Broadcast Control",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val activeCount = posters.count { it.isActive }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (activeCount > 0) ForestGreenPrimary.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$activeCount Active in App",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCount > 0) ForestGreenPrimary else Color.Gray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter chips: All, Active, Paused
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = statusFilter == "ALL",
                            onClick = { statusFilter = "ALL" },
                            label = { Text("All (${posters.size})") }
                        )
                        FilterChip(
                            selected = statusFilter == "ACTIVE",
                            onClick = { statusFilter = "ACTIVE" },
                            label = { Text("Active (${posters.count { it.isActive }})") }
                        )
                        FilterChip(
                            selected = statusFilter == "PAUSED",
                            onClick = { statusFilter = "PAUSED" },
                            label = { Text("Paused (${posters.count { !it.isActive }})") }
                        )
                    }
                }
            }

            if (displayedPosters.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Campaign,
                    title = if (statusFilter == "ALL") "No Promotional Posters" else "No $statusFilter Posters",
                    message = "Publish company schemes, discounts, and product launches with banners to retailers' home screens.",
                    actionButtonText = "+ Create New Poster",
                    onActionClick = { isAddingPoster = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayedPosters, key = { it.id }) { poster ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Poster Banner Image Preview with Controls Overlay
                                val resolvedImage = poster.imageUrl.ifBlank {
                                    AgroImagePresets.getAutomaticPosterPhoto(poster.title)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(138.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = resolvedImage,
                                        contentDescription = poster.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Top Left: Priority Badge
                                    Surface(
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "Priority #${poster.priority}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HarvestAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Top Right: Priority Shift & Quick Replace Photo
                                    Surface(
                                        shape = RoundedCornerShape(bottomStart = 8.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    repository.updatePosterPriority(poster.id, -1)
                                                    Toast.makeText(context, "Moved to higher priority", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Priority Up", tint = Color.White, modifier = Modifier.size(15.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    repository.updatePosterPriority(poster.id, 1)
                                                    Toast.makeText(context, "Moved to lower priority", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Priority Down", tint = Color.White, modifier = Modifier.size(15.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    replacingPhotoPosterId = poster.id
                                                    replacePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.PhotoCamera, contentDescription = "Change Image", tint = HarvestAmber, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = poster.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (poster.isActive) ForestGreenPrimary else Color.Gray)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = if (poster.isActive) "ACTIVE (Visible on Retailer Home)" else "PAUSED (Hidden from Retailers)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (poster.isActive) ForestGreenPrimary else Color.Gray
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = poster.isActive,
                                            onCheckedChange = { repository.togglePosterActive(poster.id) },
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = poster.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Valid: ${formatDateShort(poster.startDate)} - ${formatDateShort(poster.endDate)}",
                                        fontSize = 11.sp,
                                        color = ForestGreenPrimary
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                repository.broadcastPushNotification(
                                                    title = "New Scheme: ${poster.title}",
                                                    message = poster.description,
                                                    linkedType = "POSTER",
                                                    linkedId = poster.id
                                                )
                                                Toast.makeText(context, "Push notification sent to all retailers!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Push Alert", fontSize = 12.sp)
                                        }

                                        Row {
                                            IconButton(onClick = { posterBeingEdited = poster }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ForestGreenPrimary)
                                            }
                                            IconButton(onClick = {
                                                posterToDelete = poster
                                            }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
    }

    // Delete Confirmation Dialog
    posterToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { posterToDelete = null },
            title = { Text("Delete Promotional Poster?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove '${target.title}'? This will immediately remove it from all retailer banners.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deletePoster(target.id)
                        Toast.makeText(context, "Poster '${target.title}' deleted", Toast.LENGTH_SHORT).show()
                        posterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { posterToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isAddingPoster || posterBeingEdited != null) {
        val initial = posterBeingEdited ?: Poster()
        var title by remember { mutableStateOf(initial.title) }
        var description by remember { mutableStateOf(initial.description) }
        var imageUrl by remember { mutableStateOf(initial.imageUrl) }
        var priority by remember { mutableStateOf(initial.priority.toString()) }
        var sendPushOnSave by remember { mutableStateOf(true) }
        var linkedProductId by remember { mutableStateOf(initial.linkedProductId) }
        var linkedProductName by remember { mutableStateOf(initial.linkedProductName) }
        var showProductPicker by remember { mutableStateOf(false) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                imageUrl = uri.toString()
                Toast.makeText(context, "Photo selected from device gallery!", Toast.LENGTH_SHORT).show()
            }
        }

        val effectivePreviewImage = remember(imageUrl, title) {
            if (imageUrl.isNotBlank()) imageUrl else AgroImagePresets.getAutomaticPosterPhoto(title)
        }

        Dialog(onDismissRequest = {
            isAddingPoster = false
            posterBeingEdited = null
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (initial.id.isBlank()) "New Promotional Poster" else "Edit Poster",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ForestGreenPrimary
                        )
                        IconButton(onClick = {
                            isAddingPoster = false
                            posterBeingEdited = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Poster Banner Photo Upload & Live Preview
                        Text("Poster Photo / Banner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenPrimary)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = effectivePreviewImage,
                                contentDescription = "Poster Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(bottomStart = 8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = if (imageUrl.isNotBlank()) "Custom Photo" else "Auto Agro Banner",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Photo", fontSize = 12.sp)
                            }

                            if (imageUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { imageUrl = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto Preset", fontSize = 12.sp)
                                }
                            }
                        }

                        // Presets Quick Selector
                        Text("Or choose Agriculture Template Banner:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AgroImagePresets.POSTER_PRESETS) { preset ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (imageUrl == preset.url) 2.dp else 1.dp,
                                            color = if (imageUrl == preset.url) ForestGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { imageUrl = preset.url }
                                ) {
                                    AsyncImage(
                                        model = preset.url,
                                        contentDescription = preset.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Photo / Image URL (or upload above)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://... or content://") }
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Scheme / Poster Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Scheme Offer Details *") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        OutlinedTextField(
                            value = priority,
                            onValueChange = { if (it.all { c -> c.isDigit() }) priority = it },
                            label = { Text("Display Priority (1 = First, 2 = Second...)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Linked Product Section
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "LINK PRODUCT (ON POSTER CLICK)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (linkedProductId.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = linkedProductName.ifBlank { "Linked Product" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(text = "ID: $linkedProductId", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            TextButton(onClick = { showProductPicker = true }) {
                                                Text("Change", fontSize = 11.sp)
                                            }
                                            IconButton(onClick = {
                                                linkedProductId = ""
                                                linkedProductName = ""
                                            }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove Link", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No product linked (Purely promotional)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(
                                            onClick = { showProductPicker = true },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Link Product", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = sendPushOnSave, onCheckedChange = { sendPushOnSave = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Send Instant Push Notification to Retailers", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isAddingPoster = false
                                posterBeingEdited = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (title.isBlank()) return@Button
                                val updated = initial.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    imageUrl = imageUrl.trim(),
                                    priority = priority.toIntOrNull() ?: 1,
                                    linkedProductId = linkedProductId,
                                    linkedProductName = linkedProductName,
                                    isActive = true
                                )
                                repository.savePoster(
                                    poster = updated,
                                    sendPushNotification = sendPushOnSave,
                                    targetRetailerOption = "ALL"
                                )
                                Toast.makeText(context, "Poster published successfully!", Toast.LENGTH_SHORT).show()
                                isAddingPoster = false
                                posterBeingEdited = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Text("Publish")
                        }
                    }
                }
            }

            if (showProductPicker) {
                val products by repository.products.collectAsState()
                var productSearchQuery by remember { mutableStateOf("") }
                val filteredProducts = remember(products, productSearchQuery) {
                    products.filter {
                        productSearchQuery.isBlank() ||
                        it.itemName.contains(productSearchQuery, ignoreCase = true) ||
                        it.company.contains(productSearchQuery, ignoreCase = true)
                    }
                }

                AlertDialog(
                    onDismissRequest = { showProductPicker = false },
                    title = { Text("Select Product to Link", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                            OutlinedTextField(
                                value = productSearchQuery,
                                onValueChange = { productSearchQuery = it },
                                placeholder = { Text("Search products...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(filteredProducts, key = { it.id }) { p ->
                                    Surface(
                                        color = if (linkedProductId == p.id) ForestGreenPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                linkedProductId = p.id
                                                linkedProductName = p.itemName
                                                showProductPicker = false
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Agriculture, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(p.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${p.company} • ${p.packSize}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (linkedProductId == p.id) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = ForestGreenPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showProductPicker = false }) {
                            Text("Done")
                        }
                    }
                )
            }
        }
    }
}
