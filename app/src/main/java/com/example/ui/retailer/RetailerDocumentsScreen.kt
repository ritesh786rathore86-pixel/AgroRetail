package com.example.ui.retailer

import androidx.compose.foundation.background
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
fun RetailerDocumentsScreen(
    retailer: Retailer,
    repository: AgroRepository
) {
    val context = LocalContext.current
    val allDocs by repository.manualDocuments.collectAsState()
    val retailerDocs = remember(allDocs, retailer.id) {
        allDocs.filter { it.retailerId == retailer.id }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredDocs = remember(retailerDocs, selectedFilter) {
        when (selectedFilter) {
            "STATEMENTS" -> retailerDocs.filter { it.documentType.contains("Statement", ignoreCase = true) }
            "BILLS" -> retailerDocs.filter { it.documentType.contains("Bill", ignoreCase = true) || it.documentType.contains("Invoice", ignoreCase = true) }
            "OTHER" -> retailerDocs.filter { !it.documentType.contains("Statement", ignoreCase = true) && !it.documentType.contains("Bill", ignoreCase = true) && !it.documentType.contains("Invoice", ignoreCase = true) }
            else -> retailerDocs
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Documents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Official PDFs uploaded by Admin for your account",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Document Categories Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All Documents (${retailerDocs.size})") }
                    )
                }
                item {
                    val count = retailerDocs.count { it.documentType.contains("Statement", ignoreCase = true) }
                    FilterChip(
                        selected = selectedFilter == "STATEMENTS",
                        onClick = { selectedFilter = "STATEMENTS" },
                        label = { Text("Statements ($count)") }
                    )
                }
                item {
                    val count = retailerDocs.count { it.documentType.contains("Bill", ignoreCase = true) || it.documentType.contains("Invoice", ignoreCase = true) }
                    FilterChip(
                        selected = selectedFilter == "BILLS",
                        onClick = { selectedFilter = "BILLS" },
                        label = { Text("Bills ($count)") }
                    )
                }
                item {
                    val count = retailerDocs.count { !it.documentType.contains("Statement", ignoreCase = true) && !it.documentType.contains("Bill", ignoreCase = true) && !it.documentType.contains("Invoice", ignoreCase = true) }
                    FilterChip(
                        selected = selectedFilter == "OTHER",
                        onClick = { selectedFilter = "OTHER" },
                        label = { Text("Other PDFs ($count)") }
                    )
                }
            }

            if (filteredDocs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Description,
                    title = "No Documents Found",
                    message = if (retailerDocs.isEmpty()) {
                        "Your distributor admin has not uploaded any manual documents for your account yet."
                    } else {
                        "No documents match the selected filter."
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        RetailerDocumentCard(
                            doc = doc,
                            onViewPdf = {
                                val file = AgroPdfHelper.getOrGenerateManualDocumentPdf(context, doc)
                                AgroPdfHelper.openPdf(context, file, doc.title)
                            },
                            onSharePdf = {
                                val file = AgroPdfHelper.getOrGenerateManualDocumentPdf(context, doc)
                                AgroPdfHelper.sharePdf(context, file, doc.title)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetailerDocumentCard(
    doc: ManualDocument,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Uploaded on ${formatDate(doc.uploadedAt)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = doc.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (doc.originalFileName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "File: ${doc.originalFileName}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSharePdf,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp)
                }

                Button(
                    onClick = onViewPdf,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View PDF", fontSize = 12.sp)
                }
            }
        }
    }
}
