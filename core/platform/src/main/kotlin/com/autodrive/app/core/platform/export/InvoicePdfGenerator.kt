package com.autodrive.app.core.platform.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.R
import com.autodrive.app.core.model.money.Money
import java.io.File

object InvoicePdfGenerator {

    // ─── أبعاد وثوابت ────────────────────────────────────────────
    private const val PW           = 595f   // A4 width
    private const val PH           = 842f   // A4 height
    private const val MARGIN       = 40f
    private const val BOTTOM_LIMIT = PH - 60f
    private const val ROW_H        = 32f

    private const val ACCENT  = "#0F766E"
    private const val BG_ROW  = "#F0FDF4"  // أخضر شفاف للصفوف الفردية
    private const val BG_TOT  = "#CCFBF1"  // إجمالي
    private const val BG_AJAL = "#FEF9C3"  // عليه — أصفر فاتح
    private const val ROW_SEP = "#D1FAE5"
    private const val TXT1    = "#1F2937"
    private const val TXT2    = "#6B7280"

    // أعمدة RTL: الصنف | الكمية | السعر | الإجمالي
    private val COL_DEFS = listOf("الصنف" to 230f, "الكمية" to 65f, "السعر" to 115f, "الإجمالي" to 105f)

    // ─── Public API ──────────────────────────────────────────────
    fun generateAndShare(
        context: Context, entry: InvoicePdfEntry, userName: String,
        items: List<InvoicePdfItem> = emptyList(), invoiceStatus: String = ""
    ) = shareFile(context, buildPdf(context, entry, items, invoiceStatus), entry.invoiceNumber)

    fun generateAndPrint(
        context: Context, entry: InvoicePdfEntry,
        items: List<InvoicePdfItem> = emptyList(), invoiceStatus: String = ""
    ) = shareFile(context, buildPdf(context, entry, items, invoiceStatus), entry.invoiceNumber)

    // ─── بناء PDF ────────────────────────────────────────────────
    private fun buildPdf(
        context: Context, entry: InvoicePdfEntry,
        items: List<InvoicePdfItem>, invoiceStatus: String
    ): File {
        val tf     = loadFont(context, R.font.tajawal_regular)
        val tfBold = loadFont(context, R.font.tajawal_bold)
        val fsBase  = 14f
        val fsSmall = 12f
        val fsTitle = 16f

        val isAjal = invoiceStatus.uppercase() == "CLOSED_CREDIT"

        // أعمدة RTL
        val cols = rtlCols(COL_DEFS)
        val seps = cols.dropLast(1).map { it.s }

        val ptBody     = p(TXT1,   fsBase,  false, tf)
        val ptMuted    = p(TXT2,   fsSmall, false, tf)
        val ptHdrCell  = p("#FFFFFF", fsSmall, true,  tfBold)
        val ptTotal    = p(ACCENT,  fsBase,  true,  tfBold)
        val bgHdrRow   = bg(ACCENT)
        val bgAlt      = bg(BG_ROW)
        val bgTot      = bg(BG_TOT)
        val bgAjal     = bg(BG_AJAL)
        val lpRow      = lp(ROW_SEP, 0.6f)
        val lpAccent   = lp(ACCENT,  1.5f)
        val lpLight    = lp("#E5E7EB", 0.5f)

        fun drawTableHeader(cv: Canvas, y: Float): Float {
            drawRowBox(cv, y, ROW_H, bgHdrRow, lpRow, seps)
            cols.forEach { col -> cellRight(cv, col.label, col.s, col.e, y, ROW_H, ptHdrCell) }
            return y + ROW_H
        }

        val doc = PdfDocument()
        var pageNum = 1
        var page    = newPage(doc, pageNum)
        var cv      = page.canvas
        var y       = MARGIN

        // ── معلومات الفاتورة (رقم + تاريخ) ──────────────────────
        val ptNumLeft  = p(ACCENT, fsTitle, true, tfBold).apply { textAlign = Paint.Align.LEFT }
        val ptDateLeft = p(TXT2,   fsSmall, false, tf).apply    { textAlign = Paint.Align.LEFT }
        cv.drawText("فاتورة  #${entry.invoiceNumber}", MARGIN, y + fsTitle, ptNumLeft)
        cv.drawText(FormatUtils.formatDate(entry.createdAt), MARGIN, y + fsTitle + fsSmall + 5f, ptDateLeft)
        y += fsTitle + fsSmall + 18f

        cv.drawLine(MARGIN, y, PW - MARGIN, y, lpAccent)
        y += 10f

        // ── رأس الجدول ───────────────────────────────────────────
        y = drawTableHeader(cv, y)

        // ── صفوف البنود ──────────────────────────────────────────
        val totalsH = ROW_H * (if (isAjal) 2 else 1) + 4f
        items.forEachIndexed { idx, item ->
            if (y + ROW_H > BOTTOM_LIMIT) {
                doc.finishPage(page); pageNum++
                page = newPage(doc, pageNum); cv = page.canvas; y = MARGIN
                y = drawTableHeader(cv, y)
            }
            val rowBg = if (idx % 2 == 0) null else bgAlt
            drawRowBox(cv, y, ROW_H, rowBg, lpLight, seps)
            cellRight(cv, item.itemName,                          cols[0].s, cols[0].e, y, ROW_H, ptBody)
            cellCenter(cv, item.quantity.toString(),              cols[1].s, cols[1].e, y, ROW_H, ptBody)
            cellRight(cv, FormatUtils.formatSarNoLabel(item.sellPrice),  cols[2].s, cols[2].e, y, ROW_H, ptBody)
            cellRight(cv, FormatUtils.formatSarNoLabel(item.totalPrice), cols[3].s, cols[3].e, y, ROW_H, ptBody)
            y += ROW_H
        }

        // ── إجماليات ─────────────────────────────────────────────
        if (y + totalsH > BOTTOM_LIMIT) {
            doc.finishPage(page); pageNum++
            page = newPage(doc, pageNum); cv = page.canvas; y = MARGIN
        }
        val totalAmount = Money.sum(items.map { it.totalPrice })
        val totSeps     = listOf(cols[3].e)

        cv.drawLine(MARGIN, y, PW - MARGIN, y, lpAccent); y += 4f

        // إجمالي الفاتورة
        drawRowBox(cv, y, ROW_H, bgTot, lpRow, totSeps)
        cellRight(cv, "إجمالي الفاتورة", cols[3].e, PW - MARGIN, y, ROW_H, ptTotal)
        cellRight(cv, FormatUtils.formatSarNoLabel(totalAmount), cols[3].s, cols[3].e, y, ROW_H, ptTotal)
        y += ROW_H

        // صف "عليه" — آجل فقط
        if (isAjal) {
            val ptAjal = p("#92400E", fsBase, true, tfBold)
            drawRowBox(cv, y, ROW_H, bgAjal, lpRow, totSeps)
            cellRight(cv, "عليه", cols[3].e, PW - MARGIN, y, ROW_H, ptAjal)
            cellRight(cv, FormatUtils.formatSarNoLabel(totalAmount), cols[3].s, cols[3].e, y, ROW_H, ptAjal)
            y += ROW_H
        }

        doc.finishPage(page)

        val file = File(File(context.cacheDir, "pdfs").also { it.mkdirs() },
            "invoice_${entry.invoiceNumber}.pdf")
        doc.writeTo(file.outputStream())
        doc.close()
        return file
    }

    // ─── مساعدات ─────────────────────────────────────────────────

    private data class Col(val s: Float, val e: Float, val label: String)

    private fun rtlCols(defs: List<Pair<String, Float>>): List<Col> {
        var x = PW - MARGIN
        return defs.map { (label, w) -> Col(s = x - w, e = x, label = label).also { x -= w } }
    }

    private fun newPage(doc: PdfDocument, num: Int): PdfDocument.Page =
        doc.startPage(PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), num).create())
            .also { it.canvas.drawRect(0f, 0f, PW, PH, bg("#FFFFFF")) }

    private fun drawRowBox(cv: Canvas, y: Float, h: Float, fill: Paint?, border: Paint, seps: List<Float>) {
        fill?.let { cv.drawRect(MARGIN, y, PW - MARGIN, y + h, it) }
        cv.drawRect(MARGIN, y, PW - MARGIN, y + h, border)
        seps.forEach { x -> cv.drawLine(x, y, x, y + h, border) }
    }

    private fun cellRight(cv: Canvas, text: String, s: Float, e: Float, y: Float, h: Float, paint: Paint) {
        val saved = paint.textAlign; paint.textAlign = Paint.Align.RIGHT
        cv.drawText(text, e - 6f, y + h * 0.65f, paint); paint.textAlign = saved
    }

    private fun cellCenter(cv: Canvas, text: String, s: Float, e: Float, y: Float, h: Float, paint: Paint) {
        val saved = paint.textAlign; paint.textAlign = Paint.Align.CENTER
        cv.drawText(text, s + (e - s) / 2f, y + h * 0.65f, paint); paint.textAlign = saved
    }

    private fun shareFile(context: Context, file: File, invoiceNumber: Int) {
        val uri    = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type     = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "فاتورة #$invoiceNumber")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }

    private fun loadFont(context: Context, resId: Int): Typeface =
        ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT

    private fun p(hex: String, size: Float, bold: Boolean, tf: Typeface?) =
        Paint().apply {
            color          = Color.parseColor(hex)
            textSize       = size
            isFakeBoldText = bold
            isAntiAlias    = true
            typeface       = tf
        }

    private fun bg(hex: String)               = Paint().apply { color = Color.parseColor(hex) }
    private fun lp(hex: String, w: Float)     = Paint().apply {
        color       = Color.parseColor(hex)
        strokeWidth = w
        style       = Paint.Style.STROKE
        isAntiAlias = true
    }
}
