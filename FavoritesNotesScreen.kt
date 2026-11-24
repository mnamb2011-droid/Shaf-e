package com.shafii.encyclopedia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.theme.GoldAccent
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("المفضلة والعلامات المرجعية", style = MaterialTheme.typography.titleLarge) }
        if (bookmarks.isEmpty()) {
            item {
                Text(
                    "لم تُضف أي مسألة إلى المفضلة بعد. اضغط على أيقونة الحفظ داخل صفحة أي مسألة.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(bookmarks) { bm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenIssue(bm.issueId) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = GoldAccent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(bm.issueTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(bm.bookTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val notes by viewModel.allNotes.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("الملاحظات والفوائد الشخصية", style = MaterialTheme.typography.titleLarge) }
        if (notes.isEmpty()) {
            item {
                Text(
                    "لا توجد ملاحظات مسجلة بعد. يمكنك تدوين ملاحظاتك داخل أي مسألة فقهية، وتبقى محفوظة حتى عند تحديث قاعدة البيانات.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(notes) { n ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenIssue(n.issueId) }
                        ) {
                            Text(n.issueTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            Text(n.noteText, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { viewModel.deleteNote(n.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }
}
