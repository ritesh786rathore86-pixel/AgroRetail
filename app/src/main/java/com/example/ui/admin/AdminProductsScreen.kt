package com.example.ui.admin

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.Company
import com.example.data.model.Product
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroImagePresets
import com.example.util.ExcelParserHelper
import com.example.util.ProductPhotoPreset
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    repository: AgroRepository,
    onNavigateToExcelImport: () -> Unit = {},
    onNavigateToRecycleBin: () -> Unit = {}
) {
    val context = LocalContext.current
    val products by repository.products.collectAsState()
    val companies by repository.companies.collectAsState()
    val recycleBinItems by repository.recycleBinItems.collectAsState()
    val recycleBinProductCount = remember(recycleBinItems) {
        recycleBinItems.count { it.itemType == com.example.data.model.RecycleBinType.PRODUCT }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCompany by remember { mutableStateOf("All Companies") }
    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "DELISTED", "HIDDEN"

    val allCompanies = remember(companies, products) {
        val fromProds = products.map { it.company.trim() }.filter { it.isNotBlank() }
        val fromComps = companies.map { it.name.trim() }.filter { it.isNotBlank() }
        listOf("All Companies") + (fromProds + fromComps).distinct().sorted()
    }

    val searchTokens = remember(searchQuery) {
        searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    var isSelectMode by remember { mutableStateOf(false) }
    val selectedProductIds = remember { mutableStateListOf<String>() }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    var productBeingEdited by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var isAddingNewProduct by remember { mutableStateOf(false) }
    var productForStockUpdate by remember { mutableStateOf<Product?>(null) }
    var showFeaturedManagerDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchTokens, selectedCategory, selectedCompany, filterStatus) {
        products.filter { prod ->
            val statusMatches = when (filterStatus) {
                "ACTIVE" -> prod.isActive && !prod.isDelisted && prod.isVisible
                "DELISTED" -> prod.isDelisted
                "HIDDEN" -> !prod.isVisible
                else -> true
            }
            statusMatches &&
            (selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)) &&
            (selectedCompany == "All Companies" || prod.company.equals(selectedCompany, ignoreCase = true)) &&
            (searchTokens.isEmpty() || searchTokens.all { token ->
                prod.itemName.lowercase().contains(token) ||
                prod.itemCode.lowercase().contains(token) ||
                prod.company.lowercase().contains(token) ||
                prod.godown.lowercase().contains(token) ||
                prod.batch.lowercase().contains(token) ||
                prod.barcode.lowercase().contains(token) ||
                prod.alias.lowercase().contains(token) ||
                prod.packSize.lowercase().contains(token) ||
                prod.unit.lowercase().contains(token)
            })
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNewProduct = true },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("admin_add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Excel Import & Sample Download Banner
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNavigateToExcelImport,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("IMPORT FROM EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { shareProductSample(context) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DOWNLOAD SAMPLE EXCEL", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }

                // Batch Actions Toolbar & Recycle Bin Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showFeaturedManagerDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HarvestAmber),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Featured (${products.count { it.isFeatured }})", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToRecycleBin,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recycle Bin", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            if (recycleBinProductCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFDC2626)) {
                                    Text("$recycleBinProductCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                isSelectMode = !isSelectMode
                                if (!isSelectMode) selectedProductIds.clear()
                            }
                        ) {
                            Icon(if (isSelectMode) Icons.Default.Close else Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSelectMode) "Cancel Selection" else "Select Items", fontSize = 11.sp)
                        }
                    }

                    if (isSelectMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    if (selectedProductIds.size == filteredProducts.size) {
                                        selectedProductIds.clear()
                                    } else {
                                        selectedProductIds.clear()
                                        selectedProductIds.addAll(filteredProducts.map { it.id })
                                    }
                                }
                            ) {
                                Text(if (selectedProductIds.size == filteredProducts.size) "Deselect All" else "Select All", fontSize = 11.sp)
                            }

                            if (selectedProductIds.isNotEmpty()) {
                                Button(
                                    onClick = { showDeleteSelectedDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Delete (${selectedProductIds.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (products.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { showDeleteAllDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Delete All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (products.isNotEmpty()) {
                        TextButton(
                            onClick = { showDeleteAllDialog = true }
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete All", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, code, brand, godown, batch...") },
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

            // Status & Company Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "ACTIVE", "DELISTED", "HIDDEN").forEach { status ->
                    val isSel = filterStatus == status
                    FilterChip(
                        selected = isSel,
                        onClick = { filterStatus = status },
                        label = { Text(status, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (allCompanies.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allCompanies) { comp ->
                        val isSel = selectedCompany == comp
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedCompany = if (isSel && comp != "All Companies") "All Companies" else comp
                            },
                            label = { Text(comp, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Product List
            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "No Products Found",
                    message = "Add new products or import your product catalog directly from Excel or BUSY.",
                    actionButtonText = "+ Add Product",
                    onActionClick = { isAddingNewProduct = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        AdminProductCard(
                            product = product,
                            isSelectMode = isSelectMode,
                            isSelected = selectedProductIds.contains(product.id),
                            onToggleSelect = {
                                if (selectedProductIds.contains(product.id)) {
                                    selectedProductIds.remove(product.id)
                                } else {
                                    selectedProductIds.add(product.id)
                                }
                            },
                            onEdit = { productBeingEdited = product },
                            onDelete = { productToDelete = product },
                            onStockUpdate = { productForStockUpdate = product },
                            onToggleActive = { repository.toggleProductActive(product.id) },
                            onToggleDelist = { repository.toggleProductDelist(product.id) },
                            onToggleVisibility = { repository.toggleProductVisibility(product.id) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (isAddingNewProduct || productBeingEdited != null) {
        ProductFormDialog(
            initialProduct = productBeingEdited ?: Product(),
            companies = companies,
            onDismiss = {
                isAddingNewProduct = false
                productBeingEdited = null
            },
            onSave = { savedProduct ->
                repository.saveProduct(savedProduct)
                Toast.makeText(context, "Product saved successfully", Toast.LENGTH_SHORT).show()
                isAddingNewProduct = false
                productBeingEdited = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Move Product to Recycle Bin?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${productToDelete!!.itemName}\"? It will be safely moved to the Recycle Bin and can be restored anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = productToDelete!!.id
                        repository.deleteProduct(id)
                        Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Move to Bin")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Selected Confirmation Dialog
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Delete ${selectedProductIds.size} Products?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${selectedProductIds.size} selected products? They will be safely moved to the Recycle Bin and can be restored anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        val count = repository.deleteMultipleProducts(selectedProductIds.toSet(), deletedBy = "Admin")
                        Toast.makeText(context, "Moved $count product(s) to Recycle Bin", Toast.LENGTH_SHORT).show()
                        selectedProductIds.clear()
                        isSelectMode = false
                        showDeleteSelectedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Move to Recycle Bin")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete All Products Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Delete ALL Products?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all ${products.size} products? Don't worry, all items will be safely moved to the Recycle Bin and can be restored anytime with one click.") },
            confirmButton = {
                Button(
                    onClick = {
                        val count = repository.deleteAllProducts(deletedBy = "Admin")
                        Toast.makeText(context, "All $count product(s) moved to Recycle Bin", Toast.LENGTH_LONG).show()
                        selectedProductIds.clear()
                        isSelectMode = false
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Stock Adjustment Dialog
    if (productForStockUpdate != null) {
        val prod = productForStockUpdate!!
        var newStockText by remember { mutableStateOf(prod.currentStock.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { productForStockUpdate = null },
            title = { Text("Update Current Stock", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = prod.itemName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "Code: ${prod.itemCode} • Unit: ${prod.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newStockText,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) newStockText = it },
                        label = { Text("Available Quantity (${prod.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedQty = newStockText.toDoubleOrNull() ?: prod.currentStock
                        repository.updateStock(prod.id, updatedQty)
                        productForStockUpdate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Update Stock")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productForStockUpdate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFeaturedManagerDialog) {
        FeaturedProductsManagerDialog(
            repository = repository,
            onDismiss = { showFeaturedManagerDialog = false }
        )
    }
}

@Composable
fun AdminProductCard(
    product: Product,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockUpdate: () -> Unit,
    onToggleActive: () -> Unit,
    onToggleDelist: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else if (product.isActive && !product.isDelisted) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    if (isSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Product Thumbnail or Clean Placeholder
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
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
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = "No photo",
                            tint = ForestGreenPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.itemName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Badges row: Delisted, Inactive, Hidden
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (product.isDelisted) {
                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "DELISTED",
                                    color = Color(0xFF991B1B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!product.isVisible) {
                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "HIDDEN IN APP",
                                    color = Color(0xFF92400E),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!product.isActive) {
                            Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "INACTIVE",
                                    color = Color(0xFF475569),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Code: ${product.itemCode} • ${product.company} • ${product.packSize}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.godown.isNotBlank() || product.batch.isNotBlank()) {
                        Text(
                            text = listOfNotNull(
                                if (product.godown.isNotBlank()) "Godown: ${product.godown}" else null,
                                if (product.batch.isNotBlank()) "Batch: ${product.batch}" else null
                            ).joinToString(" • "),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Stock Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (product.currentStock > 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    modifier = Modifier.clickable(onClick = onStockUpdate)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stock: ${product.currentStock.toInt()} ${product.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.currentStock > 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit Stock", modifier = Modifier.size(12.dp), tint = Color(0xFF15803D))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

            // Pricing details (NO GST!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Selling: ${formatCurrency(product.sellingRate)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
                Text(
                    text = "Purchase: ${formatCurrency(product.purchaseRate)} • MRP: ${formatCurrency(product.mrp)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action row: Delist, Hide/Show, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Delist button
                    TextButton(
                        onClick = onToggleDelist,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (product.isDelisted) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (product.isDelisted) ForestGreenPrimary else Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (product.isDelisted) "Relist" else "Delist",
                            fontSize = 11.sp,
                            color = if (product.isDelisted) ForestGreenPrimary else Color(0xFFDC2626)
                        )
                    }

                    // Visibility toggle
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (product.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility in App",
                            tint = if (product.isVisible) ForestGreenPrimary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    initialProduct: Product,
    companies: List<Company>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var itemCode by remember { mutableStateOf(initialProduct.itemCode) }
    var itemName by remember { mutableStateOf(initialProduct.itemName) }
    var alias by remember { mutableStateOf(initialProduct.alias) }
    var company by remember { mutableStateOf(initialProduct.company) }
    var category by remember { mutableStateOf(initialProduct.category.ifBlank { "Fungicides" }) }
    var subCategory by remember { mutableStateOf(initialProduct.subCategory) }
    var unit by remember { mutableStateOf(initialProduct.unit.ifBlank { "PCS" }) }
    var packSize by remember { mutableStateOf(initialProduct.packSize.ifBlank { "1" }) }
    var packing by remember { mutableStateOf(initialProduct.packing) }
    var purchaseRate by remember { mutableStateOf(if (initialProduct.purchaseRate > 0) initialProduct.purchaseRate.toString() else "") }
    var sellingRate by remember { mutableStateOf(if (initialProduct.sellingRate > 0) initialProduct.sellingRate.toString() else "") }
    var mrp by remember { mutableStateOf(if (initialProduct.mrp > 0) initialProduct.mrp.toString() else "") }
    var hsnCode by remember { mutableStateOf(initialProduct.hsnCode) }
    var barcode by remember { mutableStateOf(initialProduct.barcode) }
    var openingStock by remember { mutableStateOf(initialProduct.openingStock.toString()) }
    var currentStock by remember { mutableStateOf(initialProduct.currentStock.toString()) }
    var godown by remember { mutableStateOf(initialProduct.godown) }
    var batch by remember { mutableStateOf(initialProduct.batch) }
    var description by remember { mutableStateOf(initialProduct.description) }
    var isActive by remember { mutableStateOf(initialProduct.isActive) }
    var isVisible by remember { mutableStateOf(initialProduct.isVisible) }
    var isDelisted by remember { mutableStateOf(initialProduct.isDelisted) }
    var imageUrl by remember { mutableStateOf(initialProduct.imageUrl) }
    var boxRate by remember { mutableStateOf(if (initialProduct.boxRate > 0) initialProduct.boxRate.toString() else "") }
    var pcsPerBox by remember { mutableStateOf(if (initialProduct.pcsPerBox > 1) initialProduct.pcsPerBox.toString() else "1") }
    var isFeatured by remember { mutableStateOf(initialProduct.isFeatured) }
    var showInAppImageSearchDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialProduct.id.isBlank()) "Add New Product" else "Edit Product",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Automatic Product Image + Manual In-App Control
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PRODUCT PHOTO & IMAGE SELECTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                if (imageUrl.isNotBlank()) {
                                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "IMAGE SET",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "Product preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "No photo",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text("No Image", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Automatic Image Match
                                    Button(
                                        onClick = {
                                            val matched = AgroImagePresets.findRelevantProductImage(
                                                itemName = itemName,
                                                company = company,
                                                category = category
                                            )
                                            if (matched != null) {
                                                imageUrl = matched
                                                Toast.makeText(context, "Matched product image for $itemName!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No reliable exact image match found. Please choose from in-app library or gallery.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Auto Match Image", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // 2. In-App Image Search / Library
                                    OutlinedButton(
                                        onClick = { showInAppImageSearchDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp), tint = ForestGreenPrimary)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Search In-App Images", fontSize = 11.sp, color = ForestGreenPrimary)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 3. Manual Gallery Upload
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gallery", fontSize = 11.sp)
                                        }

                                        // 4. Remove Image
                                        if (imageUrl.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { imageUrl = "" },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(0.9f)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Remove", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Product / Item Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemCode,
                            onValueChange = { itemCode = it },
                            label = { Text("Item Code / SKU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            label = { Text("Alias") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company / Brand *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category / Group") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = packSize,
                            onValueChange = { packSize = it },
                            label = { Text("Pack Size (e.g. 1 Ltr)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (e.g. PCS, LTR)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sellingRate,
                            onValueChange = { sellingRate = it },
                            label = { Text("Selling Rate / PCS Rate (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = mrp,
                            onValueChange = { mrp = it },
                            label = { Text("MRP (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Box Rate and PCS per Box pricing
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = boxRate,
                            onValueChange = { boxRate = it },
                            label = { Text("Box Rate (₹) [Optional]") },
                            placeholder = { Text("e.g. 2400") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pcsPerBox,
                            onValueChange = { pcsPerBox = it },
                            label = { Text("PCS in 1 Box") },
                            placeholder = { Text("e.g. 20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchaseRate,
                            onValueChange = { purchaseRate = it },
                            label = { Text("Purchase Rate (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hsnCode,
                            onValueChange = { hsnCode = it },
                            label = { Text("HSN Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("Current Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = openingStock,
                            onValueChange = { openingStock = it },
                            label = { Text("Opening Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = godown,
                            onValueChange = { godown = it },
                            label = { Text("Godown / Warehouse") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = batch,
                            onValueChange = { batch = it },
                            label = { Text("Batch Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode / EAN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Product Description / Technical Formulation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Control toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Active in Catalog", fontSize = 13.sp)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Visible in Retailer App", fontSize = 13.sp)
                        Switch(checked = isVisible, onCheckedChange = { isVisible = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Delist / Discontinue Product", fontSize = 13.sp)
                        Switch(checked = isDelisted, onCheckedChange = { isDelisted = it })
                    }

                    // Featured toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "⭐ Featured / Highlighted Product", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Displays this item in the Featured Products section on Retailer Home", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (itemName.isBlank()) return@Button
                            val prod = initialProduct.copy(
                                itemCode = if (itemCode.isNotBlank()) itemCode else "ITM-${(1000..9999).random()}",
                                itemName = itemName.trim(),
                                alias = alias.trim(),
                                company = company.trim(),
                                category = category.trim(),
                                subCategory = subCategory.trim(),
                                unit = unit.trim().ifBlank { "PCS" },
                                packSize = packSize.trim().ifBlank { "1" },
                                packing = packing.trim(),
                                purchaseRate = purchaseRate.toDoubleOrNull() ?: 0.0,
                                sellingRate = sellingRate.toDoubleOrNull() ?: 0.0,
                                mrp = mrp.toDoubleOrNull() ?: 0.0,
                                gstPercent = 0.0,
                                hsnCode = hsnCode.trim(),
                                barcode = barcode.trim(),
                                openingStock = openingStock.toDoubleOrNull() ?: 0.0,
                                currentStock = currentStock.toDoubleOrNull() ?: 0.0,
                                godown = godown.trim(),
                                batch = batch.trim(),
                                description = description.trim(),
                                imageUrl = imageUrl.trim(),
                                boxRate = boxRate.toDoubleOrNull() ?: 0.0,
                                pcsPerBox = pcsPerBox.toIntOrNull() ?: 1,
                                isFeatured = isFeatured,
                                isActive = isActive,
                                isVisible = isVisible,
                                isDelisted = isDelisted
                            )
                            onSave(prod)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }

        if (showInAppImageSearchDialog) {
            ProductInAppImageSearchDialog(
                initialQuery = itemName,
                onDismiss = { showInAppImageSearchDialog = false },
                onSelectImage = { selectedUrl ->
                    imageUrl = selectedUrl
                    showInAppImageSearchDialog = false
                }
            )
        }
    }
}

private fun shareProductSample(context: Context) {
    try {
        val sampleCsv = ExcelParserHelper.generateProductSampleCsv()
        val file = File(context.cacheDir, "Product_Sample_Import.csv")
        file.writeText(sampleCsv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Product Sample CSV Template")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download / Share Product Sample CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Saved product sample template to cache", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ProductInAppImageSearchDialog(
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onSelectImage: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    val results: List<ProductPhotoPreset> = remember(searchQuery) {
        AgroImagePresets.searchProductImages(searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("In-App Image Library", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreenPrimary)
                        Text("Choose an exact agricultural photo directly", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, chemical, brand, category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreenPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.id }) { preset ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectImage(preset.url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = preset.url,
                                        contentDescription = preset.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Surface(color = ForestGreenPrimary.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                                        Text(preset.category, fontSize = 10.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(preset.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onSelectImage(preset.url) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Select", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedProductsManagerDialog(
    repository: AgroRepository,
    onDismiss: () -> Unit
) {
    val products by repository.products.collectAsState()
    val featuredProducts = remember(products) {
        products.filter { it.isFeatured }.sortedBy { it.featuredOrder }
    }
    var showAddMultiPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = HarvestAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Featured Products (${featuredProducts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("Prominently displayed on Retailer Home Screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showAddMultiPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select & Add Products to Highlight", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (featuredProducts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.StarOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Featured Products Selected", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Click above to choose which products appear on Retailer Home.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(featuredProducts, key = { _, item: Product -> item.id }) { index: Int, prod: Product ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = HarvestAmber,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (prod.imageUrl.isNotBlank()) {
                                            AsyncImage(model = prod.imageUrl, contentDescription = prod.itemName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Default.Agriculture, contentDescription = null, tint = ForestGreenPrimary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text("${prod.company} • ${prod.packSize}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Move Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val reordered = featuredProducts.map { it.id }.toMutableList()
                                                val temp = reordered[index]
                                                reordered[index] = reordered[index - 1]
                                                reordered[index - 1] = temp
                                                repository.updateFeaturedOrder(reordered)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick = {
                                            if (index < featuredProducts.size - 1) {
                                                val reordered = featuredProducts.map { it.id }.toMutableList()
                                                val temp = reordered[index]
                                                reordered[index] = reordered[index + 1]
                                                reordered[index + 1] = temp
                                                repository.updateFeaturedOrder(reordered)
                                            }
                                        },
                                        enabled = index < featuredProducts.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                    }

                                    // Remove from featured
                                    IconButton(
                                        onClick = { repository.toggleProductFeatured(prod.id, false) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Done")
                }
            }
        }
    }

    if (showAddMultiPicker) {
        var pickerSearchQuery by remember { mutableStateOf("") }
        val selectedIds = remember { mutableStateListOf<String>() }
        val unfeaturedProducts = remember(products, pickerSearchQuery) {
            products.filter { !it.isFeatured && (pickerSearchQuery.isBlank() || it.itemName.contains(pickerSearchQuery, ignoreCase = true) || it.company.contains(pickerSearchQuery, ignoreCase = true)) }
        }

        AlertDialog(
            onDismissRequest = { showAddMultiPicker = false },
            title = { Text("Select Products to Highlight", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = { Text("Search products...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (unfeaturedProducts.isEmpty()) {
                        Text("No products available to add.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(unfeaturedProducts, key = { it.id }) { prod ->
                                val isChecked = prod.id in selectedIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedIds.remove(prod.id)
                                            else selectedIds.add(prod.id)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (it) selectedIds.add(prod.id)
                                            else selectedIds.remove(prod.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(prod.itemName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${prod.company} • ${prod.packSize}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            repository.setMultipleFeatured(selectedIds.toList(), true)
                        }
                        showAddMultiPicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Add Selected (${selectedIds.size})")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddMultiPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
