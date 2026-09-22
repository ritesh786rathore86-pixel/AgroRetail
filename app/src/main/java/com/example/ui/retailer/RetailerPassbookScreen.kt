package com.example.ui.retailer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PassbookEntry
import com.example.data.model.Retailer
import com.example.data.model.Statement
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateShort
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.HarvestAmber
import com.example.util.AgroPdfHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerPassbookScreen(
    retailer: Retailer,
    repository: AgroRepository
) {
    val context = LocalContext.current
    val passbookEntries by repository.passbookEntries.collectAsState()
    val statements by repository.statements.collectAsState()
    val distributorProfile by repository.distributorProfile.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Ledger Entries, 1 = Uploaded Statements
    var selectedStatementForPreview by remember { mutableStateOf<Statement?>(null) }

    val myEntries = remember(passbookEntries, retailer.id) {
        passbookEntries.filter { it.retailerId == retailer.id }.sortedByDescending { it.date }
    }

    val myStatements = remember(statements, retailer.id) {
        statements.filter { it.retailerId == retailer.id }.sortedByDescending { it.uploadedAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Passbook", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ForestGreenPrimary
                ),
                actions = {
                    IconButton(onClick = {
                        val liveStmt = Statement(
                            id = "stmt_live_${System.currentTimeMillis()}",
                            retailerId = retailer.id,
                            retailerName = retailer.businessName,
                            documentType = "Current Ledger Statement",
                            title = "Current Ledger Statement",
                            period = "Up to Date",
                            uploadedAt = System.currentTimeMillis()
                        )
                        val pdf = AgroPdfHelper.generateStatementPdf(context, liveStmt, distributorProfile, retailer, myEntries)
                        AgroPdfHelper.openPdf(context, pdf, "My Account Statement")
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Download Current Statement", tint = ForestGreenPrimary)
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
            // Outstanding Balance Header Card
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ForestGreenPrimary, Color(0xFF0F512B))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CLOSING RUNNING BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(retailer.outstandingAmount),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${retailer.businessName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldenSun,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreenPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Passbook Ledger (${myEntries.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Official Statements (${myStatements.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                // PASSBOOK ENTRIES
                if (myEntries.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.MenuBook,
                        title = "No Passbook Entries",
                        message = "Transactions and invoices will appear in your ledger once generated.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(myEntries, key = { it.id }) { entry ->
                            PassbookEntryCard(entry = entry)
                        }
                    }
                }
            } else {
                // STATEMENTS & OFFICIAL PASSBOOK PDFS
                if (myStatements.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = "No Official Statements Yet",
                        message = "Your distributor will upload certified monthly and quarterly statements here.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(myStatements, key = { it.id }) { stmt ->
                            StatementCard(
                                statement = stmt,
                                onClick = { selectedStatementForPreview = stmt },
                                onViewPdf = {
                                    val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, retailer, myEntries)
                                    AgroPdfHelper.openPdf(context, pdfFile, stmt.title)
                                },
                                onSharePdf = {
                                    val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, retailer, myEntries)
                                    AgroPdfHelper.sharePdf(context, pdfFile, stmt.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Statement Preview Dialog
    selectedStatementForPreview?.let { stmt ->
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
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Period: ${stmt.period}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Uploaded On: ${formatDateShort(stmt.uploadedAt)}", fontSize = 12.sp)
                            if (stmt.originalFileName.isNotBlank()) {
                                Text(text = "File Name: ${stmt.originalFileName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, retailer, myEntries)
                            AgroPdfHelper.openPdf(context, pdfFile, stmt.title)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in PDF Reader")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val pdfFile = AgroPdfHelper.getOrGenerateStatementPdf(context, stmt, distributorProfile, retailer, myEntries)
                            AgroPdfHelper.sharePdf(context, pdfFile, stmt.title)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
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

@Composable
fun PassbookEntryCard(entry: PassbookEntry) {
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Ref: ${entry.invoiceNumber} • ${formatDateShort(entry.date)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Debit or Credit
                if (entry.debit > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "- ${formatCurrency(entry.debit)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFDC2626) // Red Debit
                        )
                        Text(text = "Debit (Purchase)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+ ${formatCurrency(entry.credit)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF16A34A) // Green Credit
                        )
                        Text(text = "Credit (Payment)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Running Balance",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(entry.runningBalance),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
            }
        }
    }
}

@Composable
fun StatementCard(
    statement: Statement,
    onClick: () -> Unit,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = statement.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Period: ${statement.period}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Uploaded: ${formatDateShort(statement.uploadedAt)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onViewPdf) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View Statement",
                        tint = ForestGreenPrimary
                    )
                }

                IconButton(onClick = onSharePdf) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Statement",
                        tint = HarvestAmber
                    )
                }
            }
        }
    }
}
