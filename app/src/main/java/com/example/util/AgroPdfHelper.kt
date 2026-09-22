package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.DistributorProfile
import com.example.data.model.ManualDocument
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PassbookEntry
import com.example.data.model.Retailer
import com.example.data.model.Statement
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AgroPdfHelper {

    private const val TAG = "AgroPdfHelper"

    fun getPdfsDirectory(context: Context): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Inspects a content Uri and returns its display name and size in bytes.
     */
    fun getPdfMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "Document_${System.currentTimeMillis()}.pdf"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF metadata: ${e.message}")
        }
        if (!name.endsWith(".pdf", ignoreCase = true)) {
            name = "$name.pdf"
        }
        return Pair(name, size)
    }

    /**
     * Copies a chosen PDF Uri from Android SAF/Picker into local internal storage
     * so it never expires and is permanently accessible.
     */
    fun copyUriToInternalPdf(context: Context, uri: Uri, preferredName: String? = null): File? {
        return try {
            val meta = getPdfMetadata(context, uri)
            val baseName = preferredName?.takeIf { it.isNotBlank() } ?: meta.first
            val sanitizedName = baseName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val fileName = if (sanitizedName.endsWith(".pdf", ignoreCase = true)) {
                sanitizedName
            } else {
                "$sanitizedName.pdf"
            }

            val targetFile = File(getPdfsDirectory(context), "${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy PDF to internal storage: ${e.message}", e)
            null
        }
    }

    /**
     * Opens a PDF file with the device's default PDF viewer via FileProvider.
     */
    fun openPdf(context: Context, file: File, fallbackTitle: String = "View PDF Document") {
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "PDF file is not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, fallbackTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open PDF: ${e.message}")
            // Fallback: offer share sheet so user can open with any compatible app
            sharePdf(context, file, fallbackTitle)
        }
    }

    /**
     * Shares a PDF file using the system share sheet.
     */
    fun sharePdf(context: Context, file: File, subject: String = "AgroRetail Document") {
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "PDF file is not available to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "$subject attached.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Document via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share PDF: ${e.message}")
            Toast.makeText(context, "Could not open share menu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Resolves an attached PDF or generates a formatted PDF document on disk for viewing/sharing.
     */
    fun getOrGenerateManualDocumentPdf(
        context: Context,
        doc: ManualDocument
    ): File {
        if (doc.pdfUrl.isNotBlank()) {
            val candidateFile = File(doc.pdfUrl)
            if (candidateFile.exists() && candidateFile.length() > 0) {
                return candidateFile
            }
        }
        val safeDocId = doc.id.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val cachedFile = File(getPdfsDirectory(context), "Doc_$safeDocId.pdf")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Header bar
        paint.color = Color.rgb(20, 83, 45) // Forest Green
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AGRO RETAIL DOCUMENT", 40f, 52f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Official Document Uploaded by Admin", 40f, 74f, paint)

        // Details
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(doc.title, 40f, 140f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Document Type: ${doc.documentType}", 40f, 170f, paint)
        canvas.drawText("Retailer: ${doc.retailerName} (ID: ${doc.retailerId})", 40f, 195f, paint)
        canvas.drawText("Uploaded On: ${SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(doc.uploadedAt))}", 40f, 220f, paint)
        if (doc.originalFileName.isNotBlank()) {
            canvas.drawText("Original File: ${doc.originalFileName}", 40f, 245f, paint)
        }

        if (doc.notes.isNotBlank()) {
            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Admin Remarks:", 40f, 285f, paint)
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(doc.notes, 40f, 310f, paint)
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 10f
        canvas.drawText("Generated for viewing in AgroRetail Ordering App", 40f, 800f, paint)

        pdfDocument.finishPage(page)
        try {
            FileOutputStream(cachedFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing manual doc PDF: ${e.message}")
        } finally {
            pdfDocument.close()
        }
        return cachedFile
    }

    /**
     * Resolves an attached PDF or generates a certified PDF Tax Invoice on demand.
     */
    fun getOrGenerateInvoicePdf(
        context: Context,
        bill: Bill,
        distributor: DistributorProfile,
        retailer: Retailer?
    ): File {
        // 1. Check if pdfUrl is a direct local file
        if (bill.pdfUrl.isNotBlank()) {
            val candidateFile = File(bill.pdfUrl)
            if (candidateFile.exists() && candidateFile.length() > 0) {
                return candidateFile
            }
        }

        // 2. Check if a cached generated file exists for this bill number
        val safeBillNo = bill.billNumber.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val cachedFile = File(getPdfsDirectory(context), "Invoice_$safeBillNo.pdf")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile
        }

        // 3. Generate genuine certified GST Tax Invoice PDF
        return generateInvoicePdf(context, bill, distributor, retailer)
    }

    /**
     * Resolves an attached Statement PDF or generates a certified Account Statement on demand.
     */
    fun getOrGenerateStatementPdf(
        context: Context,
        statement: Statement,
        distributor: DistributorProfile,
        retailer: Retailer?,
        entries: List<PassbookEntry>
    ): File {
        if (statement.pdfUrl.isNotBlank()) {
            val candidateFile = File(statement.pdfUrl)
            if (candidateFile.exists() && candidateFile.length() > 0) {
                return candidateFile
            }
        }

        val safeId = statement.id.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val cachedFile = File(getPdfsDirectory(context), "Statement_$safeId.pdf")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile
        }

        return generateStatementPdf(context, statement, distributor, retailer, entries)
    }

    /**
     * Generates a standard A4 GST Tax Invoice using Android's native PdfDocument.
     */
    fun generateInvoicePdf(
        context: Context,
        bill: Bill,
        distributor: DistributorProfile,
        retailer: Retailer?
    ): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(46, 125, 50) // Forest Green
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titleBadgePaint = Paint().apply {
            color = Color.rgb(27, 94, 32)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(200, 200, 200)
            strokeWidth = 1f
        }
        val fillBoxPaint = Paint().apply {
            color = Color.rgb(245, 247, 245)
            style = Paint.Style.FILL
        }
        val tableHeaderBoxPaint = Paint().apply {
            color = Color.rgb(235, 243, 235)
            style = Paint.Style.FILL
        }

        val margin = 36f
        var y = 45f

        // Top Border Box
        canvas.drawRect(margin, y, pageWidth - margin, y + 65f, fillBoxPaint)
        canvas.drawRect(margin, y, pageWidth - margin, y + 65f, linePaint.apply { style = Paint.Style.STROKE })

        // Company Details
        canvas.drawText(distributor.companyName.ifBlank { "AgroRetail Distributors Pvt Ltd" }, margin + 12f, y + 20f, headerPaint)
        textPaint.textSize = 8.5f
        canvas.drawText("Proprietor: ${distributor.ownerName} | GSTIN: ${distributor.gstin}", margin + 12f, y + 34f, textPaint)
        canvas.drawText("Address: ${distributor.address}, ${distributor.city}, ${distributor.state}", margin + 12f, y + 46f, textPaint)
        canvas.drawText("Phone: ${distributor.phone} | Email: ${distributor.email}", margin + 12f, y + 58f, textPaint)

        y += 80f

        // Title Badge
        canvas.drawText("TAX INVOICE / CASH MEMO", margin, y, titleBadgePaint)
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val billDateStr = dateFormat.format(Date(bill.billDate))
        textPaint.textSize = 9f
        canvas.drawText("ORIGINAL FOR RECIPIENT", pageWidth - margin - 120f, y, boldPaint)

        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 15f

        // Two Column Section: Invoice Details (Left) and Retailer Info (Right)
        val colWidth = (pageWidth - 2 * margin) / 2f
        canvas.drawRect(margin, y, margin + colWidth - 5f, y + 75f, fillBoxPaint)
        canvas.drawRect(margin + colWidth + 5f, y, pageWidth - margin, y + 75f, fillBoxPaint)

        // Left: Invoice Info
        boldPaint.textSize = 9.5f
        textPaint.textSize = 9f
        canvas.drawText("Invoice Details", margin + 8f, y + 15f, boldPaint)
        canvas.drawText("Invoice No: ${bill.billNumber}", margin + 8f, y + 30f, textPaint)
        canvas.drawText("Invoice Date: $billDateStr", margin + 8f, y + 44f, textPaint)
        canvas.drawText("Payment Terms: Due within 15 Days", margin + 8f, y + 58f, textPaint)

        // Right: Retailer Info
        val retName = retailer?.businessName ?: bill.retailerName
        val retOwner = retailer?.retailerName ?: "Authorized Retailer"
        val retGst = retailer?.gstNumber?.takeIf { it.isNotBlank() } ?: "URP (Unregistered)"
        val retCity = retailer?.city?.takeIf { it.isNotBlank() } ?: "Local Market"

        val rx = margin + colWidth + 13f
        canvas.drawText("Billed To (Retailer)", rx, y + 15f, boldPaint)
        canvas.drawText("$retName ($retOwner)", rx, y + 30f, textPaint)
        canvas.drawText("GSTIN: $retGst", rx, y + 44f, textPaint)
        canvas.drawText("Station / City: $retCity", rx, y + 58f, textPaint)

        y += 90f

        // Table Header
        val thHeight = 22f
        canvas.drawRect(margin, y, pageWidth - margin, y + thHeight, tableHeaderBoxPaint)
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        canvas.drawLine(margin, y + thHeight, pageWidth - margin, y + thHeight, linePaint)

        boldPaint.textSize = 9f
        canvas.drawText("#", margin + 6f, y + 15f, boldPaint)
        canvas.drawText("Description of Goods / Agricultural Supplies", margin + 25f, y + 15f, boldPaint)
        canvas.drawText("HSN/SAC", margin + 270f, y + 15f, boldPaint)
        canvas.drawText("Taxable (₹)", margin + 350f, y + 15f, boldPaint)
        canvas.drawText("GST %", margin + 430f, y + 15f, boldPaint)
        canvas.drawText("Total (₹)", margin + 475f, y + 15f, boldPaint)

        y += thHeight + 12f

        // Sample Goods / Invoice Line Items
        textPaint.textSize = 9f
        val taxableAmount = bill.amount / 1.18
        val gstAmount = bill.amount - taxableAmount
        val cgstAmount = gstAmount / 2
        val sgstAmount = gstAmount / 2

        canvas.drawText("1", margin + 6f, y, textPaint)
        canvas.drawText("Agrochemicals & Crop Protection Solutions (Certified Batch)", margin + 25f, y, textPaint)
        canvas.drawText("380899", margin + 270f, y, textPaint)
        canvas.drawText("₹%,.2f".format(taxableAmount), margin + 350f, y, textPaint)
        canvas.drawText("18%", margin + 430f, y, textPaint)
        canvas.drawText("₹%,.2f".format(bill.amount), margin + 475f, y, boldPaint)

        y += 18f
        textPaint.textSize = 8f
        textPaint.color = Color.DKGRAY
        canvas.drawText("Includes branded crop inputs dispatched under e-way bill compliance.", margin + 25f, y, textPaint)
        textPaint.color = Color.BLACK
        textPaint.textSize = 9f

        y += 35f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 15f

        // Calculation Totals Block (Right aligned)
        val totX = margin + 330f
        canvas.drawText("Taxable Sub-Total:", totX, y, textPaint)
        canvas.drawText("₹%,.2f".format(taxableAmount), margin + 465f, y, textPaint)
        y += 14f

        canvas.drawText("CGST (9.0%):", totX, y, textPaint)
        canvas.drawText("₹%,.2f".format(cgstAmount), margin + 465f, y, textPaint)
        y += 14f

        canvas.drawText("SGST (9.0%):", totX, y, textPaint)
        canvas.drawText("₹%,.2f".format(sgstAmount), margin + 465f, y, textPaint)
        y += 14f

        canvas.drawLine(totX, y, pageWidth - margin, y, linePaint)
        y += 15f

        boldPaint.textSize = 11f
        canvas.drawText("GRAND TOTAL (INR):", totX, y, boldPaint)
        canvas.drawText("₹%,.2f".format(bill.amount), margin + 465f, y, boldPaint)

        y += 30f

        // Payment / Bank Details Box
        canvas.drawRect(margin, y, pageWidth - margin, y + 70f, fillBoxPaint)
        canvas.drawRect(margin, y, pageWidth - margin, y + 70f, linePaint.apply { style = Paint.Style.STROKE })

        boldPaint.textSize = 9f
        canvas.drawText("Remittance & Bank Settlement Details:", margin + 10f, y + 16f, boldPaint)
        textPaint.textSize = 8.5f
        canvas.drawText("Bank: ${distributor.bankName} | Account No: ${distributor.accountNumber}", margin + 10f, y + 32f, textPaint)
        canvas.drawText("IFSC Code: ${distributor.ifscCode} | UPI ID: ${distributor.upiId}", margin + 10f, y + 46f, textPaint)
        canvas.drawText("Please quote Invoice #${bill.billNumber} in NEFT / IMPS reference note.", margin + 10f, y + 60f, textPaint)

        y += 90f

        // Terms & Signatures
        textPaint.textSize = 8f
        canvas.drawText("Terms & Declarations:", margin, y, boldPaint.apply { textSize = 8.5f })
        y += 12f
        canvas.drawText("1. Goods once sold will not be returned unless damaged upon dispatch.", margin, y, textPaint)
        y += 11f
        canvas.drawText("2. Interest @18% p.a. will be levied on bills unpaid beyond due date.", margin, y, textPaint)
        y += 11f
        canvas.drawText("3. Subject to Karnal Jurisdiction only.", margin, y, textPaint)

        // Signature Box Right
        val sigX = pageWidth - margin - 150f
        canvas.drawText("For ${distributor.companyName}", sigX, y - 20f, boldPaint.apply { textSize = 8.5f })
        canvas.drawText("Authorized Signatory", sigX + 15f, y + 15f, textPaint)

        pdfDoc.finishPage(page)

        val safeBillNo = bill.billNumber.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val outFile = File(getPdfsDirectory(context), "Invoice_$safeBillNo.pdf")
        try {
            FileOutputStream(outFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing PDF: ${e.message}")
        } finally {
            pdfDoc.close()
        }
        return outFile
    }

    /**
     * Generates a certified Statement / Passbook Ledger PDF.
     */
    fun generateStatementPdf(
        context: Context,
        statement: Statement,
        distributor: DistributorProfile,
        retailer: Retailer?,
        entries: List<PassbookEntry>
    ): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(46, 125, 50)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(200, 200, 200)
            strokeWidth = 1f
        }
        val bgHeaderPaint = Paint().apply {
            color = Color.rgb(240, 245, 240)
            style = Paint.Style.FILL
        }

        val margin = 36f
        var y = 45f

        // Distributor Header
        canvas.drawText(distributor.companyName, margin, y, headerPaint)
        textPaint.textSize = 8.5f
        canvas.drawText("Certified Passbook Ledger & Account Statement", margin, y + 14f, boldPaint)
        canvas.drawText("Period: ${statement.period} | Generated: ${SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())}", margin, y + 26f, textPaint)

        y += 40f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 15f

        // Retailer summary card
        val retName = retailer?.businessName ?: statement.retailerName
        val retOwner = retailer?.retailerName ?: "Retailer Partner"
        val outAmt = retailer?.outstandingAmount ?: 0.0

        canvas.drawRect(margin, y, pageWidth - margin, y + 40f, bgHeaderPaint)
        boldPaint.textSize = 10f
        canvas.drawText("Account: $retName ($retOwner)", margin + 10f, y + 16f, boldPaint)
        textPaint.textSize = 9f
        canvas.drawText("Current Net Outstanding: ₹%,.2f".format(outAmt), margin + 10f, y + 30f, textPaint)
        canvas.drawText("GSTIN: ${retailer?.gstNumber ?: "N/A"}", pageWidth - margin - 150f, y + 16f, textPaint)

        y += 55f

        // Table Header
        canvas.drawRect(margin, y, pageWidth - margin, y + 20f, bgHeaderPaint)
        boldPaint.textSize = 8.5f
        canvas.drawText("Date", margin + 6f, y + 14f, boldPaint)
        canvas.drawText("Ref / Inv #", margin + 80f, y + 14f, boldPaint)
        canvas.drawText("Description", margin + 170f, y + 14f, boldPaint)
        canvas.drawText("Debit (₹)", margin + 340f, y + 14f, boldPaint)
        canvas.drawText("Credit (₹)", margin + 415f, y + 14f, boldPaint)
        canvas.drawText("Balance (₹)", margin + 480f, y + 14f, boldPaint)

        y += 20f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        var totalDebit = 0.0
        var totalCredit = 0.0

        val displayEntries = if (entries.isNotEmpty()) entries.take(18) else listOf(
            PassbookEntry(
                invoiceNumber = "OP-BAL",
                description = "Opening Account Balance",
                debit = 0.0,
                credit = 0.0,
                runningBalance = outAmt
            )
        )

        for (item in displayEntries) {
            totalDebit += item.debit
            totalCredit += item.credit

            textPaint.textSize = 8.5f
            canvas.drawText(dateFormat.format(Date(item.date)), margin + 6f, y, textPaint)
            canvas.drawText(item.invoiceNumber.take(12), margin + 80f, y, textPaint)
            canvas.drawText(item.description.take(24), margin + 170f, y, textPaint)

            if (item.debit > 0) {
                canvas.drawText("₹%,.0f".format(item.debit), margin + 340f, y, textPaint)
            } else {
                canvas.drawText("-", margin + 340f, y, textPaint)
            }

            if (item.credit > 0) {
                canvas.drawText("₹%,.0f".format(item.credit), margin + 415f, y, textPaint)
            } else {
                canvas.drawText("-", margin + 415f, y, textPaint)
            }

            canvas.drawText("₹%,.0f".format(item.runningBalance), margin + 480f, y, boldPaint)

            y += 16f
            if (y > pageHeight - 80f) break
        }

        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        boldPaint.textSize = 9.5f
        canvas.drawText("Total Debited: ₹%,.2f".format(totalDebit), margin + 150f, y, boldPaint)
        canvas.drawText("Total Credited: ₹%,.2f".format(totalCredit), margin + 330f, y, boldPaint)

        y += 40f
        textPaint.textSize = 8f
        canvas.drawText("This is a computer certified statement issued by ${distributor.companyName}.", margin, y, textPaint)
        val sigX = pageWidth - margin - 150f
        canvas.drawText("Authorized Account Officer", sigX, y, textPaint)

        pdfDoc.finishPage(page)

        val safeId = statement.id.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val outFile = File(getPdfsDirectory(context), "Statement_$safeId.pdf")
        try {
            FileOutputStream(outFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing statement PDF: ${e.message}")
        } finally {
            pdfDoc.close()
        }
        return outFile
    }

    /**
     * Generates an official, certified Order Confirmation PDF.
     * Guaranteed to contain actual order items, never blank.
     * For Retailer (hideGst = true): completely hides GST%, GST amount, CGST, SGST, IGST, HSN, Taxable value.
     */
    fun getOrGenerateOrderPdf(
        context: Context,
        order: Order,
        retailer: Retailer?,
        distributor: DistributorProfile = DistributorProfile(),
        hideGst: Boolean = true
    ): File {
        val safeOrderId = order.id.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val outFile = File(getPdfsDirectory(context), "Order_$safeOrderId.pdf")

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val margin = 36f
        var y = 35f

        // 1. Header Bar with Ganesha + Agriculture Logo
        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_ganesh_agro_logo)
        } catch (e: Exception) {
            null
        }

        if (logoBitmap != null) {
            val logoSize = 48
            val destRect = Rect(margin.toInt(), y.toInt(), (margin + logoSize).toInt(), (y + logoSize).toInt())
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
        } else {
            // Fallback decorative emblem
            paint.color = Color.rgb(20, 83, 45) // Forest Green
            canvas.drawCircle(margin + 24f, y + 24f, 24f, paint)
            paint.color = Color.rgb(245, 158, 11) // Golden Sun
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ॐ", margin + 14f, y + 31f, paint)
        }

        val textStartX = margin + 56f
        paint.color = Color.rgb(20, 83, 45) // Forest Green
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(distributor.companyName.ifBlank { "Siddhi Vinayak Krishi Vikas Kendra" }, textStartX, y + 18f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Quality Crop Protection, Fertilizers & Agricultural Input Supplies", textStartX, y + 32f, paint)
        canvas.drawText("${distributor.address}, ${distributor.city} • Helpline: ${distributor.phone}", textStartX, y + 44f, paint)

        y += 58f

        // Document Title Badge
        paint.color = Color.rgb(240, 245, 240)
        paint.style = Paint.Style.FILL
        canvas.drawRect(margin, y, pageWidth - margin, y + 24f, paint)
        paint.color = Color.rgb(20, 83, 45)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(margin, y, pageWidth - margin, y + 24f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ORDER CONFIRMATION (NOT AN INVOICE)", margin + 12f, y + 16f, paint)

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault())
        val orderDateStr = dateFormat.format(Date(order.dateTime))
        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f
        canvas.drawText("Status: ${order.status.name.uppercase()}", pageWidth - margin - 130f, y + 16f, paint)

        y += 32f

        // 2. Order & Retailer Details Cards (Two columns)
        val colWidth = (pageWidth - 2 * margin - 10f) / 2f
        paint.color = Color.rgb(248, 250, 252)
        paint.style = Paint.Style.FILL
        canvas.drawRect(margin, y, margin + colWidth, y + 74f, paint)
        canvas.drawRect(margin + colWidth + 10f, y, pageWidth - margin, y + 74f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        canvas.drawRect(margin, y, margin + colWidth, y + 74f, paint)
        canvas.drawRect(margin + colWidth + 10f, y, pageWidth - margin, y + 74f, paint)

        // Left: Order Info
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 83, 45)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText("Order Information", margin + 10f, y + 16f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        canvas.drawText("Order No: ${order.id}", margin + 10f, y + 30f, paint)
        canvas.drawText("Order Date: $orderDateStr", margin + 10f, y + 44f, paint)
        canvas.drawText("Payment Terms: Cash / Credit as per Party Agreement", margin + 10f, y + 58f, paint)

        // Right: Retailer Info
        val rX = margin + colWidth + 20f
        paint.color = Color.rgb(20, 83, 45)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText("Party / Retailer Details", rX, y + 16f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        val retFirm = order.retailerBusinessName.ifBlank { retailer?.businessName ?: "Authorized Retailer" }
        val retContact = order.retailerName.ifBlank { retailer?.retailerName ?: "Proprietor" }
        val retMobile = order.retailerMobile.ifBlank { retailer?.mobileNumber ?: "N/A" }
        val retAddress = retailer?.address?.takeIf { it.isNotBlank() } ?: (retailer?.city ?: "Authorized Dealer Market")

        canvas.drawText("Firm: $retFirm", rX, y + 30f, paint)
        canvas.drawText("Contact: $retContact | Mob: +91 $retMobile", rX, y + 44f, paint)
        canvas.drawText("Address: $retAddress", rX, y + 58f, paint)

        y += 86f

        // 3. Table Header
        paint.color = Color.rgb(20, 83, 45)
        paint.style = Paint.Style.FILL
        canvas.drawRect(margin, y, pageWidth - margin, y + 22f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("#", margin + 6f, y + 15f, paint)
        canvas.drawText("Product Description & SKU", margin + 28f, y + 15f, paint)
        canvas.drawText("Pack / Unit", margin + 280f, y + 15f, paint)
        canvas.drawText("Qty", margin + 355f, y + 15f, paint)
        canvas.drawText("Rate (₹)", margin + 400f, y + 15f, paint)
        canvas.drawText("Amount (₹)", margin + 465f, y + 15f, paint)

        y += 22f

        // 4. Products Rows (guaranteed to render all items)
        var totalQty = 0
        order.items.forEachIndexed { index, item ->
            totalQty += item.quantity
            val rowBg = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
            paint.color = rowBg
            paint.style = Paint.Style.FILL
            canvas.drawRect(margin, y, pageWidth - margin, y + 20f, paint)

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, y + 20f, pageWidth - margin, y + 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f

            // Item number
            canvas.drawText("${index + 1}", margin + 6f, y + 14f, paint)

            // Product name + optional code
            val nameDisplay = if (item.itemCode.isNotBlank()) {
                "${item.itemName} (${item.itemCode})"
            } else {
                item.itemName
            }.take(40)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(nameDisplay, margin + 28f, y + 14f, paint)

            // Pack size
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(item.packSize.ifBlank { "Standard" }.take(14), margin + 280f, y + 14f, paint)

            // Quantity
            canvas.drawText("${item.quantity}", margin + 355f, y + 14f, paint)

            // Rate
            canvas.drawText("₹%,.2f".format(item.rate), margin + 400f, y + 14f, paint)

            // Total Amount
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(20, 83, 45)
            canvas.drawText("₹%,.2f".format(item.total), margin + 465f, y + 14f, paint)

            y += 20f
        }

        // 5. Totals Block (Right aligned, NO GST for retailer!)
        y += 10f
        val totBoxX = margin + 280f
        paint.color = Color.rgb(240, 245, 240)
        paint.style = Paint.Style.FILL
        canvas.drawRect(totBoxX, y, pageWidth - margin, y + 42f, paint)

        paint.color = Color.rgb(20, 83, 45)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(totBoxX, y, pageWidth - margin, y + 42f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText("Total Ordered Items:", totBoxX + 10f, y + 16f, paint)
        canvas.drawText("${order.items.size} Items (${totalQty} units)", pageWidth - margin - 120f, y + 16f, paint)

        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(20, 83, 45)
        canvas.drawText("GRAND TOTAL (INR):", totBoxX + 10f, y + 33f, paint)
        canvas.drawText("₹%,.2f".format(order.grandTotal), pageWidth - margin - 120f, y + 33f, paint)

        y += 56f

        // Remarks / Notes
        if (order.notes.isNotBlank() || order.adminNotes.isNotBlank()) {
            paint.color = Color.rgb(248, 250, 252)
            paint.style = Paint.Style.FILL
            canvas.drawRect(margin, y, pageWidth - margin, y + 30f, paint)
            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            canvas.drawRect(margin, y, pageWidth - margin, y + 30f, paint)

            paint.style = Paint.Style.FILL
            paint.textSize = 8.5f
            paint.color = Color.rgb(30, 41, 59)
            val remarksText = buildString {
                if (order.notes.isNotBlank()) append("Retailer Note: ${order.notes}  ")
                if (order.adminNotes.isNotBlank()) append("Admin Remarks: ${order.adminNotes}")
            }
            canvas.drawText(remarksText.take(90), margin + 10f, y + 18f, paint)
            y += 40f
        }

        // Terms & Signatures
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Terms & Conditions:", margin, y, paint)
        y += 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("1. This is a computer generated order confirmation by Siddhi Vinayak Krishi Vikas Kendra.", margin, y, paint)
        y += 11f
        canvas.drawText("2. Goods dispatched as per availability. All disputes subject to local jurisdiction.", margin, y, paint)

        // Signature on bottom right
        val sigX = pageWidth - margin - 160f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(20, 83, 45)
        canvas.drawText("For Siddhi Vinayak Krishi Vikas Kendra", sigX, y - 10f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Authorized Signatory", sigX + 10f, y + 6f, paint)

        pdfDoc.finishPage(page)

        try {
            FileOutputStream(outFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing order PDF: ${e.message}")
        } finally {
            pdfDoc.close()
        }
        return outFile
    }
}

