package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RetailerCategory
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRetailerCategoriesScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val categories by repository.retailerCategories.collectAsState()
    val retailers by repository.retailers.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<RetailerCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<RetailerCategory?>(null) }
    var categoryForAssignDialog by remember { mutableStateOf<RetailerCategory?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Category") },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Info Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Retailer Tier Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Create tiers (Gold, Silver, Platinum, Regular) with minimum order values and category discounts, and assign retailers to tiers.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (categories.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Category,
                    title = "No Categories Configured",
                    message = "Create categories like Gold, Silver, or Platinum to organize retailers.",
                    actionButtonText = "Create Category",
                    onActionClick = { showAddCategoryDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val assignedRetailers = retailers.filter { it.category.equals(cat.name, ignoreCase = true) }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (cat.name.lowercase()) {
                                                        "platinum" -> Color(0xFFEDE9FE)
                                                        "gold" -> Color(0xFFFEF3C7)
                                                        "silver" -> Color(0xFFF1F5F9)
                                                        else -> ForestGreenPrimary.copy(alpha = 0.12f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = when (cat.name.lowercase()) {
                                                    "platinum" -> Color(0xFF7C3AED)
                                                    "gold" -> GoldenSun
                                                    "silver" -> Color(0xFF64748B)
                                                    else -> ForestGreenPrimary
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = cat.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${assignedRetailers.size} Retailers Assigned",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { categoryToEdit = cat }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { categoryToDelete = cat }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                if (cat.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = cat.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Min Order Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (cat.minOrderValue > 0) formatCurrency(cat.minOrderValue) else "No Min Value",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Special Discount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (cat.discountPercent > 0) "${cat.discountPercent.toInt()}% Off" else "Standard Price",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (cat.discountPercent > 0) HarvestAmber else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { categoryForAssignDialog = cat },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Assign Retailers to ${cat.name}", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Category Dialog
    if (showAddCategoryDialog || categoryToEdit != null) {
        val isEditing = categoryToEdit != null
        var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
        var minOrderStr by remember { mutableStateOf(categoryToEdit?.minOrderValue?.toInt()?.toString() ?: "0") }
        var discountStr by remember { mutableStateOf(categoryToEdit?.discountPercent?.toInt()?.toString() ?: "0") }
        var description by remember { mutableStateOf(categoryToEdit?.description ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                categoryToEdit = null
            },
            title = {
                Text(if (isEditing) "Edit Retailer Category" else "Create Retailer Category", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name *") },
                        placeholder = { Text("e.g. Gold, Platinum, VIP") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minOrderStr,
                        onValueChange = { minOrderStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Minimum Order Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Tier Special Discount (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Benefits") },
                        placeholder = { Text("e.g. Premium retailers with priority dispatch") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val minOrder = minOrderStr.toDoubleOrNull() ?: 0.0
                        val discount = discountStr.toDoubleOrNull() ?: 0.0

                        if (isEditing) {
                            categoryToEdit?.let { existing ->
                                repository.updateRetailerCategory(
                                    existing.copy(
                                        name = name.trim(),
                                        minOrderValue = minOrder,
                                        discountPercent = discount,
                                        description = description.trim()
                                    )
                                )
                            }
                            Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.addRetailerCategory(
                                name = name.trim(),
                                minOrderValue = minOrder,
                                discountPercent = discount,
                                description = description.trim()
                            )
                            Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show()
                        }
                        showAddCategoryDialog = false
                        categoryToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text(if (isEditing) "Save Changes" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategoryDialog = false
                    categoryToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete '${cat.name}'? Retailers in this category will be reverted to 'Regular'.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteRetailerCategory(cat.id)
                        categoryToDelete = null
                        Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Assign Retailers Dialog
    categoryForAssignDialog?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryForAssignDialog = null },
            title = {
                Text("Assign Retailers to ${cat.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(retailers) { ret ->
                        val isAssigned = ret.category.equals(cat.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAssigned) ForestGreenPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ret.businessName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Current: ${ret.category} • ${ret.city}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        if (isAssigned) {
                                            repository.assignRetailerCategory(ret.id, "Regular")
                                        } else {
                                            repository.assignRetailerCategory(ret.id, cat.name)
                                        }
                                        Toast.makeText(context, "Updated ${ret.businessName}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = if (isAssigned) {
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                                    } else {
                                        ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (isAssigned) "Remove" else "Assign", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { categoryForAssignDialog = null }) {
                    Text("Done")
                }
            }
        )
    }
}
