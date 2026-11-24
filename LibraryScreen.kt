package com.shafii.encyclopedia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.components.TagPill
import com.shafii.encyclopedia.ui.theme.GoldAccent
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val references by viewModel.references.collectAsState()
    val terminology by viewModel.terminology.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedRefId by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("المكتبة والمصادر (${references.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("مصطلحات المذهب (${terminology.size})") })
        }

        if (selectedTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(references) { r ->
                    val expanded = expandedRefId == r.id
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedRefId = if (expanded) null else r.id }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(r.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                }
                                TagPill(r.bookType, GoldAccent)
                                Spacer(Modifier.width(4.dp))
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                            }
                            AnimatedVisibility(visible = expanded) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("الموضوع: ${r.subject}", style = MaterialTheme.typography.bodyMedium)
                                    Text("تاريخ التأليف: ${r.authorshipDate}", style = MaterialTheme.typography.bodyMedium)
                                    Text("حالة الطباعة: ${r.printStatus}", style = MaterialTheme.typography.bodyMedium)
                                    Text("حالة المخطوطات: ${r.manuscriptStatus}", style = MaterialTheme.typography.bodyMedium)
                                    if (r.notes.isNotBlank()) Text("ملاحظات: ${r.notes}", style = MaterialTheme.typography.bodyMedium)

                                    if (r.editions.isNotEmpty()) {
                                        Text("الطبعات الموثقة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        r.editions.forEach { ed ->
                                            Text("• ${ed.publisher} — تحقيق: ${ed.editor} (${ed.volumesCount})", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (r.manuscripts.isNotEmpty()) {
                                        Text("سجلات المخطوطات:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        r.manuscripts.forEach { ms ->
                                            Text("• ${ms.libraryName} (${ms.catalogNumber}) — [${ms.verificationLabel}]", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (r.citedInIssues.isNotEmpty()) {
                                        Text("المسائل المحالة على هذا الكتاب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        r.citedInIssues.forEach { (id, title) ->
                                            Text(
                                                "• $title",
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .clickable { onOpenIssue(id) }
                                                    .padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(terminology) { t ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(t.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(t.definition, style = MaterialTheme.typography.bodyLarge)
                            if (t.synonyms.isNotBlank()) {
                                Text("مرادفات/دلالات: ${t.synonyms}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text("المصدر: ${t.sourceNote}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
