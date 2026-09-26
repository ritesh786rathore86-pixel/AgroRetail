package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecycleBinItem
import com.example.data.model.RecycleBinType
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRecycleBinScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val recycleBinItems by repository.recycleBinItems.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf<RecycleBinType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<String>() }

    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var itemForPermanentDelete by remember { mutableStateOf<RecycleBinItem?>(null) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(recycleBinItems, selectedTypeFilter, searchQuery) {
        recycleBinItems.filter { item ->
            (selectedTypeFilter == null || item.itemType == selectedTypeFilter) &&
            (searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.details.contains(searchQuery, ignoreCase = true)
            )
        }
    }

    val productCount = remember(recycleBinItems) { recycleBinItems.count { it.itemType == RecycleBinType.PRODUCT } }
    val retailerCount = remember(recycleBinItems) { recycleBinItems.count { it.itemType == RecycleBinType.RETAILER } }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header stats banner
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recycle Bin",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${recycleBinItems.size} total items ($productCount items, $retailerCount retailers)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (recycleBinItems.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showRestoreAllDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestGreenPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore All", fontSize = 11.sp, color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showEmptyBinDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Empty Bin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (recycleBinItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    isSelectMode = !isSelectMode
                                    if (!isSelectMode) selectedItemIds.clear()
                                }
                            ) {
                                Icon(
                                    if (isSelectMode) Icons.Default.Close else Icons.Default.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSelectMode) "Cancel Selection" else "Select Items", fontSize = 12.sp)
                            }

                            if (isSelectMode) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = {
                                            if (selectedItemIds.size == filteredItems.size) {
                                                selectedItemIds.clear()
                                            } else {
                                                selectedItemIds.clear()
                                                selectedItemIds.addAll(filteredItems.map { it.id })
                                            }
                                        }
                                    ) {
                                        Text(if (selectedItemIds.size == filteredItems.size) "Deselect All" else "Select All", fontSize = 11.sp)
                                    }

                                    if (selectedItemIds.isNotEmpty()) {
                                        FilledTonalButton(
                                            onClick = {
                                                val count = repository.restoreMultipleFromRecycleBin(selectedItemIds.toSet())
                                                Toast.makeText(context, "Restored $count item(s)!", Toast.LENGTH_SHORT).show()
                                                selectedItemIds.clear()
                                                isSelectMode = false
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Restore (${selectedItemIds.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                repository.deleteMultipleFromRecycleBin(selectedItemIds.toSet())
                                                Toast.makeText(context, "Permanently deleted ${selectedItemIds.size} item(s)", Toast.LENGTH_SHORT).show()
                                                selectedItemIds.clear()
                                                isSelectMode = false
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Delete (${selectedItemIds.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search deleted items by name, category, party...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreenPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All (${recycleBinItems.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedTypeFilter == RecycleBinType.PRODUCT,
                    onClick = { selectedTypeFilter = RecycleBinType.PRODUCT },
                    label = { Text("Items ($productCount)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedTypeFilter == RecycleBinType.RETAILER,
                    onClick = { selectedTypeFilter = RecycleBinType.RETAILER },
                    label = { Text("Retailers ($retailerCount)", fontSize = 11.sp) }
                )
            }

            // List of items
            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.DeleteSweep,
                    title = "Recycle Bin is Empty",
                    message = "Deleted items and retailers will appear here safely. You can restore them anytime or delete them permanently.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        RecycleBinCard(
                            item = item,
                            isSelectMode = isSelectMode,
                            isSelected = selectedItemIds.contains(item.id),
                            onToggleSelect = {
                                if (selectedItemIds.contains(item.id)) selectedItemIds.remove(item.id)
                                else selectedItemIds.add(item.id)
                            },
                            onRestore = {
                                val success = repository.restoreFromRecycleBin(item.id)
                                if (success) {
                                    Toast.makeText(context, "'${item.title}' restored successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Could not restore '${item.title}'", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPermanentDelete = {
                                itemForPermanentDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Empty Recycle Bin Confirmation Dialog
    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Empty Recycle Bin?") },
            text = { Text("This will permanently remove all ${recycleBinItems.size} items from the recycle bin. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.emptyRecycleBin()
                        showEmptyBinDialog = false
                        Toast.makeText(context, "Recycle bin emptied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEmptyBinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore All Confirmation Dialog
    if (showRestoreAllDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAllDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = ForestGreenPrimary) },
            title = { Text("Restore All Items?") },
            text = { Text("This will restore all ${recycleBinItems.size} items back to their active lists.") },
            confirmButton = {
                Button(
                    onClick = {
                        val count = repository.restoreAllFromRecycleBin()
                        showRestoreAllDialog = false
                        Toast.makeText(context, "Restored $count item(s) successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Restore All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single item permanent delete dialog
    itemForPermanentDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemForPermanentDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Delete Permanently?") },
            text = { Text("Are you sure you want to permanently delete '${item.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.permanentDeleteFromRecycleBin(item.id)
                        itemForPermanentDelete = null
                        Toast.makeText(context, "'${item.title}' permanently deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemForPermanentDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecycleBinCard(
    item: RecycleBinItem,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (isSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (item.itemType) {
                                RecycleBinType.PRODUCT -> ForestGreenPrimary.copy(alpha = 0.12f)
                                RecycleBinType.RETAILER -> HarvestAmber.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (item.itemType) {
                                    RecycleBinType.PRODUCT -> "ITEM / PRODUCT"
                                    RecycleBinType.RETAILER -> "RETAILER / PARTY"
                                    else -> item.itemType.name
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (item.itemType) {
                                    RecycleBinType.PRODUCT -> ForestGreenPrimary
                                    RecycleBinType.RETAILER -> Color(0xFFB45309)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Deleted: ${formatDateShort(item.deletedAt)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.details,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPermanentDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Permanently", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRestore,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
