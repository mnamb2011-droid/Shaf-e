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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.components.VerificationBadge
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun BooksScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val books by viewModel.books.collectAsState()
    val chaptersMap by viewModel.chaptersByBook.collectAsState()
    var expandedBookId by remember { mutableStateOf<Int?>(1) }

    LaunchedEffect(expandedBookId) {
        expandedBookId?.let { viewModel.loadChaptersForBook(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("أبواب الفقه الشافعي (الهيكل الكامل)", style = MaterialTheme.typography.titleLarge)
            Text(
                "اضغط على أي كتاب لاستعراض أبوابه ومسائله الموثقة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
        }

        items(books) { book ->
            val isExpanded = expandedBookId == book.id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedBookId = if (isExpanded) null else book.id
                                if (!isExpanded) viewModel.loadChaptersForBook(book.id)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${book.sortOrder}. ${book.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${book.chaptersCount} أبواب • ${book.issuesCount} مسائل موثقة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        val chapters = chaptersMap[book.id].orEmpty()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(book.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            chapters.forEach { chap ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(chap.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        if (chap.issues.isEmpty()) {
                                            Text(
                                                "الباب مهيأ لاستقبال المسائل في المراحل القادمة (Stage 2–4).",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Spacer(Modifier.height(6.dp))
                                            chap.issues.forEach { issue ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clickable { onOpenIssue(issue.id) },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Column(Modifier.padding(10.dp)) {
                                                        Text(issue.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                                        Spacer(Modifier.height(4.dp))
                                                        VerificationBadge(issue.verificationCode, issue.verificationLabel)
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
            }
        }
    }
}
