package com.example.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.model.ImportHistoryItem
import com.example.data.repository.AgroRepository
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.util.ExcelParserHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExcelImportScreen(
    repository: AgroRepository,
    initialImportType: String = "RETAILERS", // "RETAILERS" or "PRODUCTS"
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    var importType by remember { mutableStateOf(initialImportType) } // "RETAILERS" or "PRODUCTS"

    // Stepper: 0 = Upload / Choose file, 1 = Column Mapping, 2 = Preview & Confirm, 3 = Result Summary
    var currentStep by remember { mutableIntStateOf(0) }
    var fileName by remember { mutableStateOf("") }
    var detectedHeaders by remember { mutableStateOf<List<String>>(emptyList()) }
    var parsedRows by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    // Column Mapping state
    var columnMapping by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Duplicate & Stock settings
    var duplicateMode by remember { mutableStateOf("UPDATE_EXISTING") } // "UPDATE_EXISTING", "SKIP", "CREATE_NEW"
    var stockImportMode by remember { mutableStateOf("REPLACE") } // "REPLACE" or "ADD"

    var lastImportResult by remember { mutableStateOf<ImportHistoryItem?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // File Picker for .xlsx, .xls, .csv
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val name = uri.lastPathSegment?.substringAfterLast("/") ?: "import_file.xlsx"
                fileName = name
                val sheet = ExcelParserHelper.parseDocument(context, uri, name)
                if (sheet.rows.isNotEmpty()) {
                    parsedRows = sheet.rows
                    detectedHeaders = sheet.headers
                    columnMapping = if (importType == "RETAILERS") {
                        ExcelParserHelper.autoMapRetailerColumns(sheet.headers)
                    } else {
                        ExcelParserHelper.autoMapProductColumns(sheet.headers)
                    }
                    currentStep = 1
                    Toast.makeText(context, "Loaded ${sheet.rows.size} rows from $fileName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No rows found in file. Loading sample demo data.", Toast.LENGTH_LONG).show()
                    loadDemoData(importType) { fName, headers, rows, mapping ->
                        fileName = fName
                        detectedHeaders = headers
                        parsedRows = rows
                        columnMapping = mapping
                        currentStep = 1
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (importType == "RETAILERS") "Retailers Excel Import" else "Products Excel Import",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Supports .xlsx, .xls (BUSY/Tally), and .csv",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                ),
                actions = {
                    if (currentStep > 0) {
                        IconButton(onClick = {
                            currentStep = 0
                            parsedRows = emptyList()
                            fileName = ""
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Start Over", tint = ForestGreenPrimary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Mode Switcher (only on Step 0)
            if (currentStep == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    TabButton(
                        title = "👥 Retailers / Customers",
                        isSelected = importType == "RETAILERS",
                        modifier = Modifier.weight(1f)
                    ) {
                        importType = "RETAILERS"
                    }
                    TabButton(
                        title = "📦 Products / Items",
                        isSelected = importType == "PRODUCTS",
                        modifier = Modifier.weight(1f)
                    ) {
                        importType = "PRODUCTS"
                    }
                }
            }

            // Stepper indicator
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImportStepChip(number = 1, title = "Upload", isActive = currentStep >= 0, isCurrent = currentStep == 0)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    ImportStepChip(number = 2, title = "Map Columns", isActive = currentStep >= 1, isCurrent = currentStep == 1)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    ImportStepChip(number = 3, title = "Preview", isActive = currentStep >= 2, isCurrent = currentStep == 2)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    ImportStepChip(number = 4, title = "Result", isActive = currentStep >= 3, isCurrent = currentStep == 3)
                }
            }

            // Step Content
            when (currentStep) {
                0 -> Step0UploadView(
                    importType = importType,
                    onSelectFile = { filePicker.launch("*/*") },
                    onDownloadSample = {
                        shareSampleFile(context, importType)
                    },
                    onLoadDemoData = {
                        loadDemoData(importType) { fName, headers, rows, mapping ->
                            fileName = fName
                            detectedHeaders = headers
                            parsedRows = rows
                            columnMapping = mapping
                            currentStep = 1
                        }
                    }
                )

                1 -> Step1MappingView(
                    importType = importType,
                    fileName = fileName,
                    detectedHeaders = detectedHeaders,
                    columnMapping = columnMapping,
                    onMappingChanged = { key, header ->
                        columnMapping = columnMapping.toMutableMap().apply { put(key, header) }
                    },
                    onProceed = {
                        val nameCol = if (importType == "RETAILERS") columnMapping["retailerName"] else columnMapping["itemName"]
                        if (nameCol.isNullOrBlank()) {
                            Toast.makeText(context, "Please map at least the Name column to proceed.", Toast.LENGTH_LONG).show()
                        } else {
                            currentStep = 2
                        }
                    },
                    onBack = { currentStep = 0 }
                )

                2 -> Step2PreviewAndConfirmView(
                    importType = importType,
                    fileName = fileName,
                    rows = parsedRows,
                    columnMapping = columnMapping,
                    duplicateMode = duplicateMode,
                    stockImportMode = stockImportMode,
                    onDuplicateModeChange = { duplicateMode = it },
                    onStockModeChange = { stockImportMode = it },
                    isProcessing = isProcessing,
                    onStartImport = {
                        isProcessing = true
                        val result = if (importType == "RETAILERS") {
                            repository.executeRetailerImport(
                                fileName = fileName,
                                adminName = "Admin",
                                parsedRows = parsedRows,
                                columnMapping = columnMapping,
                                duplicateMode = duplicateMode
                            )
                        } else {
                            repository.executeImport(
                                fileName = fileName,
                                adminName = "Admin",
                                parsedRows = parsedRows,
                                columnMapping = columnMapping,
                                duplicateMode = duplicateMode,
                                stockImportMode = stockImportMode
                            )
                        }
                        lastImportResult = result
                        isProcessing = false
                        currentStep = 3
                    },
                    onBack = { currentStep = 1 }
                )

                3 -> Step3ResultSummaryView(
                    result = lastImportResult,
                    importType = importType,
                    onImportAnother = {
                        currentStep = 0
                        parsedRows = emptyList()
                        fileName = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ForestGreenPrimary else Color.Transparent,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Step0UploadView(
    importType: String,
    onSelectFile: () -> Unit,
    onDownloadSample: () -> Unit,
    onLoadDemoData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (importType == "RETAILERS") Icons.Default.GroupAdd else Icons.Default.PostAdd,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (importType == "RETAILERS") "Import Retailers from Excel" else "Import Products from Excel",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Upload any .xlsx, .xls, or .csv exported from BUSY, Tally, or custom spreadsheets. The smart parser adapts to any column format automatically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Select File Button
                Button(
                    onClick = onSelectFile,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Excel / CSV File", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // One-click Demo Button (crucial for quick testing on emulator)
                OutlinedButton(
                    onClick = onLoadDemoData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = HarvestAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (importType == "RETAILERS") "Load BUSY Retailer Demo Data" else "Load BUSY Product Demo Data",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample Download Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sample Excel Template (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You do NOT need to match this exact template. You can upload any BUSY or accounting export directly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDownloadSample,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary.copy(alpha = 0.15f), contentColor = ForestGreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (importType == "RETAILERS") "Download Retailer Sample Excel" else "Download Product Sample Excel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flexible Import Assurance Note
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Flexible Column Matching", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• Never rejects files for missing columns.\n• Imports whatever useful data is present.\n• If only Party Name & Mobile exist, those are imported gracefully.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1MappingView(
    importType: String,
    fileName: String,
    detectedHeaders: List<String>,
    columnMapping: Map<String, String>,
    onMappingChanged: (String, String) -> Unit,
    onProceed: () -> Unit,
    onBack: () -> Unit
) {
    val systemFields = if (importType == "RETAILERS") {
        listOf(
            "retailerName" to "Retailer / Party Name *" to "e.g. Kisan Beej Bhandar",
            "businessName" to "Firm / Shop Name" to "e.g. Kisan Agro Agency",
            "partyCode" to "Party / Customer Code" to "e.g. RET-001 or KIS01",
            "mobileNumber" to "Mobile / Phone" to "e.g. 9812345001",
            "whatsappNumber" to "WhatsApp Number" to "e.g. 9812345001",
            "address" to "Address" to "Shop / Street address",
            "city" to "City" to "e.g. Karnal",
            "state" to "State" to "e.g. Haryana",
            "pincode" to "Pincode" to "e.g. 132001",
            "gstNumber" to "GSTIN / Tax ID" to "e.g. 06AAAAA1111A1Z1",
            "email" to "Email" to "e.g. kisan@gmail.com",
            "category" to "Category" to "e.g. GOLD, SILVER, PLATINUM",
            "creditLimit" to "Credit Limit (₹)" to "e.g. 200000",
            "openingBalance" to "Opening Balance (₹)" to "e.g. 25000"
        )
    } else {
        listOf(
            "itemName" to "Product / Item Name *" to "e.g. Confidor 100ml",
            "itemCode" to "Item Code / SKU" to "e.g. BAY-001",
            "company" to "Company / Brand" to "e.g. Bayer CropScience",
            "category" to "Category / Group" to "e.g. Insecticides",
            "unit" to "Unit (PCS, BAG, etc.)" to "e.g. PCS, LTR, KG",
            "packSize" to "Pack Size" to "e.g. 100 ml, 1 Ltr, 50 Kg",
            "packing" to "Master Packing" to "e.g. Box of 20, Bag",
            "purchaseRate" to "Purchase Rate" to "Cost price for distributor",
            "sellingRate" to "Selling Rate / Dealer Price" to "Price charged to retailer",
            "mrp" to "MRP" to "Maximum Retail Price",
            "hsnCode" to "HSN Code" to "e.g. 38089190",
            "openingStock" to "Opening Stock" to "Initial inventory count",
            "currentStock" to "Current Stock" to "Available stock quantity",
            "godown" to "Godown / Warehouse" to "e.g. Main Godown, Godown A",
            "batch" to "Batch Number" to "e.g. B24-09",
            "description" to "Description / Notes" to "Key application notes"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // File summary header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = fileName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detected ${detectedHeaders.size} columns from file. Review the auto-detected mapping below or change as needed.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mapping List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(systemFields) { (fieldDef, hint) ->
                val (fieldKey, fieldLabel) = fieldDef
                val selectedHeader = columnMapping[fieldKey] ?: ""
                MappingRowCard(
                    fieldLabel = fieldLabel,
                    fieldHint = hint,
                    selectedHeader = selectedHeader,
                    availableHeaders = detectedHeaders,
                    onHeaderSelected = { onMappingChanged(fieldKey, it) }
                )
            }
        }

        // Bottom Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onProceed,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Preview & Confirm", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappingRowCard(
    fieldLabel: String,
    fieldHint: String,
    selectedHeader: String,
    availableHeaders: List<String>,
    onHeaderSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedHeader.isNotBlank()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selectedHeader.isNotBlank()) 1.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = fieldLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = fieldHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedHeader.isNotBlank()) ForestGreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        modifier = Modifier.clickable { expanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedHeader.ifBlank { "(Skip / None)" },
                                fontSize = 12.sp,
                                fontWeight = if (selectedHeader.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedHeader.isNotBlank()) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(Skip / None)", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onHeaderSelected("")
                                expanded = false
                            }
                        )
                        Divider()
                        availableHeaders.forEach { header ->
                            DropdownMenuItem(
                                text = { Text(header) },
                                onClick = {
                                    onHeaderSelected(header)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2PreviewAndConfirmView(
    importType: String,
    fileName: String,
    rows: List<Map<String, String>>,
    columnMapping: Map<String, String>,
    duplicateMode: String,
    stockImportMode: String,
    onDuplicateModeChange: (String) -> Unit,
    onStockModeChange: (String) -> Unit,
    isProcessing: Boolean,
    onStartImport: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Header
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Rows", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${rows.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ForestGreenPrimary)
                    }
                    Divider(modifier = Modifier.height(36.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mapped Columns", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${columnMapping.values.count { it.isNotBlank() }}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GoldenSun)
                    }
                    Divider(modifier = Modifier.height(36.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (importType == "RETAILERS") "Retailers" else "Products", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Column Detection Status Card (Section 8: EXCEL PREVIEW)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Column Detection Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(fileName, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val detectedFields = columnMapping.filter { it.value.isNotBlank() }
                    val notAvailableFields = columnMapping.filter { it.value.isBlank() }

                    Text("Detected in File (${detectedFields.size}):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ForestGreenPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        detectedFields.forEach { (key, col) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ForestGreenPrimary.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${formatFieldLabel(key)} ✓ ($col)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ForestGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (notAvailableFields.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Not Available in File (${notAvailableFields.size}):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            notAvailableFields.keys.take(8).forEach { key ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${formatFieldLabel(key)} -",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Missing optional columns will NOT fail the import. The importer safely reads whatever fields are present.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Duplicate Resolution Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("If Record Already Exists:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    DuplicateOptionRow(
                        title = "Update Existing Record",
                        subtitle = "Overwrites non-empty details (recommended)",
                        isSelected = duplicateMode == "UPDATE_EXISTING",
                        onClick = { onDuplicateModeChange("UPDATE_EXISTING") }
                    )
                    DuplicateOptionRow(
                        title = "Skip Duplicate",
                        subtitle = "Keeps current data and skips the row",
                        isSelected = duplicateMode == "SKIP",
                        onClick = { onDuplicateModeChange("SKIP") }
                    )
                    DuplicateOptionRow(
                        title = "Create New Record",
                        subtitle = "Creates another entry as a separate record",
                        isSelected = duplicateMode == "CREATE_NEW",
                        onClick = { onDuplicateModeChange("CREATE_NEW") }
                    )
                }
            }

            // For Products: Stock Update Mode
            if (importType == "PRODUCTS") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stock Import Mode:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        DuplicateOptionRow(
                            title = "Replace Current Stock",
                            subtitle = "Sets stock to exactly what is in this file",
                            isSelected = stockImportMode == "REPLACE",
                            onClick = { onStockModeChange("REPLACE") }
                        )
                        DuplicateOptionRow(
                            title = "Add to Current Stock",
                            subtitle = "Adds Excel quantity to existing stock balance",
                            isSelected = stockImportMode == "ADD",
                            onClick = { onStockModeChange("ADD") }
                        )
                    }
                }
            }

            // First 5 Rows Preview Table
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data Preview (First 5 Rows):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val previewRows = rows.take(5)
                    previewRows.forEachIndexed { idx, r ->
                        val title = if (importType == "RETAILERS") {
                            r[columnMapping["retailerName"]] ?: r[columnMapping["businessName"]] ?: "Row ${idx + 1}"
                        } else {
                            r[columnMapping["itemName"]] ?: "Row ${idx + 1}"
                        }
                        val subtitle = if (importType == "RETAILERS") {
                            "Mobile: ${r[columnMapping["mobileNumber"]] ?: "-"} | City: ${r[columnMapping["city"]] ?: "-"}"
                        } else {
                            "Rate: ₹${r[columnMapping["sellingRate"]] ?: "-"} | Stock: ${r[columnMapping["currentStock"]] ?: "-"} ${r[columnMapping["unit"]] ?: ""}"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onStartImport,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.weight(2f)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importing...")
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Import (${rows.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Step3ResultSummaryView(
    result: ImportHistoryItem?,
    importType: String,
    onImportAnother: () -> Unit
) {
    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No result found.")
        }
        return
    }

    val context = LocalContext.current
    var showIssuesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big Success Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (result.errors == 0) ForestGreenPrimary.copy(alpha = 0.15f) else GoldenSun.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (result.errors == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (result.errors == 0) ForestGreenPrimary else GoldenSun,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "IMPORT COMPLETED",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            color = ForestGreenPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${if (importType == "RETAILERS") "Retailers" else "Products"} • ${result.fileName}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Metrics Breakdown Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${if (importType == "RETAILERS") "Retailers" else "Products"}:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricBox(title = "Total", value = "${result.totalRows}", color = MaterialTheme.colorScheme.onSurface)
                    MetricBox(title = "Imported", value = "${result.imported}", color = ForestGreenPrimary)
                    MetricBox(title = "Updated", value = "${result.updated}", color = GoldenSun)
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricBox(title = "Skipped", value = "${result.skipped}", color = MaterialTheme.colorScheme.outline)
                    MetricBox(
                        title = "Needs Review",
                        value = "${result.errors}",
                        color = if (result.errors > 0) MaterialTheme.colorScheme.error else ForestGreenPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: [ VIEW ISSUES ] and [ DOWNLOAD IMPORT REPORT ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showIssuesDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("VIEW ISSUES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { downloadImportReport(context, result, importType) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DOWNLOAD REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Issues preview section if any
        if (result.errorReport.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Needs Review / Warnings (${result.errorReport.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    result.errorReport.take(5).forEach { err ->
                        Text("• $err", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    if (result.errorReport.size > 5) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+ ${result.errorReport.size - 5} more issues. Click [ VIEW ISSUES ] above.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onImportAnother,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Another Excel / CSV", fontWeight = FontWeight.Bold)
        }
    }

    // Full Issues Dialog
    if (showIssuesDialog) {
        Dialog(onDismissRequest = { showIssuesDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Import Issues (${result.errorReport.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showIssuesDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (result.errorReport.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No issues or skipped rows found!\nAll rows were processed successfully.",
                                textAlign = TextAlign.Center,
                                color = ForestGreenPrimary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(result.errorReport) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = GoldenSun,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = item, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showIssuesDialog = false
                            downloadImportReport(context, result, importType)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Detailed Report")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
    }
}

@Composable
private fun ImportStepChip(number: Int, title: String, isActive: Boolean, isCurrent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> ForestGreenPrimary
                        isActive -> ForestGreenPrimary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = if (isCurrent) Color.White else if (isActive) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun shareSampleFile(context: Context, importType: String) {
    try {
        val sampleCsv = if (importType == "RETAILERS") {
            ExcelParserHelper.generateRetailerSampleCsv()
        } else {
            ExcelParserHelper.generateProductSampleCsv()
        }
        val prefix = if (importType == "RETAILERS") "Retailer_Sample_Import" else "Product_Sample_Import"
        val file = File(context.cacheDir, "${prefix}.csv")
        file.writeText(sampleCsv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$prefix Template")
            putExtra(Intent.EXTRA_TEXT, "Here is the sample CSV template. Note that any BUSY or accounting export is also supported!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download / Share Sample CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Saved sample template to device cache.", Toast.LENGTH_SHORT).show()
    }
}

private fun loadDemoData(
    importType: String,
    onLoaded: (fileName: String, headers: List<String>, rows: List<Map<String, String>>, mapping: Map<String, String>) -> Unit
) {
    if (importType == "RETAILERS") {
        val csv = ExcelParserHelper.generateRetailerSampleCsv()
        val sheet = ExcelParserHelper.parseBytes(csv.toByteArray(), "BUSY_Retailers_Export.csv")
        val mapping = ExcelParserHelper.autoMapRetailerColumns(sheet.headers)
        onLoaded("BUSY_Retailers_Export.csv", sheet.headers, sheet.rows, mapping)
    } else {
        val csv = ExcelParserHelper.generateProductSampleCsv()
        val sheet = ExcelParserHelper.parseBytes(csv.toByteArray(), "BUSY_Item_Master_Export.csv")
        val mapping = ExcelParserHelper.autoMapProductColumns(sheet.headers)
        onLoaded("BUSY_Item_Master_Export.csv", sheet.headers, sheet.rows, mapping)
    }
}

private fun downloadImportReport(context: Context, result: ImportHistoryItem, importType: String) {
    try {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date(result.timestamp))
        val report = buildString {
            appendLine("==================================================")
            appendLine("IMPORT REPORT - SIDDHI VINAYAK KRISHI VIKAS KENDRA")
            appendLine("==================================================")
            appendLine("Generated At   : $dateStr")
            appendLine("File Name      : ${result.fileName}")
            appendLine("Import Module  : ${if (importType == "RETAILERS") "Retailers / Customers" else "Products / Inventory"}")
            appendLine("Admin Operator : ${result.adminName}")
            appendLine("Overall Status : ${result.status}")
            appendLine("--------------------------------------------------")
            appendLine("EXECUTION SUMMARY:")
            appendLine("  Total Rows in File   : ${result.totalRows}")
            appendLine("  Newly Imported       : ${result.imported}")
            appendLine("  Updated Existing     : ${result.updated}")
            appendLine("  Skipped Duplicate    : ${result.skipped}")
            appendLine("  Needs Review / Issues: ${result.errors}")
            appendLine("--------------------------------------------------")
            if (result.errorReport.isNotEmpty()) {
                appendLine("DETAILED ISSUES & WARNINGS (${result.errorReport.size}):")
                result.errorReport.forEachIndexed { i, issue ->
                    appendLine("  [${i + 1}] $issue")
                }
            } else {
                appendLine("All rows processed cleanly without any critical errors.")
            }
            appendLine("==================================================")
        }

        val file = File(context.cacheDir, "Import_Report_${System.currentTimeMillis()}.txt")
        file.writeText(report)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Import Report - ${result.fileName}")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download / Share Import Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Report generated: ${result.fileName}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatFieldLabel(key: String): String = when (key) {
    "retailerName" -> "Retailer / Party Name"
    "businessName" -> "Firm / Shop Name"
    "partyCode" -> "Party / Customer Code"
    "mobileNumber" -> "Mobile / Phone"
    "whatsappNumber" -> "WhatsApp"
    "address" -> "Address"
    "city" -> "City"
    "state" -> "State"
    "pincode" -> "Pincode"
    "gstNumber" -> "GSTIN"
    "email" -> "Email"
    "category" -> "Category"
    "creditLimit" -> "Credit Limit"
    "openingBalance" -> "Opening Balance"
    "itemName" -> "Product / Item Name"
    "itemCode" -> "Item Code / SKU"
    "company" -> "Company / Brand"
    "unit" -> "Unit"
    "packSize" -> "Pack Size"
    "packing" -> "Packing"
    "purchaseRate" -> "Purchase Rate"
    "sellingRate" -> "Selling Rate"
    "mrp" -> "MRP"
    "hsnCode" -> "HSN Code"
    "openingStock" -> "Opening Stock"
    "currentStock" -> "Current Stock"
    "godown" -> "Godown"
    "batch" -> "Batch"
    "description" -> "Description"
    else -> key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}

