package com.example.ui.admin

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.util.ExcelParserHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRetailersScreen(
    repository: AgroRepository,
    onNavigateToExcelImport: () -> Unit = {}
) {
    val context = LocalContext.current
    val retailers by repository.retailers.collectAsState()
    val categories by repository.retailerCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    var retailerBeingEdited by remember { mutableStateOf<Retailer?>(null) }
    var retailerBeingViewed by remember { mutableStateOf<Retailer?>(null) }
    var retailerToDelete by remember { mutableStateOf<Retailer?>(null) }
    var isAddingNewRetailer by remember { mutableStateOf(false) }

    val filteredRetailers = remember(retailers, searchQuery, selectedCategoryFilter) {
        retailers.filter { ret ->
            (selectedCategoryFilter == "ALL" || ret.category.equals(selectedCategoryFilter, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
                ret.businessName.contains(searchQuery, ignoreCase = true) ||
                ret.retailerName.contains(searchQuery, ignoreCase = true) ||
                ret.partyCode.contains(searchQuery, ignoreCase = true) ||
                ret.mobileNumber.contains(searchQuery, ignoreCase = true) ||
                ret.city.contains(searchQuery, ignoreCase = true) ||
                ret.state.contains(searchQuery, ignoreCase = true)
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNewRetailer = true },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Retailer")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Action Banner: Excel Import & Download Sample
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
                        onClick = { shareRetailerSample(context) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DOWNLOAD RETAILER SAMPLE EXCEL", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, party code, phone, city...") },
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
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val catFilters = listOf("ALL", "PLATINUM", "GOLD", "SILVER")
                catFilters.forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (filteredRetailers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Storefront,
                    title = "No Retailers Found",
                    message = "Add retailers manually or import your customer list from Excel / BUSY.",
                    actionButtonText = "+ Add Retailer",
                    onActionClick = { isAddingNewRetailer = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredRetailers, key = { it.id }) { retailer ->
                        AdminRetailerCard(
                            retailer = retailer,
                            onView = { retailerBeingViewed = retailer },
                            onEdit = { retailerBeingEdited = retailer },
                            onDelete = { retailerToDelete = retailer },
                            onToggleActive = { repository.toggleRetailerActive(retailer.id) },
                            onChangeCategory = { newCat ->
                                repository.saveRetailer(retailer.copy(category = newCat))
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Retailer Dialog
    if (isAddingNewRetailer || retailerBeingEdited != null) {
        RetailerFormDialog(
            initialRetailer = retailerBeingEdited ?: Retailer(),
            onDismiss = {
                isAddingNewRetailer = false
                retailerBeingEdited = null
            },
            onSave = { savedRetailer, plainPin ->
                repository.saveRetailer(savedRetailer, plainPin)
                Toast.makeText(context, "Retailer saved successfully", Toast.LENGTH_SHORT).show()
                isAddingNewRetailer = false
                retailerBeingEdited = null
            }
        )
    }

    // View Details Dialog
    if (retailerBeingViewed != null) {
        RetailerDetailsDialog(
            retailer = retailerBeingViewed!!,
            onDismiss = { retailerBeingViewed = null },
            onEdit = {
                val target = retailerBeingViewed
                retailerBeingViewed = null
                retailerBeingEdited = target
            }
        )
    }

    // Delete Confirmation Dialog
    if (retailerToDelete != null) {
        AlertDialog(
            onDismissRequest = { retailerToDelete = null },
            title = { Text("Delete Retailer?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${retailerToDelete!!.businessName} (${retailerToDelete!!.retailerName})? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = retailerToDelete!!.id
                        repository.deleteRetailer(id)
                        Toast.makeText(context, "Retailer deleted", Toast.LENGTH_SHORT).show()
                        retailerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { retailerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminRetailerCard(
    retailer: Retailer,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onChangeCategory: (String) -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (retailer.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = retailer.businessName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (!retailer.isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "INACTIVE",
                                    color = Color(0xFF991B1B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Prop: ${retailer.retailerName} • Mob: +91 ${retailer.mobileNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (retailer.partyCode.isNotBlank()) {
                        Text(
                            text = "Party Code: ${retailer.partyCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenPrimary
                        )
                    }
                    Text(
                        text = "${retailer.address}, ${retailer.city}${if (retailer.state.isNotBlank()) ", ${retailer.state}" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category pill (Clickable to change)
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (retailer.category.uppercase()) {
                            "PLATINUM" -> Color(0xFFE0E7FF)
                            "GOLD" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFF1F5F9)
                        },
                        modifier = Modifier.clickable { showCategoryMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = retailer.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (retailer.category.uppercase()) {
                                    "PLATINUM" -> Color(0xFF3730A3)
                                    "GOLD" -> Color(0xFF92400E)
                                    else -> Color(0xFF475569)
                                }
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        listOf("PLATINUM", "GOLD", "SILVER", "BRONZE").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onChangeCategory(cat)
                                    showCategoryMenu = false
                                }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Outstanding Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(retailer.outstandingAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (retailer.outstandingAmount > 0) Color(0xFFDC2626) else ForestGreenPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Credit Limit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(retailer.creditLimit),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = retailer.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action row: View, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details", fontSize = 11.sp)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun RetailerDetailsDialog(
    retailer: Retailer,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = retailer.businessName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (retailer.isActive) "Active Customer" else "Inactive / Deactivated",
                            fontSize = 12.sp,
                            color = if (retailer.isActive) ForestGreenPrimary else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Party Code", retailer.partyCode.ifBlank { "Not set" })
                    DetailRow("Contact Person", retailer.retailerName)
                    DetailRow("Mobile", "+91 ${retailer.mobileNumber}")
                    DetailRow("WhatsApp", if (retailer.whatsappNumber.isNotBlank()) "+91 ${retailer.whatsappNumber}" else "Same as mobile")
                    DetailRow("Email", retailer.email.ifBlank { "None" })
                    DetailRow("Category", retailer.category)
                    DetailRow("GSTIN", retailer.gstNumber.ifBlank { "Unregistered" })
                    DetailRow("Address", "${retailer.address}, ${retailer.city} - ${retailer.pincode}, ${retailer.state}")
                    DetailRow("Credit Limit", formatCurrency(retailer.creditLimit))
                    DetailRow("Opening Balance", formatCurrency(retailer.openingBalance))
                    DetailRow("Current Outstanding", formatCurrency(retailer.outstandingAmount))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RetailerFormDialog(
    initialRetailer: Retailer,
    onDismiss: () -> Unit,
    onSave: (Retailer, String?) -> Unit
) {
    var businessName by remember { mutableStateOf(initialRetailer.businessName) }
    var retailerName by remember { mutableStateOf(initialRetailer.retailerName) }
    var partyCode by remember { mutableStateOf(initialRetailer.partyCode) }
    var mobileNumber by remember { mutableStateOf(initialRetailer.mobileNumber) }
    var whatsappNumber by remember { mutableStateOf(initialRetailer.whatsappNumber) }
    var email by remember { mutableStateOf(initialRetailer.email) }
    var pin by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(initialRetailer.address) }
    var city by remember { mutableStateOf(initialRetailer.city) }
    var state by remember { mutableStateOf(initialRetailer.state) }
    var pincode by remember { mutableStateOf(initialRetailer.pincode) }
    var gstNumber by remember { mutableStateOf(initialRetailer.gstNumber) }
    var category by remember { mutableStateOf(initialRetailer.category.ifBlank { "SILVER" }) }
    var creditLimit by remember { mutableStateOf(if (initialRetailer.creditLimit > 0) initialRetailer.creditLimit.toString() else "100000") }
    var openingBalance by remember { mutableStateOf(if (initialRetailer.openingBalance > 0) initialRetailer.openingBalance.toString() else "0") }
    var outstandingAmount by remember { mutableStateOf(initialRetailer.outstandingAmount.toString()) }
    var isActive by remember { mutableStateOf(initialRetailer.isActive) }

    Dialog(onDismissRequest = onDismiss) {
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
                        text = if (initialRetailer.id.isBlank()) "Add New Retailer" else "Edit Retailer Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
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
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Firm / Shop Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = retailerName,
                        onValueChange = { retailerName = it },
                        label = { Text("Owner / Retailer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = partyCode,
                            onValueChange = { partyCode = it.uppercase() },
                            label = { Text("Party Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it.uppercase() },
                            label = { Text("Category (GOLD...)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) mobileNumber = it },
                        label = { Text("Mobile Number (10 digits) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) whatsappNumber = it },
                        label = { Text("WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text(if (initialRetailer.id.isBlank()) "Security PIN (4-6 digits) *" else "Reset PIN (blank to keep)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = creditLimit,
                            onValueChange = { creditLimit = it },
                            label = { Text("Credit Limit (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = openingBalance,
                            onValueChange = { openingBalance = it },
                            label = { Text("Opening Bal (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = { gstNumber = it.uppercase() },
                        label = { Text("GSTIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it },
                            label = { Text("Pincode") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isActive) "Active (Can Login & Place Orders)" else "Deactivated (Blocked from Ordering)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (businessName.isBlank() || mobileNumber.length != 10) return@Button
                            val updated = initialRetailer.copy(
                                businessName = businessName.trim(),
                                retailerName = retailerName.trim(),
                                partyCode = partyCode.trim(),
                                mobileNumber = mobileNumber.trim(),
                                whatsappNumber = whatsappNumber.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                city = city.trim(),
                                state = state.trim(),
                                pincode = pincode.trim(),
                                gstNumber = gstNumber.trim(),
                                category = category.trim().ifBlank { "SILVER" },
                                creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                                openingBalance = openingBalance.toDoubleOrNull() ?: 0.0,
                                outstandingAmount = outstandingAmount.toDoubleOrNull() ?: 0.0,
                                isActive = isActive
                            )
                            onSave(updated, if (pin.isNotBlank()) pin else null)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text("Save Retailer")
                    }
                }
            }
        }
    }
}

private fun shareRetailerSample(context: Context) {
    try {
        val sampleCsv = ExcelParserHelper.generateRetailerSampleCsv()
        val file = File(context.cacheDir, "Retailer_Sample_Import.csv")
        file.writeText(sampleCsv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Retailer Sample CSV Template")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download / Share Retailer Sample CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Saved retailer sample template to cache", Toast.LENGTH_SHORT).show()
    }
}
