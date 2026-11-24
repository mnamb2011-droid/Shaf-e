package com.shafii.encyclopedia.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.shafii.encyclopedia.data.repository.IssueFullDetail
import java.io.File
import java.io.FileOutputStream

/**
 * Offline exporter for selected jurisprudential research as plain text (.txt) or PDF (.pdf).
 */
object IssueExporter {

    fun formatIssueAsPlainText(detail: IssueFullDetail): String = buildString {
        val issue = detail.issue
        appendLine("الموسوعة الشافعية الشاملة")
        appendLine("====================================")
        appendLine("المسألة: ${issue.title}")
        appendLine("التصنيف الفقهي: ${issue.fiqhClassification}")
        appendLine("درجة التحقق: ${detail.verificationLabel}")
        appendLine()
        appendLine("1. تصوير المسألة:\n${issue.issueFraming}\n")
        appendLine("2. الحكم المختصر:\n${issue.shortRuling}\n")
        appendLine("3. المعتمد في المذهب:\n${issue.mutamadSummary}\n")
        appendLine("4. قول الإمام الشافعي القديم:\n${issue.qadimSummary}\n")
        appendLine("5. قول الإمام الشافعي الجديد:\n${issue.jadidSummary}\n")
        appendLine("6. أقوال الأصحاب:\n${issue.ashabSummary}\n")
        appendLine("7. الخلاف والأوجه:\n${issue.khilafAwjuhSummary}\n")
        appendLine("8. تطور الترجيح عبر العصور:\n${issue.historicalEvolution}\n")
        appendLine("9. مناقشة الأدلة:\n${issue.evidenceDiscussion}\n")
        appendLine("10. سبب الخلاف:\n${issue.reasonForDisagreement}\n")
        appendLine("11. خلاصة مقارنة:\n${issue.comparativeSummary}\n")

        if (detail.opinions.isNotEmpty()) {
            appendLine("--- الأقوال الموثقة ومصادرها ---")
            detail.opinions.forEachIndexed { idx, op ->
                appendLine("${idx + 1}) ${op.scholarCommonName} (${op.periodName}) [${op.opinionType.replace('_', ' ')}]:")
                appendLine("   القول: ${op.opinionText}")
                if (op.reasoningSummary.isNotBlank()) appendLine("   التعليل: ${op.reasoningSummary}")
                appendLine("   التحقق: ${op.verificationLabel}")
                op.sources.forEach { src ->
                    val volPage = buildString {
                        if (!src.volume.isNullOrBlank()) append(" ج${src.volume}")
                        if (!src.page.isNullOrBlank()) append(" ص${src.page}")
                    }
                    appendLine("   المصدر: ${src.referenceTitle}$volPage — ${src.sectionLocation}")
                }
                appendLine()
            }
        }
    }

    fun exportToTextFile(context: Context, detail: IssueFullDetail): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "shafii_issue_${detail.issue.id}.txt")
        file.writeText(formatIssueAsPlainText(detail), Charsets.UTF_8)
        return file
    }

    fun exportToPdfFile(context: Context, detail: IssueFullDetail): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "shafii_issue_${detail.issue.id}.pdf")

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val maxLineWidth = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F4C3A")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E2421")
            textSize = 11.5f
            textAlign = Paint.Align.RIGHT
        }

        val lines = wrapText(formatIssueAsPlainText(detail), bodyPaint, maxLineWidth)
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin + 16f

        canvas.drawText(detail.issue.title, pageWidth - margin, y, titlePaint)
        y += 26f

        for (line in lines) {
            if (y > pageHeight - margin) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin + 12f
            }
            canvas.drawText(line, pageWidth - margin, y, bodyPaint)
            y += 17f
        }
        pdf.finishPage(page)

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة البحث الفقهي"))
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            val current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current.clear()
                    current.append(candidate)
                } else {
                    if (current.isNotEmpty()) result.add(current.toString())
                    current.clear()
                    current.append(word)
                }
            }
            if (current.isNotEmpty()) result.add(current.toString())
        }
        return result
    }
}
