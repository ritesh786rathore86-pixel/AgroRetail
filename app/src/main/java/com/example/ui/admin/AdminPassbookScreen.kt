package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DistributorProfile
import com.example.data.model.PassbookEntry
import com.example.data.model.Retailer
import com.example.data.model.Statement
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroPdfHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPassbookScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val retailers by repository.retailers.collectAsState()
    val entries by repository.passbookEntries.collectAsState()
    val statements by repository.statements.collectAsState()
    val distributorProfile by repository.distributorProfile.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Passbook Ledger, 1 = Uploaded Statements
    var isAddingEntry by remember { mutableStateOf(false) }
    var isUploadingStatement by remember { mutableStateOf(false) }
    var selectedStatementForPreview by remember { mutableStateOf<Statement?>(null) }

    var selectedRetailerFilter by remember { mutableStateOf<Retailer?>(null) }
    var expandedRetailerFilter by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, selectedRetailerFilter) {
        val list = if (selectedRetailerFilter == null) entries
        else entries.filter { it.retailerId == selectedRetailerFilter?.id }
        list.sortedByDescending { it.date }
    }

    val filteredStatements = remember(statements, selectedRetailerFilter) {
        val list = if (selectedRetailerFilter == null) statements
        else statements.filter { it.retailerId == selectedRetailerFilter?.id }
        list.sortedByDescending { it.uploadedAt }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) isAddingEntry = true
                    else isUploadingStatement = true
                },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (selectedTabIndex == 0) Icons.Default.PostAdd else Icons.Default.UploadFile,
                    contentDescription = "Add"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Retailer Selector Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedRetailerFilter,
                    onExpandedChange = { expandedRetailerFilter = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedRetailerFilter?.businessName ?: "All Retailers (${retailers.size})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRetailerFilter) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRetailerFilter,
                        onDismissRequest = { expandedRetailerFilter = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Retailers") },
                            onClick = {
                                selectedRetailerFilter = null
                                expandedRetailerFilter = false
                            }
                        )
                        retailers.forEach { ret ->
                            DropdownMenuItem(
                                text = { Text(ret.businessName) },
                                onClick = {
                                    selectedRetailerFilter = ret
                                    expandedRetailerFilter = false
                                }
                            )
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreenPrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Ledger Entries (${filteredEntries.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("PDF Statements (${filteredStatements.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTabIndex == 0) {
                if (filteredEntries.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.MenuBook,
                        title = "No Ledger Entries",
                        message = "Post debit or credit transactions to update retailer passbook running balances.",
                        actionButtonText = "+ Add Transaction Entry",
                        onActionClick = { isAddingEntry = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredEntries, key = { it.id }) { entry ->
                            val retName = retailers.find { it.id == entry.retailerId }?.businessName ?: "Retailer"
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.description,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "$retName • Ref: ${entry.invoiceNumber} • ${formatDateShort(entry.date)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (entry.debit > 0) {
                                            Text(
                                                text = "- ${formatCurrency(entry.debit)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFFDC2626)
                                            )
                                        } else {
                                            Text(
                                                text = "+ ${formatCurrency(entry.credit)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF16A34A)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Running Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = formatCurrency(entry.runningBalance), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (filteredStatements.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = "No PDF Statements Uploaded",
                        message = "Upload certified monthly/quarterly ledger PDF statements for retailers.",
                        actionButtonText = "+ Upload Statement PDF",
                        onActionClick = { isUploadingStatement = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredStatements, key = { it.id }) { stmt ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStatementForPreview = stmt }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFEF2F2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = "PDF",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(text = stmt.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "${stmt.retailerName} • Period: ${stmt.period}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = "Uploaded: ${formatDateShort(stmt.uploadedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            val ret = retailers.find { it.id == stmt.retailerId }
                                            val retEntries = entries.filter { it.retailerId == stmt.retailerId }
                                            val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, ret, retEntries)
                                            AgroPdfHelper.openPdf(context, pdfFile, stmt.title)
                                        }) {
                                            Icon(Icons.Default.Visibility, contentDescription = "View", tint = ForestGreenPrimary)
                                        }

                                        IconButton(onClick = {
                                            val ret = retailers.find { it.id == stmt.retailerId }
                                            val retEntries = entries.filter { it.retailerId == stmt.retailerId }
                                            val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, ret, retEntries)
                                            AgroPdfHelper.sharePdf(context, pdfFile, "${stmt.title} for ${stmt.retailerName}")
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = HarvestAmber)
                                        }

                                        IconButton(onClick = {
                                            repository.deleteStatement(stmt.id)
                                            Toast.makeText(context, "Statement deleted", Toast.LENGTH_SHORT).show()
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

    // Add Ledger Transaction Dialog
    if (isAddingEntry) {
        AddLedgerEntryDialog(
            retailers = retailers,
            onDismiss = { isAddingEntry = false },
            onSave = { ret, refNum, desc, debit, credit ->
                repository.addPassbookEntry(
                    retailerId = ret.id,
                    invoiceNumber = refNum,
                    description = desc,
                    debit = debit,
                    credit = credit,
                    date = System.currentTimeMillis()
                )
                Toast.makeText(context, "Ledger entry posted successfully!", Toast.LENGTH_SHORT).show()
                isAddingEntry = false
            }
        )
    }

    // Upload Statement Dialog
    if (isUploadingStatement) {
        UploadStatementDialog(
            retailers = retailers,
            distributorProfile = distributorProfile,
            passbookEntries = entries,
            onDismiss = { isUploadingStatement = false },
            onUpload = { ret, title, period, finalPdfPath, fileName ->
                repository.uploadStatement(
                    retailerId = ret.id,
                    documentType = "Account Statement",
                    title = title,
                    period = period,
                    pdfUrl = finalPdfPath,
                    fileName = fileName
                )
                Toast.makeText(context, "Statement uploaded & sent to retailer!", Toast.LENGTH_SHORT).show()
                isUploadingStatement = false
            }
        )
    }

    // Statement Preview Dialog
    selectedStatementForPreview?.let { stmt ->
        val retailer = retailers.find { it.id == stmt.retailerId }
        val retEntries = entries.filter { it.retailerId == stmt.retailerId }
        Dialog(onDismissRequest = { selectedStatementForPreview = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stmt.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { selectedStatementForPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Retailer: ${stmt.retailerName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(text = "Period: ${stmt.period}", fontSize = 12.sp)
                            Text(text = "Uploaded On: ${formatDateShort(stmt.uploadedAt)}", fontSize = 12.sp)
                            if (stmt.originalFileName.isNotBlank()) {
                                Text(text = "File Name: ${stmt.originalFileName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, retailer, retEntries)
                            AgroPdfHelper.openPdf(context, pdfFile, stmt.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in PDF Reader")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, retailer, retEntries)
                            AgroPdfHelper.sharePdf(context, pdfFile, "${stmt.title} for ${stmt.retailerName}")
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = ForestGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share via WhatsApp / Email", color = ForestGreenPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerEntryDialog(
    retailers: List<Retailer>,
    onDismiss: () -> Unit,
    onSave: (Retailer, String, String, Double, Double) -> Unit
) {
    var selectedRetailer by remember { mutableStateOf<Retailer?>(retailers.firstOrNull()) }
    var refNumber by remember { mutableStateOf("RCP-${(1000..9999).random()}") }
    var description by remember { mutableStateOf("Payment Received via NEFT / Cash") }
    var entryType by remember { mutableStateOf("CREDIT") } // "CREDIT" or "DEBIT"
    var amount by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Post Ledger Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedRetailer?.businessName ?: "Select Retailer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Retailer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        retailers.forEach { ret ->
                            DropdownMenuItem(
                                text = { Text("${ret.businessName} (Outstanding: ${formatCurrency(ret.outstandingAmount)})") },
                                onClick = {
                                    selectedRetailer = ret
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = entryType == "CREDIT",
                        onClick = {
                            entryType = "CREDIT"
                            description = "Payment Received via NEFT / Cash"
                        },
                        label = { Text("Credit (Payment Received)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = entryType == "DEBIT",
                        onClick = {
                            entryType = "DEBIT"
                            description = "Goods Invoice Purchase / Debit Note"
                        },
                        label = { Text("Debit (Charge)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("Voucher / Ref Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            val ret = selectedRetailer
                            if (ret != null && amt > 0) {
                                val debit = if (entryType == "DEBIT") amt else 0.0
                                val credit = if (entryType == "CREDIT") amt else 0.0
                                onSave(ret, refNumber, description, debit, credit)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text("Post Entry")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadStatementDialog(
    retailers: List<Retailer>,
    distributorProfile: DistributorProfile,
    passbookEntries: List<PassbookEntry>,
    onDismiss: () -> Unit,
    onUpload: (Retailer, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedRetailer by remember { mutableStateOf<Retailer?>(retailers.firstOrNull()) }
    var title by remember { mutableStateOf("Quarterly Account Statement") }
    var period by remember { mutableStateOf("Q1 FY 2025-26 (Apr - Jun)") }
    var expandedDropdown by remember { mutableStateOf(false) }

    // PDF attachment states
    var attachedPdfFile by remember { mutableStateOf<File?>(null) }
    var attachedFileName by remember { mutableStateOf("") }
    var attachedFileSize by remember { mutableStateOf(0L) }
    var isAutoGenerateSelected by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = AgroPdfHelper.getPdfMetadata(context, uri)
            val copiedFile = AgroPdfHelper.copyUriToInternalPdf(context, uri, meta.first)
            if (copiedFile != null) {
                attachedPdfFile = copiedFile
                attachedFileName = meta.first
                attachedFileSize = meta.second
                isAutoGenerateSelected = false
                Toast.makeText(context, "PDF '${meta.first}' attached successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not copy PDF file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upload Certified Statement PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = ForestGreenPrimary
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRetailer?.businessName ?: "Select Retailer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Retailer *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            retailers.forEach { ret ->
                                DropdownMenuItem(
                                    text = { Text("${ret.businessName} (${ret.retailerName})") },
                                    onClick = {
                                        selectedRetailer = ret
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Statement Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = period,
                        onValueChange = { period = it },
                        label = { Text("Statement Period (e.g. FY 2025-26)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "ATTACH STATEMENT PDF DOCUMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary,
                        letterSpacing = 0.5.sp
                    )

                    if (attachedPdfFile != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = attachedFileName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (attachedFileSize > 0) "${attachedFileSize / 1024} KB • Attached & Ready" else "Attached & Ready",
                                            fontSize = 11.sp,
                                            color = ForestGreenPrimary
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        attachedPdfFile?.let { AgroPdfHelper.openPdf(context, it, "Preview Attached Statement") }
                                    }) {
                                        Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = ForestGreenPrimary)
                                    }
                                    IconButton(onClick = {
                                        attachedPdfFile = null
                                        attachedFileName = ""
                                        attachedFileSize = 0L
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    } else if (isAutoGenerateSelected) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = HarvestAmber.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = HarvestAmber,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Auto-Generate Passbook Ledger PDF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Will compile retailer's transaction entries into a formal ledger document.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { isAutoGenerateSelected = false }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose PDF from Device / Storage")
                            }

                            OutlinedButton(
                                onClick = { isAutoGenerateSelected = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = HarvestAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto-Generate Certified Statement PDF", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val ret = selectedRetailer
                            if (ret != null && title.isNotBlank()) {
                                val (finalPath, finalName) = if (attachedPdfFile != null) {
                                    Pair(attachedPdfFile!!.absolutePath, attachedFileName)
                                } else {
                                    val tempStmt = Statement(
                                        id = "stmt_${System.currentTimeMillis()}",
                                        retailerId = ret.id,
                                        retailerName = ret.businessName,
                                        documentType = "Account Statement",
                                        title = title.trim(),
                                        period = period.trim(),
                                        uploadedAt = System.currentTimeMillis()
                                    )
                                    val retEntries = passbookEntries.filter { it.retailerId == ret.id }
                                    val generatedFile = AgroPdfHelper.generateStatementPdf(context, tempStmt, distributorProfile, ret, retEntries)
                                    Pair(generatedFile.absolutePath, "Statement_${ret.businessName.replace(" ", "_")}.pdf")
                                }
                                onUpload(ret, title.trim(), period.trim(), finalPath, finalName)
                            } else {
                                Toast.makeText(context, "Please select a retailer and title", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upload")
                    }
                }
            }
        }
    }
}
