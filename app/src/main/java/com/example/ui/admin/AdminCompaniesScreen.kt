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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Company
import com.example.data.repository.AgroRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCompaniesScreen(
    repository: AgroRepository
) {
    val context = LocalContext.current
    val companies by repository.companies.collectAsState()
    val products by repository.products.collectAsState()

    var companyBeingEdited by remember { mutableStateOf<Company?>(null) }
    var companyToDelete by remember { mutableStateOf<Company?>(null) }
    var isAddingCompany by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingCompany = true },
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                text = { Text("Add Company") },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Info Header
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
                        text = "Partner Companies & Brands",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Manage chemical, seed, and fertilizer manufacturers and customize their logos.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (companies.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Business,
                    title = "No Companies Found",
                    message = "Add partner chemical, seed, and fertilizer manufacturing companies.",
                    actionButtonText = "+ Add Company",
                    onActionClick = { isAddingCompany = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(companies, key = { it.id }) { company ->
                        val count = products.count { it.company.equals(company.name, ignoreCase = true) }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Company Logo
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MintLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (company.logoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = company.logoUrl,
                                            contentDescription = company.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = company.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenPrimary,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = company.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (!company.isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.errorContainer
                                            ) {
                                                Text(
                                                    text = "Inactive",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (company.description.isNotBlank()) {
                                        Text(
                                            text = company.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$count Products Linked • Contact: ${company.contactPerson.ifBlank { "N/A" }}",
                                        fontSize = 11.sp,
                                        color = ForestGreenPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { companyBeingEdited = company }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ForestGreenPrimary)
                                    }
                                    IconButton(onClick = { companyToDelete = company }) {
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

    // Add / Edit Dialog
    if (isAddingCompany || companyBeingEdited != null) {
        val editing = companyBeingEdited ?: Company()
        var name by remember { mutableStateOf(editing.name) }
        var logoUrl by remember { mutableStateOf(editing.logoUrl) }
        var description by remember { mutableStateOf(editing.description) }
        var contactPerson by remember { mutableStateOf(editing.contactPerson) }
        var phone by remember { mutableStateOf(editing.phone) }
        var email by remember { mutableStateOf(editing.email) }
        var isActive by remember { mutableStateOf(editing.isActive) }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                logoUrl = uri.toString()
            }
        }

        Dialog(onDismissRequest = {
            isAddingCompany = false
            companyBeingEdited = null
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (editing.id.isBlank()) "Add Company" else "Edit Company",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Company / Brand Name *") },
                        placeholder = { Text("e.g. Syngenta, Bayer, UPL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Logo Input with Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            label = { Text("Logo Image URL or Path") },
                            placeholder = { Text("https://... or select image") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { imagePicker.launch("image/*") }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Pick Image", tint = ForestGreenPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Crop protection & seeds") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("Representative / Contact Person") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Company Active Status:", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isAddingCompany = false
                                companyBeingEdited = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Please enter company name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val comp = editing.copy(
                                    name = name.trim(),
                                    logoUrl = logoUrl.trim(),
                                    description = description.trim(),
                                    contactPerson = contactPerson.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    isActive = isActive
                                )
                                repository.saveCompany(comp)
                                Toast.makeText(context, "Company saved", Toast.LENGTH_SHORT).show()
                                isAddingCompany = false
                                companyBeingEdited = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    companyToDelete?.let { company ->
        AlertDialog(
            onDismissRequest = { companyToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Company?") },
            text = { Text("Are you sure you want to delete '${company.name}'? Products linked to this company will remain in the catalog.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCompany(company.id)
                        companyToDelete = null
                        Toast.makeText(context, "Company deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { companyToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
