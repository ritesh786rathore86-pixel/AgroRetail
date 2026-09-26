package com.example.ui.retailer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.Product
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.MintLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerProductCatalogScreen(
    repository: AgroRepository,
    initialCompanyFilter: String? = null,
    onNavigateToCart: () -> Unit
) {
    val products by repository.products.collectAsState()
    val companies by repository.companies.collectAsState()
    val cart by repository.cart.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var visualSearchUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCompany by remember { mutableStateOf(initialCompanyFilter ?: "All") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            visualSearchUri = uri
            val imageResults = repository.searchProductsByImage(uri.toString())
            if (imageResults.isNotEmpty()) {
                val matchedComp = imageResults.first().company
                if (matchedComp.isNotBlank()) {
                    selectedCompany = matchedComp
                }
            }
        }
    }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    val categories = remember(products) {
        listOf("All") + products.filter { it.isActive && it.isVisible && !it.isDelisted }.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val companyNames = remember(companies, products) {
        val fromProds = products.map { it.company.trim() }.filter { it.isNotBlank() }
        val fromComps = companies.map { it.name.trim() }.filter { it.isNotBlank() }
        listOf("All") + (fromProds + fromComps).distinct().sorted()
    }

    val searchTokens = remember(searchQuery) {
        searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    // Filter active, visible, non-delisted products only
    val filteredProducts = remember(products, searchTokens, selectedCategory, selectedCompany) {
        products.filter { prod ->
            !prod.isDelisted &&
            (selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)) &&
            (selectedCompany == "All" || prod.company.equals(selectedCompany, ignoreCase = true)) &&
            (searchTokens.isEmpty() || searchTokens.all { token ->
                prod.itemName.lowercase().contains(token) ||
                prod.itemCode.lowercase().contains(token) ||
                prod.alias.lowercase().contains(token) ||
                prod.barcode.lowercase().contains(token) ||
                prod.company.lowercase().contains(token) ||
                prod.category.lowercase().contains(token) ||
                prod.packSize.lowercase().contains(token) ||
                prod.unit.lowercase().contains(token) ||
                prod.description.lowercase().contains(token)
            })
        }
    }

    val totalCartItems = remember(cart) { cart.values.sumOf { it.quantity } }
    val cartSubTotal = remember(cart) { cart.values.sumOf { it.grandTotal } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Catalog", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                ),
                actions = {
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.testTag("catalog_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (totalCartItems > 0) {
                                    Badge(containerColor = HarvestAmber) {
                                        Text("$totalCartItems")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "View Cart", tint = ForestGreenPrimary)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (totalCartItems > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$totalCartItems items in Cart",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(cartSubTotal),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }

                        Button(
                            onClick = onNavigateToCart,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.testTag("view_cart_floating_button")
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Cart", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar with Visual Camera Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, code, alias, barcode, brand...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreenPrimary)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank() || visualSearchUri != null) {
                            IconButton(onClick = {
                                searchQuery = ""
                                visualSearchUri = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                        IconButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Visual product search",
                                tint = if (visualSearchUri != null) GoldenSun else ForestGreenPrimary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("catalog_search_input")
            )

            // Visual search banner indicator
            if (visualSearchUri != null) {
                Surface(
                    color = MintLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Visual photo search applied", fontSize = 12.sp, color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { visualSearchUri = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear Photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Company filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(companyNames) { comp ->
                    val isSelected = selectedCompany == comp
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCompany = comp },
                        label = { Text(comp, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HarvestAmber,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "No Products Found",
                    message = "No active products match your search or filter criteria.",
                    actionButtonText = "Clear Filters",
                    onActionClick = {
                        searchQuery = ""
                        selectedCategory = "All"
                        selectedCompany = "All"
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        RetailerProductCard(
                            product = product,
                            currentCartQty = cart[product.id]?.quantity ?: 0,
                            onClickDetail = { selectedProductForDetail = product },
                            onAddToCart = { qty -> repository.addToCart(product, qty) },
                            onUpdateCartQty = { newQty -> repository.setItemQuantity(product.id, newQty) }
                        )
                    }
                }
            }
        }
    }

    if (selectedProductForDetail != null) {
        val prod = selectedProductForDetail!!
        RetailerProductDetailDialog(
            product = prod,
            currentCartQty = cart[prod.id]?.quantity ?: 0,
            onAddToCart = { qty -> repository.addToCart(prod, qty) },
            onUpdateCartQty = { qty -> repository.setItemQuantity(prod.id, qty) },
            onDismiss = { selectedProductForDetail = null }
        )
    }
}

@Composable
fun RetailerProductCard(
    product: Product,
    currentCartQty: Int,
    onClickDetail: () -> Unit,
    onAddToCart: (Int) -> Unit,
    onUpdateCartQty: (Int) -> Unit
) {
    var selectedQty by remember { mutableIntStateOf(1) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickDetail() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClickDetail() }
                ) {
                    // Product Icon / Image
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MintLight),
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
                                imageVector = when (product.category.lowercase()) {
                                    "seeds" -> Icons.Default.Grass
                                    "fertilizers" -> Icons.Default.Science
                                    else -> Icons.Default.PestControl
                                },
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = product.itemName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (product.alias.isNotBlank()) {
                            Text(
                                text = product.alias,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${product.company} • ${product.category}",
                            fontSize = 11.sp,
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Pack size chip & Info button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = product.packSize,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onClickDetail,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "View Product Details",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Stock row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(product.sellingRate),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    if (product.mrp > product.sellingRate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MRP ${formatCurrency(product.mrp)}",
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = product.packSize.ifBlank { product.unit },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Available Stock
                val inStock = product.currentStock > 0
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (inStock) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = if (inStock) "Stock: ${product.currentStock.toInt()} ${product.unit}" else "Out of Stock",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inStock) Color(0xFF166534) else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Add to Cart / Quantity Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code: ${product.itemCode}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (currentCartQty > 0) {
                    // Already in cart: Quantity modifier
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateCartQty(currentCartQty - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$currentCartQty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onUpdateCartQty(currentCartQty + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    // Quantity selector + Add to Cart button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (selectedQty > 1) selectedQty-- },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = "$selectedQty",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { selectedQty++ },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onAddToCart(selectedQty) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("add_to_cart_button")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetailerProductDetailDialog(
    product: Product,
    currentCartQty: Int,
    onAddToCart: (Int) -> Unit,
    onUpdateCartQty: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var detailQty by remember { mutableIntStateOf(if (currentCartQty > 0) currentCartQty else 1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Product Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ForestGreenPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Image Banner / Preview
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintLight),
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
                                    imageVector = when (product.category.lowercase()) {
                                        "seeds" -> Icons.Default.Grass
                                        "fertilizers" -> Icons.Default.Science
                                        else -> Icons.Default.PestControl
                                    },
                                    contentDescription = null,
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }

                    // Product Titles & Badges
                    item {
                        Column {
                            Text(
                                text = product.itemName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (product.alias.isNotBlank()) {
                                Text(
                                    text = "Also known as: ${product.alias}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MintLight
                                ) {
                                    Text(
                                        text = product.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = product.company,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = product.packSize,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Price & Margin Card
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Wholesale Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = formatCurrency(product.sellingRate),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenPrimary
                                        )
                                    }
                                    if (product.mrp > product.sellingRate) {
                                        val margin = product.mrp - product.sellingRate
                                        val marginPercent = ((margin / product.mrp) * 100).toInt()
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFDCFCE7)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = "Margin: ${formatCurrency(margin)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534)
                                                )
                                                Text(
                                                    text = "$marginPercent% Retail Profit",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF166534)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "MRP: ${formatCurrency(product.mrp)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Packing: ${product.packSize.ifBlank { product.unit }}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ForestGreenPrimary)
                                }
                            }
                        }
                    }

                    // Stock & Availability
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (product.currentStock > 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (product.currentStock > 0) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (product.currentStock > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (product.currentStock > 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (product.currentStock > 0) "Current Available Stock" else "Currently Out of Stock",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (product.currentStock > 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                }
                                Text(
                                    text = "${product.currentStock.toInt()} ${product.unit}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (product.currentStock > 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    // Product Specifications
                    item {
                        Column {
                            Text(
                                text = "Specifications & Identifiers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Item Code", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(product.itemCode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    if (product.barcode.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Barcode / EAN", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(product.barcode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Packing Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${product.packSize} (${product.packing})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Description / Dosage
                    if (product.description.isNotBlank()) {
                        item {
                            Column {
                                Text(
                                    text = "Technical Formulation & Usage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.description,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Cart Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Qty Selector
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (detailQty > 1) {
                                        detailQty--
                                        if (currentCartQty > 0) onUpdateCartQty(detailQty)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$detailQty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    detailQty++
                                    if (currentCartQty > 0) onUpdateCartQty(detailQty)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (currentCartQty > 0) {
                                onUpdateCartQty(detailQty)
                            } else {
                                onAddToCart(detailQty)
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            if (currentCartQty > 0) Icons.Default.Check else Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (currentCartQty > 0) "Update Cart ($detailQty)" else "Add to Cart ($detailQty)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
