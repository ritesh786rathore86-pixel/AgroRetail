package com.example.data.model

enum class UserRole {
    ADMIN,
    RETAILER
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val role: UserRole = UserRole.RETAILER,
    val retailerId: String = ""
)

data class AdminUser(
    val id: String = "",
    val email: String = "",
    val name: String = ""
)

data class DistributorProfile(
    val companyName: String = "Siddhi Vinayak Krishi Vikas Kendra",
    val ownerName: String = "Suresh Sharma",
    val email: String = "contact@siddhivinayak.com",
    val phone: String = "9876543210",
    val gstin: String = "07AAAAA0000A1Z5",
    val address: String = "Main Mandi Road, Sector 14, Karnal, Haryana",
    val city: String = "Karnal",
    val state: String = "Haryana",
    val bankName: String = "State Bank of India",
    val accountNumber: String = "389201948210",
    val ifscCode: String = "SBIN0001234",
    val upiId: String = "siddhivinayak@sbi"
)

data class Retailer(
    val id: String = "",
    val businessName: String = "",
    val retailerName: String = "",
    val partyCode: String = "",
    val mobileNumber: String = "",
    val alternatePhone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val pinHash: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val gstNumber: String = "",
    val category: String = "SILVER", // Configurable: e.g. GOLD, SILVER, PLATINUM
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class RetailerCategory(
    val id: String = "",
    val name: String = "",
    val colorHex: String = "#EAB308", // Hex color representation
    val description: String = "",
    val minOrderValue: Double = 0.0,
    val discountPercent: Double = 0.0
)

data class Product(
    val id: String = "",
    val itemCode: String = "",
    val itemName: String = "",
    val alias: String = "",
    val company: String = "",
    val category: String = "", // e.g. Pesticides, Fertilizers, Seeds, Bio-Nutrients
    val subCategory: String = "",
    val unit: String = "PCS", // PCS, BOX, BAG, BOTTLE, PACK, KG, LITRE, OTHER
    val packSize: String = "1 Ltr",
    val packing: String = "Box of 10",
    val purchaseRate: Double = 0.0,
    val sellingRate: Double = 0.0,
    val mrp: Double = 0.0,
    val gstPercent: Double = 0.0,
    val hsnCode: String = "",
    val barcode: String = "",
    val openingStock: Double = 0.0,
    val currentStock: Double = 0.0,
    val godown: String = "",
    val batch: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val isVisible: Boolean = true, // VISIBLE TO RETAILERS: ON/OFF
    val isDelisted: Boolean = false, // Delisted remains in Admin records but hidden from retailers
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Company(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val description: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val isActive: Boolean = true
)

enum class OrderStatus {
    Pending,
    Accepted,
    Rejected,
    Completed,
    Cancelled
}

data class OrderItem(
    val productId: String = "",
    val itemCode: String = "",
    val itemName: String = "",
    val company: String = "",
    val packSize: String = "",
    val unit: String = "PCS",
    val quantity: Int = 1,
    val rate: Double = 0.0,
    val gstPercent: Double = 0.0,
    val total: Double = 0.0,
    val imageUrl: String = ""
)

data class Order(
    val id: String = "", // e.g. ORD-1025 or ORD-2026-000001
    val retailerId: String = "",
    val retailerName: String = "",
    val retailerBusinessName: String = "",
    val retailerMobile: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val items: List<OrderItem> = emptyList(),
    val subTotal: Double = 0.0,
    val gstTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val notes: String = "",
    val status: OrderStatus = OrderStatus.Pending,
    val adminNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class ManualDocument(
    val id: String = "",
    val retailerId: String = "",
    val retailerName: String = "",
    val documentType: String = "Statement PDF", // "Statement PDF", "Bill PDF", "Other PDF"
    val title: String = "",
    val notes: String = "",
    val pdfUrl: String = "",
    val originalFileName: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)

data class Bill(
    val id: String = "",
    val billNumber: String = "",
    val retailerId: String = "",
    val retailerName: String = "",
    val billDate: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val pdfUrl: String = "",
    val originalFileName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class PassbookEntry(
    val id: String = "",
    val retailerId: String = "",
    val date: Long = System.currentTimeMillis(),
    val invoiceNumber: String = "",
    val description: String = "",
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val runningBalance: Double = 0.0
)

data class Statement(
    val id: String = "",
    val retailerId: String = "",
    val retailerName: String = "",
    val documentType: String = "Account Statement",
    val title: String = "",
    val period: String = "",
    val pdfUrl: String = "",
    val originalFileName: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)

data class Poster(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val fileType: String = "IMAGE", // IMAGE, PDF
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // default +30 days
    val priority: Int = 1, // lower number = higher priority
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Admin",
    val notificationTitle: String = "",
    val notificationMessage: String = ""
)

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetRetailerId: String = "ALL", // "ALL" or specific retailerId
    val linkedType: String = "NONE", // ORDER, BILL, PASSBOOK, STATEMENT, POSTER, PAYMENT_REMINDER, NONE
    val linkedId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class PaymentReminder(
    val id: String = "",
    val retailerId: String = "",
    val retailerName: String = "",
    val retailerMobile: String = "",
    val outstandingAmount: Double = 0.0,
    val dueDate: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val message: String = "",
    val billNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val snoozedUntil: Long = 0L
)

data class ImportHistoryItem(
    val id: String = "",
    val fileName: String = "",
    val adminName: String = "Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val totalRows: Int = 0,
    val imported: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val errors: Int = 0,
    val errorReport: List<String> = emptyList(),
    val stockImportMode: String = "REPLACE", // REPLACE or ADD
    val importType: String = "PRODUCTS" // "RETAILERS" or "PRODUCTS"
) {
    val status: String get() = if (errors == 0) "SUCCESS" else "WITH_ERRORS"
    val errorLogs: List<String> get() = errorReport
}

data class CartItem(
    val product: Product,
    val quantity: Int,
    val selectedUnit: String = product.unit.ifBlank { "PCS" }
) {
    val totalAmount: Double get() = product.sellingRate * quantity
    val gstAmount: Double get() = 0.0 // No GST calculation
    val grandTotal: Double get() = totalAmount
}

enum class PasswordResetStatus {
    Pending,
    Approved,
    Rejected,
    Resolved
}

data class PasswordResetRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val retailerId: String = "",
    val retailerName: String = "",
    val retailerBusinessName: String = "",
    val mobileNumber: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: PasswordResetStatus = PasswordResetStatus.Pending,
    val adminNote: String = "",
    val resolvedAt: Long = 0L,
    val temporaryPin: String = ""
)

enum class RecycleBinType {
    ORDER,
    POSTER,
    PRODUCT,
    RETAILER,
    DOCUMENT,
    BILL,
    REMINDER
}

data class RecycleBinItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalId: String = "",
    val itemType: RecycleBinType = RecycleBinType.ORDER,
    val title: String = "",
    val subtitle: String = "",
    val details: String = "",
    val payloadJson: String = "",
    val retailerId: String = "", // for retailer isolation
    val deletedBy: String = "Admin",
    val deletedAt: Long = System.currentTimeMillis()
)

