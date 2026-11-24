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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun ScholarsScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val scholars by viewModel.scholars.collectAsState()
    var expandedId by remember { mutableStateOf<Int?>(1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("أئمة وعلماء المذهب الشافعي", style = MaterialTheme.typography.titleLarge)
            Text("التراجم العلمية الموثقة حسب الطبقات التاريخية.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(scholars) { s ->
            val expanded = expandedId == s.id
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedId = if (expanded) null else s.id }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.commonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${s.periodName} • ت ${s.deathYearHijri}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("الاسم الكامل: ${s.fullName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("المولد والوفاة: ${s.birthYearHijri} – ${s.deathYearHijri}", style = MaterialTheme.typography.bodyMedium)
                            Text("الشيوخ: ${s.teachers}", style = MaterialTheme.typography.bodyMedium)
                            Text("التلاميذ: ${s.students}", style = MaterialTheme.typography.bodyMedium)
                            Text("الإسهام العلمي: ${s.contributions}", style = MaterialTheme.typography.bodyMedium)
                            Text("أبرز المصنفات الشافعية: ${s.bibliography}", style = MaterialTheme.typography.bodyMedium)
                            if (s.notes.isNotBlank()) {
                                Text("تنبيه منهجي: ${s.notes}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (s.documentedIssues.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text("المسائل الموثق فيها قوله (${s.documentedIssues.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                s.documentedIssues.forEach { (issueId, issueTitle) ->
                                    Text(
                                        text = "• $issueTitle",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .clickable { onOpenIssue(issueId) }
                                            .padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
