package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.repository.AgroRepository
import com.example.data.repository.AuthManager
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: AgroRepository
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = AgroRepository(context)
        authManager = AuthManager(context)
    }

    @Test
    fun testAppNameStringResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AgroRetail", appName)
    }

    @Test
    fun testPinHashingAndVerification() {
        val pin = "1234"
        val hash = SecurityUtils.hashPin(pin)
        assertNotEquals(pin, hash)
        assertTrue(SecurityUtils.verifyPin("1234", hash))
        assertFalse(SecurityUtils.verifyPin("9999", hash))
    }

    @Test
    fun testCartOperationsAndOrderPlacement() {
        val testProduct = Product(
            id = "test_prod_1",
            itemName = "Test Fertilizer",
            sellingRate = 500.0,
            gstPercent = 18.0,
            currentStock = 50.0
        )

        repository.clearCart()
        assertEquals(0, repository.cart.value.size)

        repository.addToCart(testProduct, 2)
        val cart = repository.cart.value
        assertEquals(1, cart.size)
        assertEquals(2, cart[testProduct.id]?.quantity)

        val item = cart[testProduct.id]!!
        assertEquals(1000.0, item.totalAmount, 0.01)
        assertEquals(0.0, item.gstAmount, 0.01)
        assertEquals(1000.0, item.grandTotal, 0.01)

        val retailer = repository.retailers.value.first()
        val order = repository.placeOrder(retailer, notes = "Urgent delivery")
        assertNotNull(order)
        assertTrue(order.id.startsWith("ORD-"))
        assertEquals(OrderStatus.Pending, order.status)
        assertEquals(1000.0, order.grandTotal, 0.01)

        // Cart should be empty after placement
        assertEquals(0, repository.cart.value.size)
    }

    @Test
    fun testPassbookLedgerCalculations() {
        val retailer = repository.retailers.value.first()
        val initialEntries = repository.passbookEntries.value.filter { it.retailerId == retailer.id }
        val initialCount = initialEntries.size

        repository.addPassbookEntry(
            retailerId = retailer.id,
            invoiceNumber = "TEST-INV-100",
            description = "Test Invoice Debit",
            debit = 1000.0,
            credit = 0.0
        )

        val updatedEntries = repository.passbookEntries.value.filter { it.retailerId == retailer.id }
        assertEquals(initialCount + 1, updatedEntries.size)

        val lastEntry = updatedEntries.last()
        assertEquals(1000.0, lastEntry.debit, 0.01)
        assertEquals(0.0, lastEntry.credit, 0.01)
    }

    @Test
    fun testAuthManagerAdminAndRetailerSession() {
        authManager.logout()
        assertNull(authManager.currentUser.value)

        authManager.setAdminSession("Distributor Head", "admin@agroretail.com")
        assertNotNull(authManager.currentUser.value)
        assertEquals("Distributor Head", authManager.currentUser.value?.name)

        val retailer = repository.retailers.value.first()
        authManager.setRetailerSession(retailer)
        assertEquals(retailer.id, authManager.currentUser.value?.id)
        assertEquals(retailer.id, authManager.currentRetailer.value?.id)

        authManager.logout()
        assertNull(authManager.currentUser.value)
        assertNull(authManager.currentRetailer.value)
    }

    @Test
    fun testRetailerExcelAutoMappingAndFlexibleImport() {
        val headers = listOf("Party Name", "Phone", "Town", "GSTIN No")
        val mapping = com.example.util.ExcelParserHelper.autoMapRetailerColumns(headers)

        assertEquals("Party Name", mapping["retailerName"])
        assertEquals("Phone", mapping["mobileNumber"])
        assertEquals("Town", mapping["city"])
        assertEquals("GSTIN No", mapping["gstNumber"])
        // Optional columns like category or credit limit should be empty and not cause failure
        assertEquals("", mapping["category"])
        assertEquals("", mapping["creditLimit"])

        // Test minimal 3-column import with new retailer
        val minimalRows = listOf(
            mapOf("Party Name" to "Punjab Krishi Kendra", "Phone" to "9123456780", "Town" to "Ludhiana")
        )
        val result = repository.executeRetailerImport(
            fileName = "busy_sample.csv",
            parsedRows = minimalRows,
            columnMapping = mapping,
            duplicateMode = "UPDATE",
            adminName = "Admin"
        )
        assertEquals(1, result.totalRows)
        assertEquals(1, result.imported)
        assertEquals(0, result.errors)
        assertTrue(repository.retailers.value.any { it.retailerName == "Punjab Krishi Kendra" })
    }

    @Test
    fun testProductExcelAutoMappingAndStockHandling() {
        val headers = listOf("Item Description", "Brand", "Dealer Price", "Current Qty")
        val mapping = com.example.util.ExcelParserHelper.autoMapProductColumns(headers)

        assertEquals("Item Description", mapping["itemName"])
        assertEquals("Brand", mapping["company"])
        assertEquals("Dealer Price", mapping["sellingRate"])
        assertEquals("Current Qty", mapping["currentStock"])

        val rows = listOf(
            mapOf("Item Description" to "Super Insecticide 500ml", "Brand" to "Bayer", "Dealer Price" to "450", "Current Qty" to "25")
        )
        val result = repository.executeImport(
            fileName = "busy_items.csv",
            parsedRows = rows,
            columnMapping = mapping,
            duplicateMode = "UPDATE",
            stockImportMode = "SET_CURRENT",
            adminName = "Admin"
        )
        assertEquals(1, result.totalRows)
        assertEquals(1, result.imported)
        assertEquals(0, result.errors)
        val importedProduct = repository.products.value.find { it.itemName == "Super Insecticide 500ml" }
        assertNotNull(importedProduct)
        assertEquals(450.0, importedProduct?.sellingRate ?: 0.0, 0.01)
        assertEquals(25.0, importedProduct?.currentStock ?: 0.0, 0.01)
    }
}
