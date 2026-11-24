package com.shafii.encyclopedia.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.data.repository.IssueFullDetail
import com.shafii.encyclopedia.ui.components.ScholarlySectionCard
import com.shafii.encyclopedia.ui.components.TagPill
import com.shafii.encyclopedia.ui.components.VerificationBadge
import com.shafii.encyclopedia.ui.theme.*
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel
import com.shafii.encyclopedia.util.DatabasePackManager
import com.shafii.encyclopedia.util.IssueExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    detail: IssueFullDetail,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenRelatedIssue: (Int) -> Unit,
    onOpenComparison: (Int) -> Unit
) {
    val context = LocalContext.current
    val issue = detail.issue
    val isBookmarked by viewModel.observeIsBookmarked(issue.id).collectAsState(initial = false)
    val notes by viewModel.observeNotesForIssue(issue.id).collectAsState(initial = emptyList())
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("عرض المسألة الفقهية", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBookmark(issue, isBookmarked) }) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "المفضلة",
                            tint = if (isBookmarked) GoldAccent else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showNoteDialog = true }) {
                        Icon(Icons.Default.EditNote, contentDescription = "إضافة ملاحظة")
                    }
                    IconButton(onClick = {
                        val txt = IssueExporter.formatIssueAsPlainText(detail)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText(issue.title, txt))
                        Toast.makeText(context, "تم نسخ المسألة مع مصادرها", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ")
                    }
                    IconButton(onClick = {
                        val pdfFile = IssueExporter.exportToPdfFile(context, detail)
                        IssueExporter.shareFile(context, pdfFile, "application/pdf")
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF")
                    }
                }
            )
        }
    ) { padding ->
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. اسم المسألة + 2. التصنيف الفقهي + 18. درجة التحقق
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(issue.fiqhClassification, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(6.dp))
                            Text(issue.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VerificationBadge(issue.verificationStatusCode, detail.verificationLabel)
                                OutlinedButton(
                                    onClick = { onOpenComparison(issue.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("وضع المقارنة")
                                }
                            }
                        }
                    }
                }

                // 3. تصوير المسألة
                item { ScholarlySectionCard("تصوير المسألة", issue.issueFraming) }

                // 4. الحكم المختصر
                item { ScholarlySectionCard("الحكم المختصر", issue.shortRuling, accentColor = GoldAccent) }

                // 5. المعتمد في المذهب (+ المتقدمين والمتأخرين واتفاق الشيخين)
                item {
                    val fullMutamad = buildString {
                        appendLine(issue.mutamadSummary)
                        appendLine("\n• المعتمد عند المتقدمين: ${issue.mutamadEarly}")
                        appendLine("• المعتمد عند المتأخرين: ${issue.mutamadLate}")
                        append("• موقف الشيخين (الرافعي والنووي): ${issue.rafiiNawawiAgreement}")
                    }
                    ScholarlySectionCard(
                        title = "المعتمد في المذهب",
                        content = fullMutamad,
                        accentColor = MutamadGreen,
                        highlightBackground = true
                    )
                }

                // 6. قول الإمام الشافعي القديم
                item { ScholarlySectionCard("قول الإمام الشافعي القديم (العراقي)", issue.qadimSummary, accentColor = QadimAmber) }

                // 7. قول الإمام الشافعي الجديد
                item { ScholarlySectionCard("قول الإمام الشافعي الجديد (المصري)", issue.jadidSummary, accentColor = JadidBlue) }

                // 8. أقوال الأصحاب
                item { ScholarlySectionCard("أقوال الأصحاب", issue.ashabSummary) }

                // 9. الخلاف والأوجه
                item { ScholarlySectionCard("الخلاف والأوجه والطرق", issue.khilafAwjuhSummary) }

                // 10. تطور الترجيح عبر العصور
                item { ScholarlySectionCard("تطور الترجيح عبر العصور", issue.historicalEvolution, accentColor = EmeraldPrimary) }

                // Historical Opinion Engine Cards (Expandable Quotations & Sources)
                item {
                    Text(
                        "التسلسل التاريخي للأقوال الموثقة (${detail.opinions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(detail.opinions) { op ->
                    var showQuotes by remember { mutableStateOf(false) }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (op.isMutamad) 1.5.dp else 1.dp,
                            color = if (op.isMutamad) MutamadGreen else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${op.scholarCommonName} • ${op.periodName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (op.isMutamad) TagPill("المعتمد", MutamadGreen)
                                if (op.isContemporary) TagPill("اجتهاد معاصر", ContemporaryPurple)
                                if (op.qadimJadidClass == "قديم" || op.qadimJadidClass == "مرجح_للقديم") {
                                    TagPill(op.qadimJadidClass.replace('_', ' '), QadimAmber)
                                } else if (op.qadimJadidClass == "جديد" || op.qadimJadidClass == "موافق_للجديد") {
                                    TagPill(op.qadimJadidClass.replace('_', ' '), JadidBlue)
                                }
                                TagPill(op.opinionType.replace('_', ' '), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(op.opinionText, style = MaterialTheme.typography.bodyLarge)
                            if (op.reasoningSummary.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("التعليل: ${op.reasoningSummary}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (op.attributionNotes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("تنبيه النسبة: ${op.attributionNotes}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.height(6.dp))
                            VerificationBadge(op.verificationCode, op.verificationLabel)

                            if (op.quotations.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                TextButton(onClick = { showQuotes = !showQuotes }) {
                                    Text(if (showQuotes) "إخفاء النصوص المقتبسة" else "عرض النص المقتبس من المصدر (${op.quotations.size})")
                                }
                                AnimatedVisibility(visible = showQuotes) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        op.quotations.forEach { q ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text("«${q.quotationText}»", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                    Spacer(Modifier.height(4.dp))
                                                    val loc = buildString {
                                                        append(q.referenceTitle)
                                                        if (!q.volume.isNullOrBlank()) append(" — ج${q.volume}")
                                                        if (!q.page.isNullOrBlank()) append(" ص${q.page}")
                                                    }
                                                    Text(loc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 11. أدلة الأقوال
                item {
                    val evText = if (detail.evidences.isEmpty()) {
                        DatabasePackManager.FALLBACK_MISSING_TEXT
                    } else {
                        detail.evidences.joinToString("\n\n") { ev ->
                            "• [${ev.evidenceType.replace('_', ' ')}] ${ev.textContent}\n  المخرج: ${ev.sourceReference}" +
                                if (ev.authenticityNote.isNotBlank()) "\n  ملاحظة: ${ev.authenticityNote}" else ""
                        }
                    }
                    ScholarlySectionCard("أدلة الأقوال", evText)
                }

                // 12. مناقشة الأدلة
                item { ScholarlySectionCard("مناقشة الأدلة", issue.evidenceDiscussion) }

                // 13. سبب الخلاف
                item { ScholarlySectionCard("سبب الخلاف", issue.reasonForDisagreement) }

                // 14. خلاصة مقارنة
                item { ScholarlySectionCard("خلاصة مقارنة", issue.comparativeSummary, accentColor = GoldAccent) }

                // 15 & 16. المصادر الأصلية ومواضع النصوص وأرقام الصفحات
                item {
                    val allSources = detail.opinions.flatMap { it.sources }
                    val srcText = if (allSources.isEmpty()) {
                        DatabasePackManager.FALLBACK_MISSING_TEXT
                    } else {
                        allSources.joinToString("\n\n") { src ->
                            val volPage = when {
                                !src.volume.isNullOrBlank() && !src.page.isNullOrBlank() -> "ج${src.volume} / ص${src.page}"
                                !src.volume.isNullOrBlank() -> "ج${src.volume} (رقم الصفحة غير مقابل على طبعة محددة)"
                                else -> "رقم الجزء والصفحة غير مقيد بطبعة محددة (الموضع بالباب)"
                            }
                            val checkedTag = if (src.checkedAgainstOriginal) "مقابل على النص" else "نسبة موثقة تحتاج مقابلة صفحة"
                            "• ${src.referenceTitle} (${src.authorName})\n  الموضع: ${src.sectionLocation}\n  الجزء والصفحة: $volPage\n  نوع التوثيق: ${src.citationType.replace('_', ' ')} — $checkedTag"
                        }
                    }
                    ScholarlySectionCard("المصادر الأصلية ومواضع النصوص وأرقام الصفحات", srcText)
                }

                // 19. روابط المسائل ذات الصلة
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("روابط المسائل ذات الصلة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            if (detail.relatedIssues.isEmpty()) {
                                Text(DatabasePackManager.FALLBACK_MISSING_TEXT, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                detail.relatedIssues.forEach { rel ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onOpenRelatedIssue(rel.issueId) }
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(rel.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                            Text("${rel.relationType.replace('_', ' ')}: ${rel.notes}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // User's Personal Notes on this Issue
                if (notes.isNotEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("ملاحظاتي الشخصية على هذه المسألة", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
                                Spacer(Modifier.height(6.dp))
                                notes.forEach { n ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(n.noteText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        IconButton(onClick = { viewModel.deleteNote(n.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("إضافة ملاحظة شخصية") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    placeholder = { Text("اكتب فائدة أو تعليقاً على المسألة...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveNote(issue.id, issue.title, noteDraft)
                    noteDraft = ""
                    showNoteDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
