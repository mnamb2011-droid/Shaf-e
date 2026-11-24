package com.shafii.encyclopedia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.components.IssueSummaryCard
import com.shafii.encyclopedia.ui.navigation.AppDestination
import com.shafii.encyclopedia.ui.theme.EmeraldPrimary
import com.shafii.encyclopedia.ui.theme.GoldAccent
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit,
    onOpenIssue: (Int) -> Unit,
    onGlobalSearch: (String) -> Unit
) {
    val issues by viewModel.allIssues.collectAsState()
    val history by viewModel.recentHistory.collectAsState()
    val report by viewModel.integrityReport.collectAsState()
    var quickQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner & Global Search Bar
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(EmeraldPrimary, Color(0xFF0A3327))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = "الموسوعة الشافعية الشاملة",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "نظام معرفي فقهي محلي موثق لتتبع تطور الأقوال من القديم والجديد إلى المعتمد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD8EFE6)
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = quickQuery,
                        onValueChange = { quickQuery = it },
                        placeholder = { Text("ابحث باسم المسألة أو العالم أو الكتاب...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (quickQuery.isNotBlank()) {
                                IconButton(onClick = { onGlobalSearch(quickQuery) }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "بحث")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF1A211E),
                            unfocusedTextColor = Color(0xFF1A211E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onGlobalSearch(quickQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("بحث شامل (دون إنترنت)", color = Color.White)
                    }
                }
            }
        }

        // Offline Database Stats Strip
        if (report != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip("الأبواب", "${report!!.booksCount}", Modifier.weight(1f))
                    StatChip("المسائل", "${report!!.issuesCount}", Modifier.weight(1f))
                    StatChip("الأقوال", "${report!!.opinionsCount}", Modifier.weight(1f))
                    StatChip("العلماء", "${report!!.scholarsCount}", Modifier.weight(1f))
                }
            }
        }

        // Quick Access Grid to All Sections
        item {
            Text("أقسام الموسوعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile("أبواب الفقه", Icons.Default.MenuBook, Modifier.weight(1f)) { onNavigate(AppDestination.BOOKS) }
                QuickTile("المقارنة", Icons.Default.CompareArrows, Modifier.weight(1f)) { onNavigate(AppDestination.COMPARISON) }
                QuickTile("العلماء", Icons.Default.People, Modifier.weight(1f)) { onNavigate(AppDestination.SCHOLARS) }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile("المكتبة والمصادر", Icons.Default.LibraryBooks, Modifier.weight(1f)) { onNavigate(AppDestination.LIBRARY) }
                QuickTile("المفضلة", Icons.Default.Bookmark, Modifier.weight(1f)) { onNavigate(AppDestination.FAVORITES) }
                QuickTile("الملاحظات", Icons.Default.EditNote, Modifier.weight(1f)) { onNavigate(AppDestination.NOTES) }
            }
        }

        // Recently Viewed Issues
        if (history.isNotEmpty()) {
            item {
                Text("المسائل المشاهدة مؤخراً", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history.take(8)) { h ->
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onOpenIssue(h.issueId) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(h.bookTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(4.dp))
                                Text(h.issueTitle, style = MaterialTheme.typography.bodyMedium, maxLines = 2, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Featured Verified Issues
        item {
            Text("مسائل تأسيسية موثقة (القديم والجديد والمعتمد)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(issues.take(6)) { issue ->
            IssueSummaryCard(issue = issue, onClick = { onOpenIssue(issue.id) })
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickTile(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
