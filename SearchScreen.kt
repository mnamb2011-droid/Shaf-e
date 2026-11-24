package com.shafii.encyclopedia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.data.repository.SearchScope
import com.shafii.encyclopedia.ui.components.IssueSummaryCard
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onOpenIssue: (Int) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val scope by viewModel.searchScope.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val history by viewModel.searchHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("البحث الشامل (FTS5 — بدون تشكيل)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearch(it, scope, recordToHistory = false) },
            placeholder = { Text("مثال: وقت المغرب، لحم الإبل، النووي، المنهاج...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { viewModel.updateSearch("", scope) }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SearchScope.entries) { s ->
                FilterChip(
                    selected = scope == s,
                    onClick = { viewModel.updateSearch(query, s, recordToHistory = query.isNotBlank()) },
                    label = { Text(s.labelAr) }
                )
            }
        }

        if (query.isBlank() && history.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سجل البحث", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { viewModel.clearSearchHistory() }) { Text("مسح السجل") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item ->
                    AssistChip(
                        onClick = { viewModel.updateSearch(item.queryText, scope) },
                        label = { Text(item.queryText) },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "النتائج: ${results.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))

        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لم يُعثر على نتائج مطابقة في الحزمة الحالية.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results) { issue ->
                    IssueSummaryCard(issue = issue, onClick = {
                        if (query.isNotBlank()) viewModel.updateSearch(query, scope, recordToHistory = true)
                        onOpenIssue(issue.id)
                    })
                }
            }
        }
    }
}
