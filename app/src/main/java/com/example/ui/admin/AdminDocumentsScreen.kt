package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ManualDocument
import com.example.data.model.Retailer
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatDate
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroPdfHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDocumentsScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val allDocs by repository.manualDocuments.collectAsState()
    val retailers by repository.retailers.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedRetailerFilter by remember { mutableStateOf<String?>(null) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var documentToDelete by remember { mutableStateOf<ManualDocument?>(null) }

    val filteredDocs = remember(allDocs, selectedRetailerFilter, selectedTypeFilter) {
        allDocs.filter { doc ->
            val matchesRetailer = selectedRetailerFilter == null || doc.retailerId == selectedRetailerFilter
            val matchesType = when (selectedTypeFilter) {
                "STATEMENTS" -> doc.documentType.contains("Statement", ignoreCase = true)
                "BILLS" -> doc.documentType.contains("Bill", ignoreCase = true) || doc.documentType.contains("Invoice", ignoreCase = true)
                "OTHER" -> !doc.documentType.contains("Statement", ignoreCase = true) && !doc.documentType.contains("Bill", ignoreCase = true) && !doc.documentType.contains("Invoice", ignoreCase = true)
                else -> true
            }
            matchesRetailer && matchesType
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadDialog = true },
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                text = { Text("Upload PDF") },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Admin Documents Repository",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Manually upload statements, bills, or PDFs for specific retailers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreenPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${allDocs.size} Uploaded",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ForestGreenPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Retailer Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedRetailerFilter == null,
                        onClick = { selectedRetailerFilter = null },
                        label = { Text("All Retailers") }
                    )
                }
                items(retailers) { ret ->
                    val docCount = allDocs.count { it.retailerId == ret.id }
                    FilterChip(
                        selected = selectedRetailerFilter == ret.id,
                        onClick = { selectedRetailerFilter = ret.id },
                        label = { Text("${ret.businessName} ($docCount)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Document Type Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "ALL",
                        onClick = { selectedTypeFilter = "ALL" },
                        label = { Text("All Types") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "STATEMENTS",
                        onClick = { selectedTypeFilter = "STATEMENTS" },
                        label = { Text("Statements") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "BILLS",
                        onClick = { selectedTypeFilter = "BILLS" },
                        label = { Text("Bills / Invoices") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "OTHER",
                        onClick = { selectedTypeFilter = "OTHER" },
                        label = { Text("Other PDFs") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredDocs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Description,
                    title = "No Documents Found",
                    message = "Tap 'Upload PDF' below to assign a manual PDF document to a retailer.",
                    actionButtonText = "Upload PDF",
                    onActionClick = { showUploadDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        AdminDocumentCard(
                            doc = doc,
                            onViewPdf = {
                                val file = AgroPdfHelper.getOrGenerateManualDocumentPdf(context, doc)
                                AgroPdfHelper.openPdf(context, file, doc.title)
                            },
                            onSharePdf = {
                                val file = AgroPdfHelper.getOrGenerateManualDocumentPdf(context, doc)
                                AgroPdfHelper.sharePdf(context, file, doc.title)
                            },
                            onDelete = {
                                documentToDelete = doc
                            }
                        )
                    }
                }
            }
        }
    }

    // Upload Document Dialog
    if (showUploadDialog) {
        AdminUploadDocumentDialog(
            retailers = retailers,
            preSelectedRetailerId = selectedRetailerFilter,
            onDismiss = { showUploadDialog = false },
            onUpload = { retailerId, docType, title, notes, pdfUri, fileName ->
                var finalPdfPath = ""
                if (pdfUri != null) {
                    val copied = AgroPdfHelper.copyUriToInternalPdf(context, pdfUri, fileName)
                    if (copied != null) {
                        finalPdfPath = copied.absolutePath
                    }
                }
                repository.uploadManualDocument(
                    retailerId = retailerId,
                    documentType = docType,
                    title = title,
                    notes = notes,
                    pdfUrl = finalPdfPath,
                    fileName = fileName
                )
                Toast.makeText(context, "Document uploaded and assigned to retailer", Toast.LENGTH_SHORT).show()
                showUploadDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to delete '${doc.title}' assigned to ${doc.retailerName}? The retailer will no longer see this document.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteManualDocument(doc.id)
                        documentToDelete = null
                        Toast.makeText(context, "Document removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminDocumentCard(
    doc: ManualDocument,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit
) {
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
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    doc.documentType.contains("Statement", ignoreCase = true) -> Color(0xFFEFF6FF)
                                    doc.documentType.contains("Bill", ignoreCase = true) -> Color(0xFFECFDF5)
                                    else -> Color(0xFFFDF4FF)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                doc.documentType.contains("Statement", ignoreCase = true) -> Icons.Default.Summarize
                                doc.documentType.contains("Bill", ignoreCase = true) -> Icons.Default.ReceiptLong
                                else -> Icons.Default.PictureAsPdf
                            },
                            contentDescription = null,
                            tint = when {
                                doc.documentType.contains("Statement", ignoreCase = true) -> Color(0xFF2563EB)
                                doc.documentType.contains("Bill", ignoreCase = true) -> ForestGreenPrimary
                                else -> Color(0xFF9333EA)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = doc.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = doc.retailerName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        doc.documentType.contains("Statement", ignoreCase = true) -> Color(0xFFDBEAFE)
                        doc.documentType.contains("Bill", ignoreCase = true) -> Color(0xFFD1FAE5)
                        else -> Color(0xFFF3E8FF)
                    }
                ) {
                    Text(
                        text = doc.documentType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            doc.documentType.contains("Statement", ignoreCase = true) -> Color(0xFF1E40AF)
                            doc.documentType.contains("Bill", ignoreCase = true) -> ForestGreenPrimary
                            else -> Color(0xFF7E22CE)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (doc.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${doc.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Uploaded: ${formatDate(doc.uploadedAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (doc.originalFileName.isNotBlank()) {
                    Text(
                        text = doc.originalFileName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSharePdf,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onViewPdf,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUploadDocumentDialog(
    retailers: List<Retailer>,
    preSelectedRetailerId: String?,
    onDismiss: () -> Unit,
    onUpload: (retailerId: String, docType: String, title: String, notes: String, pdfUri: Uri?, fileName: String) -> Unit
) {
    val context = LocalContext.current
    var selectedRetailer by remember {
        mutableStateOf(retailers.find { it.id == preSelectedRetailerId } ?: retailers.firstOrNull())
    }
    var retailerDropdownExpanded by remember { mutableStateOf(false) }

    val docTypes = listOf("Statement PDF", "Bill PDF", "Other PDF")
    var selectedDocType by remember { mutableStateOf(docTypes.first()) }
    var docTypeDropdownExpanded by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            val (name, _) = AgroPdfHelper.getPdfMetadata(context, uri)
            selectedFileName = name
            if (title.isBlank()) {
                title = name.removeSuffix(".pdf").replace("_", " ")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = ForestGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Document for Retailer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retailer Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = retailerDropdownExpanded,
                    onExpandedChange = { retailerDropdownExpanded = !retailerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRetailer?.let { "${it.businessName} (${it.city})" } ?: "Select Retailer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Retailer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = retailerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = retailerDropdownExpanded,
                        onDismissRequest = { retailerDropdownExpanded = false }
                    ) {
                        retailers.forEach { ret ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(ret.businessName, fontWeight = FontWeight.SemiBold)
                                        Text("${ret.retailerName} • ${ret.city}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedRetailer = ret
                                    retailerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Document Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = docTypeDropdownExpanded,
                    onExpandedChange = { docTypeDropdownExpanded = !docTypeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDocType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = docTypeDropdownExpanded,
                        onDismissRequest = { docTypeDropdownExpanded = false }
                    ) {
                        docTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedDocType = type
                                    docTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title *") },
                    placeholder = { Text("e.g. Account Statement FY 25-26") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarks / Notes (Optional)") },
                    placeholder = { Text("e.g. Monthly ledger statement for reconciliation") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // PDF File Selection Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedPdfUri != null) ForestGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pdfPickerLauncher.launch("application/pdf") }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedPdfUri != null) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (selectedPdfUri != null) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedFileName.isNotBlank()) selectedFileName else "Select PDF File from Device",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedPdfUri != null) ForestGreenPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedPdfUri != null) "PDF chosen. Ready to upload." else "Tap to browse .pdf files",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ret = selectedRetailer
                    if (ret == null) {
                        Toast.makeText(context, "Please select a retailer", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a document title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onUpload(
                        ret.id,
                        selectedDocType,
                        title,
                        notes,
                        selectedPdfUri,
                        if (selectedFileName.isNotBlank()) selectedFileName else "$title.pdf"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
            ) {
                Text("Upload & Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
