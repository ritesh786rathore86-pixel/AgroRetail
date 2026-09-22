package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Bill
import com.example.data.model.DistributorProfile
import com.example.data.model.Retailer
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
fun AdminBillsScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val bills by repository.bills.collectAsState()
    val retailers by repository.retailers.collectAsState()
    val distributorProfile by repository.distributorProfile.collectAsState()

    var isAddingBill by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBillForPreview by remember { mutableStateOf<Bill?>(null) }

    val filteredBills = remember(bills, searchQuery) {
        bills.filter { b ->
            searchQuery.isBlank() ||
            b.billNumber.contains(searchQuery, ignoreCase = true) ||
            b.retailerName.contains(searchQuery, ignoreCase = true)
        }.sortedByDescending { it.billDate }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingBill = true },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload Bill")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Bill # or Retailer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreenPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filteredBills.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Receipt,
                    title = "No Invoices Uploaded",
                    message = "Upload GST invoices for retailers with real PDF documents. They will automatically be debited to their passbook.",
                    actionButtonText = "+ Upload New Invoice",
                    onActionClick = { isAddingBill = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBills, key = { it.id }) { bill ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBillForPreview = bill }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                        Text(
                                            text = "Invoice #${bill.billNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${bill.retailerName} • ${formatDateShort(bill.billDate)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = formatCurrency(bill.amount),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreenPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ForestGreenPrimary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "PDF Attached",
                                                    fontSize = 10.sp,
                                                    color = ForestGreenPrimary,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        val ret = retailers.find { it.id == bill.retailerId }
                                        val pdfFile = AgroPdfHelper.getOrGenerateInvoicePdf(context, bill, distributorProfile, ret)
                                        AgroPdfHelper.openPdf(context, pdfFile, "Invoice #${bill.billNumber}")
                                    }) {
                                        Icon(Icons.Default.Visibility, contentDescription = "View PDF", tint = ForestGreenPrimary)
                                    }

                                    IconButton(onClick = {
                                        val ret = retailers.find { it.id == bill.retailerId }
                                        val pdfFile = AgroPdfHelper.getOrGenerateInvoicePdf(context, bill, distributorProfile, ret)
                                        AgroPdfHelper.sharePdf(context, pdfFile, "Tax Invoice #${bill.billNumber} for ${bill.retailerName}")
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = HarvestAmber)
                                    }

                                    IconButton(onClick = {
                                        repository.deleteBill(bill.id)
                                        Toast.makeText(context, "Bill deleted", Toast.LENGTH_SHORT).show()
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

    if (isAddingBill) {
        UploadBillDialog(
            retailers = retailers,
            distributorProfile = distributorProfile,
            onDismiss = { isAddingBill = false },
            onUpload = { retailer, billNumber, amount, finalPdfPath, fileName ->
                repository.uploadBill(
                    retailerId = retailer.id,
                    billNumber = billNumber,
                    billDate = System.currentTimeMillis(),
                    amount = amount,
                    pdfUrl = finalPdfPath,
                    fileName = fileName
                )
                Toast.makeText(context, "Invoice uploaded, debited to passbook & notification sent!", Toast.LENGTH_LONG).show()
                isAddingBill = false
            }
        )
    }

    // Bill Details & PDF Preview Dialog
    selectedBillForPreview?.let { bill ->
        val retailer = retailers.find { it.id == bill.retailerId }
        Dialog(onDismissRequest = { selectedBillForPreview = null }) {
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
                            Text(text = "Tax Invoice #${bill.billNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { selectedBillForPreview = null }) {
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
                            Text(text = "Billed Retailer: ${bill.retailerName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(text = "Invoice Date: ${formatDateShort(bill.billDate)}", fontSize = 12.sp)
                            Text(text = "Total Amount: ${formatCurrency(bill.amount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreenPrimary)
                            if (bill.originalFileName.isNotBlank()) {
                                Text(text = "File Name: ${bill.originalFileName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PDF Actions
                    Button(
                        onClick = {
                            val pdfFile = AgroPdfHelper.getOrGenerateInvoicePdf(context, bill, distributorProfile, retailer)
                            AgroPdfHelper.openPdf(context, pdfFile, "Invoice #${bill.billNumber}")
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
                            val pdfFile = AgroPdfHelper.getOrGenerateInvoicePdf(context, bill, distributorProfile, retailer)
                            AgroPdfHelper.sharePdf(context, pdfFile, "Invoice #${bill.billNumber} for ${bill.retailerName}")
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
fun UploadBillDialog(
    retailers: List<Retailer>,
    distributorProfile: DistributorProfile,
    onDismiss: () -> Unit,
    onUpload: (Retailer, String, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedRetailer by remember { mutableStateOf<Retailer?>(retailers.firstOrNull()) }
    var billNumber by remember { mutableStateOf("INV-2026-${(1000..9999).random()}") }
    var amount by remember { mutableStateOf("") }
    var expandedRetailerDropdown by remember { mutableStateOf(false) }

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
                        text = "Upload GST Tax Invoice",
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
                    // Retailer Selection Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedRetailerDropdown,
                        onExpandedChange = { expandedRetailerDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRetailer?.let { "${it.businessName} (${it.retailerName})" } ?: "Select Retailer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Retailer *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRetailerDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedRetailerDropdown,
                            onDismissRequest = { expandedRetailerDropdown = false }
                        ) {
                            retailers.forEach { ret ->
                                DropdownMenuItem(
                                    text = { Text("${ret.businessName} (${ret.retailerName})") },
                                    onClick = {
                                        selectedRetailer = ret
                                        expandedRetailerDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = { billNumber = it },
                        label = { Text("Invoice / Bill Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                        label = { Text("Invoice Grand Total (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // PDF Attachment Section
                    Text(
                        text = "ATTACH INVOICE PDF DOCUMENT",
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
                                        attachedPdfFile?.let { AgroPdfHelper.openPdf(context, it, "Preview Attached Invoice") }
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
                                            text = "Auto-Generate Certified GST Invoice",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Will build official printable PDF with HSN & GST breakups.",
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
                        // Action Buttons to Attach or Auto Generate
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
                                Text("Auto-Generate Certified PDF Invoice", color = MaterialTheme.colorScheme.onSurface)
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
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            val ret = selectedRetailer
                            if (ret != null && billNumber.isNotBlank() && amt > 0) {
                                val (finalPath, finalName) = if (attachedPdfFile != null) {
                                    Pair(attachedPdfFile!!.absolutePath, attachedFileName)
                                } else {
                                    // Generate certified PDF directly
                                    val tempBill = Bill(
                                        id = "bill_${System.currentTimeMillis()}",
                                        billNumber = billNumber.trim(),
                                        retailerId = ret.id,
                                        retailerName = ret.businessName,
                                        amount = amt,
                                        billDate = System.currentTimeMillis()
                                    )
                                    val generatedFile = AgroPdfHelper.generateInvoicePdf(context, tempBill, distributorProfile, ret)
                                    Pair(generatedFile.absolutePath, "Invoice_${billNumber.trim()}.pdf")
                                }
                                onUpload(ret, billNumber.trim(), amt, finalPath, finalName)
                            } else {
                                Toast.makeText(context, "Please select retailer and enter valid amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upload & Post")
                    }
                }
            }
        }
    }
}

