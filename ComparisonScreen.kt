package com.shafii.encyclopedia.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.data.repository.ComparisonFilter
import com.shafii.encyclopedia.data.repository.OpinionDetail
import com.shafii.encyclopedia.ui.components.TagPill
import com.shafii.encyclopedia.ui.theme.*
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun ComparisonScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val allIssues by viewModel.allIssues.collectAsState()
    val detail by viewModel.comparisonIssue.collectAsState()
    val filter by viewModel.comparisonFilter.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    val filteredOpinions = remember(detail, filter) {
        val list = detail?.opinions.orEmpty()
        when (filter) {
            ComparisonFilter.ALL -> list
            ComparisonFilter.QADIM_JADID -> list.filter {
                it.qadimJadidClass == "قديم" || it.qadimJadidClass == "جديد"
            }
            ComparisonFilter.EARLY_AND_INTERMEDIATE -> list.filter {
                it.periodCode == "imam_shafii" || it.periodCode == "early_ashab" || it.periodCode == "intermediate"
            }
            ComparisonFilter.LATER_AND_SHAYKHAN -> list.filter {
                it.periodCode == "tanqih_shaykhan" || it.periodCode == "later_muhaqqiqun"
            }
            ComparisonFilter.CONTEMPORARY -> list.filter { it.isContemporary }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("المقارنة بين الأقوال والتسلسل التاريخي", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(detail?.issue?.title ?: "اختر مسألة للمقارنة", maxLines = 1)
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    allIssues.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.title) },
                            onClick = {
                                viewModel.selectIssueForComparison(item.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ComparisonFilter.entries) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { viewModel.setComparisonFilter(f) },
                        label = { Text(f.labelAr) }
                    )
                }
            }
        }

        if (detail != null) {
            // Distinction Banner: Mu'tamad vs Documented Opinions
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MutamadGreen)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("المعتمد في المذهب (تمييزاً له عن سائر الأقوال)", style = MaterialTheme.typography.titleMedium, color = MutamadGreen, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(detail!!.issue.mutamadSummary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { onOpenIssue(detail!!.issue.id) }) {
                            Text("فتح صفحة المسألة الكاملة")
                        }
                    }
                }
            }

            // Visual Historical Timeline
            item {
                Text("الخط الزمني لتطور الأقوال", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (filteredOpinions.isEmpty()) {
                    Text("لا توجد أقوال مطابقة لهذا الفلتر في المسألة المختارة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredOpinions.forEachIndexed { idx, op ->
                            TimelineOpinionNode(op = op, index = idx + 1)
                        }
                    }
                }
            }

            // Side-by-Side Comparison Table (Scrollable horizontally for clarity)
            if (filteredOpinions.isNotEmpty()) {
                item {
                    Text("جدول المقارنة المباشرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    ComparisonTable(
                        opinions = filteredOpinions,
                        reasonForDisagreement = detail!!.issue.reasonForDisagreement
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineOpinionNode(op: OpinionDetail, index: Int) {
    val dotColor = when {
        op.isMutamad -> MutamadGreen
        op.isContemporary -> ContemporaryPurple
        op.qadimJadidClass == "قديم" -> QadimAmber
        else -> JadidBlue
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(dotColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(48.dp)
                    .background(dotColor.copy(alpha = 0.35f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            border = if (op.isMutamad) BorderStroke(1.5.dp, MutamadGreen) else null
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(op.scholarCommonName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (op.isMutamad) TagPill("المعتمد", MutamadGreen)
                    if (op.isContemporary) TagPill("معاصر", ContemporaryPurple)
                }
                Text("${op.periodName} (${op.periodHijriRange})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(op.opinionText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ComparisonTable(opinions: List<OpinionDetail>, reasonForDisagreement: String) {
    val hScroll = rememberScrollState()
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.horizontalScroll(hScroll).padding(10.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .background(EmeraldPrimary, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                TableCell("العالم / الطبقة", 130, Color.White, true)
                TableCell("القول", 200, Color.White, true)
                TableCell("الدليل / التعليل", 180, Color.White, true)
                TableCell("المصدر", 160, Color.White, true)
            }
            opinions.forEach { op ->
                val bg = if (op.isMutamad) MutamadGreen.copy(alpha = 0.12f) else Color.Transparent
                val srcSummary = op.sources.joinToString("؛ ") { it.referenceTitle }.ifBlank { "لم يثبت لدينا نص موثق" }
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    TableCell("${op.scholarCommonName}\n(${op.periodName})", 130, MaterialTheme.colorScheme.onSurface, op.isMutamad)
                    TableCell(op.opinionText, 200, MaterialTheme.colorScheme.onSurface, false)
                    TableCell(op.reasoningSummary.ifBlank { reasonForDisagreement }, 180, MaterialTheme.colorScheme.onSurfaceVariant, false)
                    TableCell(srcSummary, 160, MaterialTheme.colorScheme.secondary, false)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TableCell(text: String, widthDp: Int, color: Color, bold: Boolean) {
    Text(
        text = text,
        color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .width(widthDp.dp)
            .padding(horizontal = 6.dp)
    )
}
