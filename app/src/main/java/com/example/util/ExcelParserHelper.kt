package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

object ExcelParserHelper {

    private const val TAG = "ExcelParserHelper"

    data class ParsedSheet(
        val fileName: String,
        val headers: List<String>,
        val rows: List<Map<String, String>>
    )

    /**
     * Parses a Uri pointing to .xlsx, .xls, .csv, or .tsv file.
     */
    fun parseDocument(context: Context, uri: Uri, fileName: String): ParsedSheet {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading uri: ${e.message}")
            ByteArray(0)
        }

        if (bytes.isEmpty()) {
            return ParsedSheet(fileName, emptyList(), emptyList())
        }

        return parseBytes(bytes, fileName)
    }

    /**
     * Parses byte array based on content/extension.
     */
    fun parseBytes(bytes: ByteArray, fileName: String): ParsedSheet {
        val lower = fileName.lowercase()
        // Check magic bytes for ZIP (PK.. - standard for .xlsx)
        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

        if (isZip || lower.endsWith(".xlsx")) {
            val result = parseXlsxZip(bytes, fileName)
            if (result.rows.isNotEmpty()) return result
        }

        // Check if XML Spreadsheet (BUSY often exports .xls as XML SpreadsheetML)
        val textSnippet = String(bytes.take(500).toByteArray(), StandardCharsets.UTF_8).lowercase()
        if (textSnippet.contains("<?xml") || textSnippet.contains("<workbook") || textSnippet.contains("<table")) {
            val result = parseXmlSpreadsheet(bytes, fileName)
            if (result.rows.isNotEmpty()) return result
        }

        // Default to CSV / Delimited Text Parser
        return parseDelimitedText(bytes, fileName)
    }

    /**
     * Parses native OpenXML .xlsx files without third party libraries.
     */
    private fun parseXlsxZip(bytes: ByteArray, fileName: String): ParsedSheet {
        return try {
            val sharedStrings = mutableListOf<String>()
            var sheetXmlBytes: ByteArray? = null

            // First pass: extract sharedStrings and sheet1
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                            sharedStrings.addAll(parseSharedStrings(zis.readBytes()))
                        }
                        entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                        (entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) && sheetXmlBytes == null) -> {
                            sheetXmlBytes = zis.readBytes()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (sheetXmlBytes != null) {
                parseSheetXml(sheetXmlBytes!!, sharedStrings, fileName)
            } else {
                parseDelimitedText(bytes, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing .xlsx: ${e.message}", e)
            parseDelimitedText(bytes, fileName)
        }
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            var eventType = parser.eventType
            var inT = false
            val currentText = java.lang.StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inT = false
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            list.add(currentText.toString())
                            currentText.setLength(0)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shared strings: ${e.message}")
        }
        return list
    }

    private fun parseSheetXml(xmlBytes: ByteArray, sharedStrings: List<String>, fileName: String): ParsedSheet {
        val rowsList = mutableListOf<List<String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            var eventType = parser.eventType

            var currentRow = mutableListOf<String>()
            var cellType = ""
            var cellValue = java.lang.StringBuilder()
            var inV = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "row" -> currentRow = mutableListOf()
                            "c" -> {
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                cellValue.setLength(0)
                            }
                            "v", "t" -> inV = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV) {
                            cellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "v", "t" -> inV = false
                            "c" -> {
                                val raw = cellValue.toString().trim()
                                val resolved = if (cellType == "s") {
                                    val index = raw.toIntOrNull()
                                    if (index != null && index in sharedStrings.indices) {
                                        sharedStrings[index]
                                    } else raw
                                } else raw
                                currentRow.add(resolved)
                            }
                            "row" -> {
                                if (currentRow.any { it.isNotBlank() }) {
                                    rowsList.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sheet xml: ${e.message}")
        }

        if (rowsList.isEmpty()) {
            return ParsedSheet(fileName, emptyList(), emptyList())
        }

        val rawHeaders = rowsList.first()
        val headers = rawHeaders.mapIndexed { idx, h ->
            if (h.isNotBlank()) h.trim() else "Column_${idx + 1}"
        }

        val mappedRows = mutableListOf<Map<String, String>>()
        for (i in 1 until rowsList.size) {
            val row = rowsList[i]
            val map = mutableMapOf<String, String>()
            for (j in headers.indices) {
                val headerName = headers[j]
                val valStr = if (j < row.size) row[j].trim() else ""
                map[headerName] = valStr
            }
            if (map.values.any { it.isNotBlank() }) {
                mappedRows.add(map)
            }
        }

        return ParsedSheet(fileName, headers, mappedRows)
    }

    /**
     * Parses Microsoft XML Spreadsheet format (commonly exported by BUSY accounting).
     */
    private fun parseXmlSpreadsheet(bytes: ByteArray, fileName: String): ParsedSheet {
        val rowsList = mutableListOf<List<String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
            var eventType = parser.eventType

            var currentRow = mutableListOf<String>()
            var cellValue = java.lang.StringBuilder()
            var inData = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "row" -> currentRow = mutableListOf()
                            "data", "td", "th" -> {
                                inData = true
                                cellValue.setLength(0)
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inData) {
                            cellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "data", "td", "th" -> {
                                inData = false
                                currentRow.add(cellValue.toString().trim())
                            }
                            "row", "tr" -> {
                                if (currentRow.any { it.isNotBlank() }) {
                                    rowsList.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in XML spreadsheet parse: ${e.message}")
        }

        if (rowsList.isEmpty()) {
            return parseDelimitedText(bytes, fileName)
        }

        val headers = rowsList.first().mapIndexed { idx, h ->
            if (h.isNotBlank()) h.trim() else "Col_${idx + 1}"
        }

        val mappedRows = mutableListOf<Map<String, String>>()
        for (i in 1 until rowsList.size) {
            val row = rowsList[i]
            val map = mutableMapOf<String, String>()
            for (j in headers.indices) {
                map[headers[j]] = if (j < row.size) row[j].trim() else ""
            }
            if (map.values.any { it.isNotBlank() }) {
                mappedRows.add(map)
            }
        }

        return ParsedSheet(fileName, headers, mappedRows)
    }

    /**
     * Parses CSV, TSV, or Semicolon delimited text files with RFC4180 quotation handling.
     */
    fun parseDelimitedText(bytes: ByteArray, fileName: String): ParsedSheet {
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
        val lines = reader.readLines()
        if (lines.isEmpty()) {
            return ParsedSheet(fileName, emptyList(), emptyList())
        }

        // Determine delimiter based on first non-empty line
        val firstLine = lines.firstOrNull { it.isNotBlank() } ?: ""
        val delimiter = when {
            firstLine.count { it == '\t' } >= 2 -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }

        val rowsList = mutableListOf<List<String>>()
        for (line in lines) {
            if (line.isBlank()) continue
            rowsList.add(parseCsvLine(line, delimiter))
        }

        if (rowsList.isEmpty()) {
            return ParsedSheet(fileName, emptyList(), emptyList())
        }

        val headers = rowsList.first().mapIndexed { idx, h ->
            if (h.isNotBlank()) h.trim() else "Column_${idx + 1}"
        }

        val mappedRows = mutableListOf<Map<String, String>>()
        for (i in 1 until rowsList.size) {
            val row = rowsList[i]
            val map = mutableMapOf<String, String>()
            for (j in headers.indices) {
                map[headers[j]] = if (j < row.size) row[j].trim() else ""
            }
            if (map.values.any { it.isNotBlank() }) {
                mappedRows.add(map)
            }
        }

        return ParsedSheet(fileName, headers, mappedRows)
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++ // Skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * Smart Header Auto-detection for Retailer imports.
     * Matches standard names, BUSY export headers, Tally headers, or generic formats.
     */
    fun autoMapRetailerColumns(headers: List<String>): Map<String, String> {
        val mapping = mutableMapOf<String, String>()

        fun findHeader(vararg patterns: String): String {
            for (p in patterns) {
                val cleanPattern = p.lowercase().trim()
                val match = headers.find { h ->
                    val cleanH = h.lowercase().replace("_", " ").trim()
                    cleanH == cleanPattern || cleanH.contains(cleanPattern)
                }
                if (match != null) return match
            }
            return ""
        }

        mapping["retailerName"] = findHeader("retailer name", "party name", "customer name", "account name", "firm name", "shop name", "name", "ledger name", "party")
        mapping["businessName"] = findHeader("firm name", "shop name", "business name", "trade name", "company name", "store name", "party name", "customer name")
        mapping["partyCode"] = findHeader("party code", "customer code", "account code", "code", "ledger code", "party no", "customer no")
        mapping["mobileNumber"] = findHeader("mobile", "mobile no", "mobile number", "phone", "contact", "contact no", "telephone", "cell", "phone no")
        mapping["whatsappNumber"] = findHeader("whatsapp", "wa number", "whatsapp no", "whatsapp number", "wa mobile")
        mapping["address"] = findHeader("address", "party address", "billing address", "street", "location", "area", "addr")
        mapping["city"] = findHeader("city", "town", "district", "place", "station")
        mapping["state"] = findHeader("state", "province", "region")
        mapping["pincode"] = findHeader("pincode", "pin code", "postal code", "zip", "zip code", "pin")
        mapping["gstNumber"] = findHeader("gstin", "gst no", "gst number", "gstin/uin", "gst", "tax no")
        mapping["email"] = findHeader("email", "e-mail", "email id", "mail", "email address")
        mapping["category"] = findHeader("category", "party category", "group", "type", "class", "grade")
        mapping["creditLimit"] = findHeader("credit limit", "limit", "cr limit", "max credit")
        mapping["openingBalance"] = findHeader("opening balance", "op balance", "op bal", "balance", "opening bal", "due amount")

        return mapping
    }

    /**
     * Smart Header Auto-detection for Product imports.
     */
    fun autoMapProductColumns(headers: List<String>): Map<String, String> {
        val mapping = mutableMapOf<String, String>()

        fun findHeader(vararg patterns: String): String {
            for (p in patterns) {
                val cleanPattern = p.lowercase().trim()
                val match = headers.find { h ->
                    val cleanH = h.lowercase().replace("_", " ").trim()
                    cleanH == cleanPattern || cleanH.contains(cleanPattern)
                }
                if (match != null) return match
            }
            return ""
        }

        mapping["itemName"] = findHeader("item name", "product name", "item description", "particulars", "description", "item", "product", "material name", "commodity")
        mapping["itemCode"] = findHeader("item code", "item no", "product code", "material code", "code", "part no", "sku")
        mapping["alias"] = findHeader("alias", "print name", "short name", "display name", "alternate name")
        mapping["company"] = findHeader("company", "manufacturer", "brand", "make", "mfg by", "company name", "supplier")
        mapping["category"] = findHeader("item group", "category", "group", "item type", "type", "classification")
        mapping["subCategory"] = findHeader("sub category", "sub group", "sub-group", "subgroup")
        mapping["unit"] = findHeader("unit", "uom", "measurement", "unit of measure", "measure")
        mapping["packSize"] = findHeader("pack size", "packing size", "pack", "size")
        mapping["packing"] = findHeader("packing", "case pack", "box pack", "master pack")
        mapping["purchaseRate"] = findHeader("purchase rate", "cost price", "purchase price", "cost", "buying rate")
        mapping["sellingRate"] = findHeader("selling rate", "sale rate", "sales rate", "rate", "dealer price", "wholesale rate", "price")
        mapping["mrp"] = findHeader("mrp", "max retail price", "maximum retail price", "m.r.p")
        mapping["gstPercent"] = findHeader("gst rate", "gst percent", "gst %", "tax rate", "gst", "tax")
        mapping["hsnCode"] = findHeader("hsn code", "hsn", "hsn/sac", "sac")
        mapping["barcode"] = findHeader("barcode", "upc", "ean", "qr code")
        mapping["openingStock"] = findHeader("opening stock", "op stock", "op. stock", "initial stock", "opening qty")
        mapping["currentStock"] = findHeader("current stock", "closing stock", "stock", "quantity", "qty", "balance qty", "available qty", "stock qty")
        mapping["godown"] = findHeader("godown", "warehouse", "store", "location", "rack")
        mapping["batch"] = findHeader("batch", "batch no", "lot", "lot no")
        mapping["description"] = findHeader("description", "notes", "remarks", "details")

        return mapping
    }

    /**
     * Generates a reference sample CSV for Retailers.
     * Note: App supports BUSY exports and any column layout flexibly.
     */
    fun generateRetailerSampleCsv(): String {
        return """
            Party Code,Party Name,Firm Name,Mobile,WhatsApp Number,Address,City,State,Pincode,GSTIN,Email,Category,Credit Limit,Opening Balance
            RET-001,Kisan Beej Bhandar,Kisan Beej Bhandar,9812345001,9812345001,"Shop 12, Main Mandi",Karnal,Haryana,132001,06AAAAA1111A1Z1,kisan@gmail.com,GOLD,200000,25000
            RET-002,Sharma Krishi Sewa Kendra,Sharma Krishi Sewa Kendra,9812345002,9812345002,"Near Bus Stand",Panipat,Haryana,132103,06BBBBB2222B1Z2,sharma@gmail.com,PLATINUM,500000,45000
            RET-003,Bharat Agro Agency,Bharat Agro Agency,9812345003,9812345003,"Railway Road",Kaithal,Haryana,136027,06CCCCC3333C1Z3,bharat@gmail.com,SILVER,150000,0
            RET-004,Jai Kisan Fertilizer Store,Jai Kisan Fertilizer Store,9812345004,9812345004,"Old Grain Market",Kurukshetra,Haryana,136118,06DDDDD4444D1Z4,jaikisan@gmail.com,GOLD,300000,12000
        """.trimIndent()
    }

    /**
     * Generates a reference sample CSV for Products.
     * Note: App supports BUSY exports and any column layout flexibly.
     */
    fun generateProductSampleCsv(): String {
        return """
            Item Code,Item Name,Company,Category,Unit,Pack Size,Packing,Purchase Rate,Selling Rate,MRP,HSN Code,Opening Stock,Current Stock,Godown,Batch
            BAY-001,Confidor 100ml,Bayer CropScience,Insecticides,PCS,100 ml,Box of 20,290,320,385,38089190,50,150,Godown-A,B24-09
            SYN-002,Amistar Top 200ml,Syngenta India,Fungicides,PCS,200 ml,Box of 10,780,850,1020,38089290,30,80,Godown-A,SYN-89
            UPL-003,Saaf Fungicide 500g,UPL Limited,Fungicides,BAG,500 g,Box of 20,310,345,420,38089290,100,240,Godown-B,UPL-44
            IFF-004,Nano Urea 500ml,IFFCO,Fertilizers,BOTTLE,500 ml,Box of 24,195,215,225,31021000,200,500,Godown-C,IFF-01
            PII-005,Nominee Gold 100ml,PI Industries,Herbicides,PCS,100 ml,Box of 10,610,670,795,38089390,40,95,Godown-B,PI-77
        """.trimIndent()
    }
}
