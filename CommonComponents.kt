package com.shafii.encyclopedia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.data.repository.IssueSummary
import com.shafii.encyclopedia.ui.theme.*
import com.shafii.encyclopedia.util.DatabasePackManager

@Composable
fun VerificationBadge(code: String, label: String, modifier: Modifier = Modifier) {
    val (bgColor, fgColor, icon) = when (code) {
        "verified_primary" -> Triple(Color(0xFFD8F3E5), MutamadGreen, Icons.Default.Verified)
        "verified_secondary" -> Triple(Color(0xFFE0EEF5), JadidBlue, Icons.Default.CheckCircle)
        "disputed_attribution" -> Triple(Color(0xFFFBE4E4), DisputedRed, Icons.Default.Warning)
        "famous_needs_collation" -> Triple(Color(0xFFF9EED2), QadimAmber, Icons.Default.Info)
        else -> Triple(Color(0xFFECECEC), Color(0xFF4A4A4A), Icons.Default.Info)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = fgColor)
        }
    }
}

@Composable
fun TagPill(text: String, bgColor: Color, fgColor: Color = Color.White) {
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            color = fgColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ScholarlySectionCard(
    title: String,
    content: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    highlightBackground: Boolean = false
) {
    val safeText = content.ifBlank { DatabasePackManager.FALLBACK_MISSING_TEXT }
    val isMissing = safeText.trim() == DatabasePackManager.FALLBACK_MISSING_TEXT

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlightBackground)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = safeText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isMissing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun IssueSummaryCard(
    issue: IssueSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${issue.bookTitle} • ${issue.chapterTitle}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = issue.shortRuling,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Spacer(Modifier.height(8.dp))
            VerificationBadge(code = issue.verificationCode, label = issue.verificationLabel)
        }
    }
}
