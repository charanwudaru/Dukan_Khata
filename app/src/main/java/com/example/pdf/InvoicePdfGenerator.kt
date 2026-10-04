package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Bill
import com.example.data.model.BillItem
import com.example.data.model.ShopProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    fun generateBillPdf(
        context: Context,
        shopProfile: ShopProfile?,
        bill: Bill,
        items: List<BillItem>
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(26, 35, 126) // Deep Navy
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }

            val tableHeaderBg = Paint().apply {
                color = Color.rgb(240, 243, 246)
                style = Paint.Style.FILL
            }

            var y = 45f

            // --- Header: Shop Details ---
            val shopTitle = shopProfile?.shopName?.ifEmpty { "Dukan Khata Store" } ?: "Dukan Khata Store"
            canvas.drawText(shopTitle, 40f, y, headerPaint)
            y += 18f

            textPaint.textSize = 10f
            if (!shopProfile?.address.isNullOrBlank()) {
                canvas.drawText(shopProfile?.address ?: "", 40f, y, textPaint)
                y += 14f
            }
            if (!shopProfile?.phone.isNullOrBlank()) {
                canvas.drawText("Phone: ${shopProfile?.phone}", 40f, y, textPaint)
                y += 14f
            }
            val hasGst = !shopProfile?.gstin.isNullOrBlank()
            if (hasGst) {
                boldPaint.textSize = 10f
                canvas.drawText("GSTIN: ${shopProfile?.gstin}", 40f, y, boldPaint)
                y += 16f
            }

            // Document Title on Right
            boldPaint.textSize = 14f
            boldPaint.color = Color.rgb(26, 35, 126)
            val docTitle = when (bill.billType.lowercase()) {
                "quotation" -> "ESTIMATE / QUOTATION"
                "receipt" -> "PAYMENT RECEIPT"
                else -> if (hasGst) "TAX INVOICE" else "RETAIL INVOICE"
            }
            canvas.drawText(docTitle, 380f, 45f, boldPaint)

            boldPaint.textSize = 10f
            boldPaint.color = Color.DKGRAY
            canvas.drawText("Bill #: ${bill.billNumber.ifEmpty { bill.id.take(8).uppercase() }}", 380f, 62f, boldPaint)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
            canvas.drawText("Date: ${dateFormat.format(Date(bill.billDate))}", 380f, 76f, textPaint)
            canvas.drawText("Mode: ${bill.paymentMode.uppercase()}", 380f, 90f, boldPaint)

            y = maxOf(y + 10f, 110f)
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 16f

            // --- Customer Section ---
            boldPaint.textSize = 11f
            boldPaint.color = Color.BLACK
            canvas.drawText("Billed To:", 40f, y, boldPaint)
            y += 14f
            textPaint.textSize = 11f
            canvas.drawText("Name: ${bill.customerNameSnapshot}", 40f, y, textPaint)
            if (bill.customerPhoneSnapshot.isNotBlank()) {
                canvas.drawText("Phone: ${bill.customerPhoneSnapshot}", 300f, y, textPaint)
            }
            y += 20f

            // --- Table Header ---
            canvas.drawRect(40f, y, 555f, y + 24f, tableHeaderBg)
            boldPaint.textSize = 10.5f
            boldPaint.color = Color.rgb(30, 30, 30)
            canvas.drawText("#", 50f, y + 16f, boldPaint)
            canvas.drawText("Item Description", 85f, y + 16f, boldPaint)
            canvas.drawText("Qty", 320f, y + 16f, boldPaint)
            canvas.drawText("Price (₹)", 410f, y + 16f, boldPaint)
            canvas.drawText("Total (₹)", 495f, y + 16f, boldPaint)
            y += 24f

            // --- Table Rows ---
            textPaint.textSize = 10.5f
            items.forEachIndexed { index, item ->
                y += 18f
                canvas.drawText("${index + 1}", 50f, y, textPaint)
                canvas.drawText(item.itemNameSnapshot, 85f, y, textPaint)
                val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else "%.2f".format(item.qty)
                canvas.drawText("$qtyStr ${item.unit}", 320f, y, textPaint)
                canvas.drawText("₹%.2f".format(item.pricePerUnit), 410f, y, textPaint)
                canvas.drawText("₹%.2f".format(item.lineTotal), 495f, y, textPaint)
                y += 6f
                canvas.drawLine(40f, y, 555f, y, linePaint)
            }

            y += 18f

            // --- Summary Section ---
            val summaryX = 350f
            textPaint.textSize = 11f
            canvas.drawText("Subtotal:", summaryX, y, textPaint)
            canvas.drawText("₹%.2f".format(bill.subtotal), 495f, y, textPaint)
            y += 16f

            if (bill.discount > 0) {
                canvas.drawText("Discount:", summaryX, y, textPaint)
                canvas.drawText("-₹%.2f".format(bill.discount), 495f, y, textPaint)
                y += 16f
            }

            if (hasGst && bill.gstAmount > 0) {
                val halfGst = bill.gstAmount / 2.0
                canvas.drawText("CGST (Split):", summaryX, y, textPaint)
                canvas.drawText("₹%.2f".format(halfGst), 495f, y, textPaint)
                y += 14f
                canvas.drawText("SGST (Split):", summaryX, y, textPaint)
                canvas.drawText("₹%.2f".format(halfGst), 495f, y, textPaint)
                y += 16f
            }

            // Grand Total
            canvas.drawLine(summaryX, y, 555f, y, linePaint)
            y += 18f
            boldPaint.textSize = 14f
            boldPaint.color = Color.rgb(26, 35, 126)
            canvas.drawText("Total Payable:", summaryX, y, boldPaint)
            canvas.drawText("₹%.2f".format(bill.total), 485f, y, boldPaint)

            y += 35f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // --- Footer ---
            textPaint.textSize = 10f
            textPaint.color = Color.GRAY
            val lang = shopProfile?.language?.lowercase() ?: "en"
            val footerMsg = when (lang) {
                "hi" -> "धन्यवाद! फिर पधारें।"
                "te" -> "ధన్యవాదాలు! మళ్లీ రండి."
                "ta" -> "நன்றி! மீண்டும் வருக."
                "kn" -> "ಧನ್ಯವಾದಗಳು! ಮತ್ತೆ ಭೇಟಿ ನೀಡಿ."
                "ml" -> "നന്ദി! വീണ്ടും സന്ദർശിക്കുക."
                "mr" -> "धन्यवाद! पुन्हा भेट द्या."
                "bn" -> "ধন্যবাদ! আবার আসবেন।"
                "gu" -> "આભાર! ફરી પધારો."
                "pa" -> "ਧੰਨਵਾਦ! ਦੁਬਾਰਾ ਆਓ।"
                else -> "Thank You for your business! Visit Again."
            }
            canvas.drawText(footerMsg, 40f, y, textPaint)
            textPaint.textSize = 8.5f
            canvas.drawText("Generated via Dukan Khata • 100% Offline Ledger", 40f, y + 14f, textPaint)

            pdfDocument.finishPage(page)

            // Save to internal cache dir
            val billsDir = File(context.cacheDir, "bills")
            if (!billsDir.exists()) billsDir.mkdirs()
            val fileName = "Bill_${bill.billNumber.ifEmpty { bill.id.take(6) }}.pdf"
            val file = File(billsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun sharePdfViaWhatsApp(
        context: Context,
        pdfFile: File,
        customerPhone: String?,
        message: String = "Here is your bill from our store."
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // If phone provided, format for international whatsapp if starting without prefix
                if (!customerPhone.isNullOrBlank()) {
                    val cleanNumber = customerPhone.replace(Regex("[^0-9]"), "")
                    val formattedPhone = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                }
            }

            // Try direct WhatsApp packages first
            val whatsappIntent = Intent(shareIntent).apply {
                setPackage("com.whatsapp")
            }
            val w4bIntent = Intent(shareIntent).apply {
                setPackage("com.whatsapp.w4b")
            }

            val pm = context.packageManager
            if (whatsappIntent.resolveActivity(pm) != null) {
                context.startActivity(whatsappIntent)
            } else if (w4bIntent.resolveActivity(pm) != null) {
                context.startActivity(w4bIntent)
            } else {
                // Fallback to standard chooser
                val chooser = Intent.createChooser(shareIntent, "Share Bill PDF via")
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewOrPrintPdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open Bill PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF viewer found on device", Toast.LENGTH_SHORT).show()
        }
    }
}
