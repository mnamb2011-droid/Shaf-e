package com.shafii.encyclopedia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel
import com.shafii.encyclopedia.util.IssueExporter
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val darkTheme by viewModel.darkTheme.collectAsState()
    val readerConfig by viewModel.readerConfig.collectAsState()
    val report by viewModel.integrityReport.collectAsState()
    val metadata by viewModel.metadata.collectAsState()

    val importPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPack(uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("إعدادات التطبيق وإدارة قاعدة البيانات", style = MaterialTheme.typography.titleLarge) }

        // Reading & Display Settings
        item {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("العرض والقراءة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الوضع الليلي (Dark Mode)")
                        Switch(checked = darkTheme, onCheckedChange = { viewModel.setDarkTheme(it) })
                    }

                    Text("حجم الخط: ${(readerConfig.fontScale * 100).roundToInt()}%")
                    Slider(
                        value = readerConfig.fontScale,
                        onValueChange = { viewModel.setFontScale(it) },
                        valueRange = 0.85f..1.55f
                    )

                    Text("تباعد الأسطر: ${String.format("%.2f", readerConfig.lineSpacingMultiplier)}")
                    Slider(
                        value = readerConfig.lineSpacingMultiplier,
                        onValueChange = { viewModel.setLineSpacing(it) },
                        valueRange = 1.25f..2.0f
                    )
                }
            }
        }

        // Database Version, Size & Integrity Checker
        item {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إدارة قاعدة البيانات المحلية (Offline-First)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (report != null) {
                        val kb = report!!.databaseSizeBytes / 1024.0
                        Text("إصدار المحتوى: ${report!!.contentVersion}")
                        Text("المرحلة: ${report!!.contentStage}")
                        Text("حجم قاعدة البيانات النشطة: ${String.format("%.1f", kb)} كيلوبايت")
                        Text("حالة السلامة: ${if (report!!.isHealthy) "سليمة (OK)" else report!!.integrityMessage}")
                        Text("الإحصاء: ${report!!.booksCount} كتاباً • ${report!!.issuesCount} مسألة • ${report!!.opinionsCount} قولاً • ${report!!.scholarsCount} عالماً • ${report!!.referencesCount} مصدراً")
                    }
                    if (metadata["editorial_policy"] != null) {
                        Text(
                            "المنهجية التحريرية: ${metadata["editorial_policy"]}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.runIntegrityCheckNow() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تشغيل فحص سلامة قاعدة البيانات (PRAGMA)")
                    }

                    OutlinedButton(
                        onClick = { importPackLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("استيراد حزمة قاعدة بيانات (.db أو .db.gz)")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.exportBackup { file ->
                                IssueExporter.shareFile(context, file, "application/gzip")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تصدير نسخة احتياطية مضغوطة (.db.gz)")
                    }

                    TextButton(
                        onClick = { viewModel.resetToStarterDatabase() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("استعادة قاعدة البيانات التأسيسية المدمجة")
                    }
                }
            }
        }
    }
}
