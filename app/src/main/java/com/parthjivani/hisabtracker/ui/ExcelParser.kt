package com.parthjivani.hisabtracker.ui

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

data class ParsedExcelSheet(
    val name: String,
    val grid: List<List<String>>
)

data class ExtractedRow(
    val originalRowIndex: Int,
    val rawDate: String,
    val rawDesc: String,
    val rawAmount: String,
    val rawCategory: String
)

data class ExtractedTable(
    val sheetName: String,
    val title: String,
    val headerRowIndex: Int,
    val dateColumnIndex: Int,
    val descColumnIndex: Int,
    val amountColumnIndex: Int,
    val categoryColumnIndex: Int,
    val rows: List<ExtractedRow>
)

object ExcelParser {

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var currentItem = StringBuilder()
            var inSi = false
            var inT = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "si") {
                            inSi = true
                            currentItem.clear()
                        } else if (parser.name == "t" && inSi) {
                            inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            currentItem.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            inT = false
                        } else if (parser.name == "si") {
                            strings.add(currentItem.toString())
                            inSi = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strings
    }

    private fun parseWorkbookSheetNames(inputStream: InputStream): List<String> {
        val sheetNames = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: ""
                    if (name.isNotEmpty()) {
                        sheetNames.add(name)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sheetNames
    }

    private fun colNameToIndex(name: String): Int {
        var index = 0
        for (i in 0 until name.length) {
            index = index * 26 + (name[i].uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }

    private fun parseCellRef(ref: String): Pair<Int, Int> {
        var colName = ""
        var rowStr = ""
        for (char in ref) {
            if (char.isLetter()) {
                colName += char
            } else if (char.isDigit()) {
                rowStr += char
            }
        }
        val colIdx = colNameToIndex(colName.ifEmpty { "A" })
        val rowIdx = (rowStr.toIntOrNull() ?: 1) - 1
        return Pair(rowIdx, colIdx)
    }

    private fun parseWorksheet(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rowsMap = mutableMapOf<Int, MutableMap<Int, String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            
            var eventType = parser.eventType
            var currentCellRef = ""
            var currentCellType = ""
            var isVal = false
            var inlineStrVal = StringBuilder()
            var isInlineStr = false
            var currentVal = StringBuilder()
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "c") {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentVal.clear()
                            isVal = false
                            isInlineStr = false
                            inlineStrVal.clear()
                        } else if (parser.name == "v") {
                            isVal = true
                            currentVal.clear()
                        } else if (parser.name == "t") {
                            isInlineStr = true
                            inlineStrVal.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (isVal) {
                            currentVal.append(parser.text)
                        } else if (isInlineStr) {
                            inlineStrVal.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "v") {
                            isVal = false
                        } else if (parser.name == "t") {
                            isInlineStr = false
                        } else if (parser.name == "c") {
                            var cellValue = ""
                            if (currentCellType == "s") {
                                val strIndex = currentVal.toString().toIntOrNull()
                                if (strIndex != null && strIndex in sharedStrings.indices) {
                                    cellValue = sharedStrings[strIndex]
                                }
                            } else if (currentCellType == "inlineStr") {
                                cellValue = inlineStrVal.toString()
                            } else {
                                cellValue = currentVal.toString()
                            }
                            
                            if (currentCellRef.isNotEmpty()) {
                                val (rowIdx, colIdx) = parseCellRef(currentCellRef)
                                val rowMap = rowsMap.getOrPut(rowIdx) { mutableMapOf() }
                                rowMap[colIdx] = cellValue
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (rowsMap.isEmpty()) return emptyList()
        val maxRow = rowsMap.keys.maxOrNull() ?: 0
        val grid = MutableList(maxRow + 1) { mutableListOf<String>() }
        for (r in 0..maxRow) {
            val rowMap = rowsMap[r] ?: emptyMap()
            val maxCol = if (rowMap.isNotEmpty()) rowMap.keys.maxOrNull() ?: 0 else 0
            val rowList = MutableList(maxCol + 1) { "" }
            for ((c, v) in rowMap) {
                rowList[c] = v
            }
            grid[r] = rowList
        }
        return grid
    }

    fun parseExcelZip(fileBytes: ByteArray): List<ParsedExcelSheet> {
        val result = mutableListOf<ParsedExcelSheet>()
        try {
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(fileBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml" || name == "xl/workbook.xml" || name.startsWith("xl/worksheets/")) {
                        entries[name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            val sharedStringsStr = entries["xl/sharedStrings.xml"]
            val sharedStrings = if (sharedStringsStr != null) {
                parseSharedStrings(ByteArrayInputStream(sharedStringsStr))
            } else emptyList()
            
            val workbookStr = entries["xl/workbook.xml"]
            val sheetNames = if (workbookStr != null) {
                parseWorkbookSheetNames(ByteArrayInputStream(workbookStr))
            } else emptyList()
            
            val worksheetEntries = entries.keys
                .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
                .sortedBy { key ->
                    val numStr = key.substringAfter("xl/worksheets/sheet").substringBefore(".xml")
                    numStr.toIntOrNull() ?: 999
                }
                
            for (i in worksheetEntries.indices) {
                val entryKey = worksheetEntries[i]
                val sheetData = entries[entryKey] ?: continue
                val grid = parseWorksheet(ByteArrayInputStream(sheetData), sharedStrings)
                val name = if (i in sheetNames.indices) sheetNames[i] else "Sheet ${i + 1}"
                result.add(ParsedExcelSheet(name = name, grid = grid))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun findHeaderColumns(row: List<String>): Triple<Int, Int, Int>? {
        var dateIdx = -1
        var descIdx = -1
        var amountIdx = -1
        
        val dateSynonyms = listOf("date", "dt", "tarikh", "day")
        val descSynonyms = listOf("desc", "description", "note", "notes", "particulars", "particular", "narration", "details", "detail", "item", "remarks", "party", "payee")
        val amountSynonyms = listOf("amount", "amt", "spent", "received", "earned", "dr", "cr", "debit", "credit", "value", "rupees", "rs", "total")
        
        for (i in row.indices) {
            val cellClean = row[i].trim().lowercase()
            if (cellClean.isEmpty()) continue
            
            if (dateIdx == -1 && dateSynonyms.any { cellClean.contains(it) || it == cellClean }) {
                dateIdx = i
            } else if (descIdx == -1 && descSynonyms.any { cellClean.contains(it) || it == cellClean }) {
                descIdx = i
            } else if (amountIdx == -1 && amountSynonyms.any { cellClean.contains(it) || it == cellClean }) {
                amountIdx = i
            }
        }
        
        if (dateIdx != -1 && amountIdx != -1) {
            return Triple(dateIdx, descIdx, amountIdx)
        }
        return null
    }

    private class TableBoundingBox(
        val startRow: Int,
        val endRow: Int,
        val startCol: Int,
        val endCol: Int
    )

    private fun findTableTitleForColumns(grid: List<List<String>>, headerIdx: Int, startCol: Int, endCol: Int): String {
        // Look above for a non-empty cell in the rows just preceding the header
        for (r in (headerIdx - 1) downTo (headerIdx - 3).coerceAtLeast(0)) {
            if (r in grid.indices) {
                val row = grid[r]
                // Look for first non-empty cell inside the column range of the table
                for (col in startCol..minOf(endCol, row.size - 1)) {
                    val cell = row[col].trim()
                    if (cell.isNotEmpty()) {
                        return cell
                    }
                }
                // If nothing in that column range, get any non-empty cell in the row
                val nonAb = row.filter { it.trim().isNotEmpty() }
                if (nonAb.isNotEmpty()) {
                    return nonAb.first().trim()
                }
            }
        }
        return "Table at Row ${headerIdx + 1}"
    }

    fun extractTablesFromSheets(sheets: List<ParsedExcelSheet>): List<ExtractedTable> {
        val tables = mutableListOf<ExtractedTable>()
        for (sheet in sheets) {
            val grid = sheet.grid
            val boundingBoxes = mutableListOf<TableBoundingBox>()
            
            for (r in grid.indices) {
                val row = grid[r]
                val dateCols = mutableListOf<Int>()
                val dateSynonyms = listOf("date", "dt", "tarikh", "day")
                
                // Identify potential Date column indices in this row
                for (c in row.indices) {
                    val cellClean = row[c].trim().lowercase()
                    if (cellClean.isEmpty()) continue
                    
                    if (dateSynonyms.any { cellClean == it || cellClean.contains(it) }) {
                        // Ensure this cell hasn't already been consumed by an existing table bounding box
                        val isConsumed = boundingBoxes.any { box ->
                            r in box.startRow..box.endRow && c in box.startCol..box.endCol
                        }
                        if (!isConsumed) {
                            dateCols.add(c)
                        }
                    }
                }
                
                // For each Date column found, see if we can locate an Amount column to define a table
                for (dateCol in dateCols) {
                    val nextDateCol = dateCols.filter { it > dateCol }.minOrNull() ?: row.size
                    val maxScanCol = minOf(nextDateCol, dateCol + 8)
                    
                    var amountCol = -1
                    var descCol = -1
                    var catCol = -1
                    
                    val descSynonyms = listOf("desc", "description", "note", "notes", "particulars", "particular", "narration", "details", "detail", "item", "remarks", "party", "payee")
                    val amountSynonyms = listOf("amount", "amt", "spent", "received", "earned", "dr", "cr", "debit", "credit", "value", "rupees", "rs", "total")
                    val catSynonyms = listOf("category", "cat", "group", "tag", "classification", "head", "type")
                    
                    for (c in (dateCol + 1) until maxScanCol) {
                        if (c in row.indices) {
                            val cellClean = row[c].trim().lowercase()
                            if (cellClean.isEmpty()) continue
                            
                            if (amountCol == -1 && amountSynonyms.any { cellClean.contains(it) || it == cellClean }) {
                                amountCol = c
                            } else if (descCol == -1 && descSynonyms.any { cellClean.contains(it) || it == cellClean }) {
                                descCol = c
                            } else if (catCol == -1 && catSynonyms.any { cellClean.contains(it) || it == cellClean }) {
                                catCol = c
                            }
                        }
                    }
                    
                    // Fallback: If we didn't find description, grab any non-empty column in the range
                    if (descCol == -1 && amountCol != -1) {
                        for (c in (dateCol + 1) until maxScanCol) {
                            if (c != amountCol && c != catCol && c in row.indices) {
                                val cellClean = row[c].trim()
                                if (cellClean.isNotEmpty()) {
                                    descCol = c
                                    break
                                }
                            }
                        }
                    }
                    
                    // If we found both a Date and an Amount column, we can extract the table!
                    if (amountCol != -1) {
                        val startCol = minOf(dateCol, amountCol, if (descCol != -1) descCol else amountCol, if (catCol != -1) catCol else amountCol)
                        val endCol = maxOf(dateCol, amountCol, if (descCol != -1) descCol else amountCol, if (catCol != -1) catCol else amountCol)
                        
                        val tableRows = mutableListOf<ExtractedRow>()
                        var dataRowIdx = r + 1
                        
                        while (dataRowIdx < grid.size) {
                            // Stop if another header bounding box is declared at this cell location
                            val isOverlap = boundingBoxes.any { box ->
                                dataRowIdx == box.startRow && dateCol in box.startCol..box.endCol
                            }
                            if (isOverlap) {
                                break
                            }
                            
                            val dataRow = grid[dataRowIdx]
                            val dateVal = dataRow.getOrNull(dateCol)?.trim() ?: ""
                            val amtVal = dataRow.getOrNull(amountCol)?.trim() ?: ""
                            
                            // Exit if both Date and Amount are empty (end of table data)
                            if (dateVal.isEmpty() && amtVal.isEmpty()) {
                                break
                            }
                            
                            // Look for row level summaries like "Total", "Grand Total", "Balance"
                            val cellTexts = (startCol..endCol).map { dataRow.getOrNull(it)?.trim()?.lowercase() ?: "" }
                            if (cellTexts.any { it.contains("total") || it.contains("sum") || it.contains("grand total") || it.contains("balance") }) {
                                break
                            }
                            
                            val descVal = if (descCol != -1) dataRow.getOrNull(descCol)?.trim() ?: "" else ""
                            val catVal = if (catCol != -1) dataRow.getOrNull(catCol)?.trim() ?: "" else ""
                            
                            tableRows.add(
                                ExtractedRow(
                                    originalRowIndex = dataRowIdx,
                                    rawDate = dateVal,
                                    rawDesc = descVal,
                                    rawAmount = amtVal,
                                    rawCategory = catVal
                                )
                            )
                            dataRowIdx++
                        }
                        
                        if (tableRows.isNotEmpty()) {
                            val title = findTableTitleForColumns(grid, r, startCol, endCol)
                            val endRow = dataRowIdx - 1
                            boundingBoxes.add(
                                TableBoundingBox(
                                    startRow = r,
                                    endRow = endRow,
                                    startCol = startCol,
                                    endCol = endCol
                                )
                            )
                            tables.add(
                                ExtractedTable(
                                    sheetName = sheet.name,
                                    title = title,
                                    headerRowIndex = r,
                                    dateColumnIndex = dateCol,
                                    descColumnIndex = descCol,
                                    amountColumnIndex = amountCol,
                                    categoryColumnIndex = catCol,
                                    rows = tableRows
                                )
                            )
                        }
                    }
                }
            }
        }
        return tables
    }

    fun parseAmount(raw: String): Double? {
        val clean = raw.replace("₹", "").replace(",", "").replace("-", "").trim()
        return clean.toDoubleOrNull()
    }

    fun parseExcelDate(raw: String, sheetName: String): Long {
        val cleanVal = raw.trim()
        
        val doubleVal = cleanVal.toDoubleOrNull()
        if (doubleVal != null && doubleVal > 10000 && doubleVal < 100000) {
            val days = doubleVal.toLong()
            val ms = (days - 25569L) * 24L * 60L * 60L * 1000L
            return ms
        }
        
        val digitsVal = cleanVal.toIntOrNull()
        if (digitsVal != null && digitsVal in 1..31) {
            val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
            val monthIdx = months.indexOfFirst { sheetName.lowercase().startsWith(it) }
            if (monthIdx != -1) {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH)
                var year = currentYear
                if (monthIdx in 0..2 && currentMonth >= 3) {
                    year += 1
                } else if (monthIdx in 3..11 && currentMonth < 3) {
                    year -= 1
                }
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthIdx)
                cal.set(Calendar.DAY_OF_MONTH, digitsVal)
                cal.set(Calendar.HOUR_OF_DAY, 12)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }
        
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "yyyy-MM-dd",
            "dd/MM/yy", "dd-MM-yy", "d/M/yyyy", "d-M-yyyy",
            "d MMM yyyy", "dd MMM yyyy", "dd-MMM-yyyy", "d-MMM-yyyy",
            "dd.MM.yyyy", "dd.MM.yy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(cleanVal)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        
        val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        val monthIdx = months.indexOfFirst { sheetName.lowercase().startsWith(it) }
        if (monthIdx != -1) {
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH)
            var year = currentYear
            if (monthIdx in 0..2 && currentMonth >= 3) {
                year += 1
            } else if (monthIdx in 3..11 && currentMonth < 3) {
                year -= 1
            }
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthIdx)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        
        return System.currentTimeMillis()
    }
}
