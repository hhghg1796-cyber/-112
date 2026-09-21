package com.example.ui.screens.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ContentType
import com.example.domain.model.LibraryDocument
import com.example.domain.model.OcrStatus
import com.example.ui.components.OcrStatusBadge
import com.example.ui.components.TypeBadge

enum class ReaderViewMode(val titleArabic: String) {
    ORIGINAL_PAGE("النسخة المصورة"),
    EXTRACTED_OCR("النص المستخرج")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    document: LibraryDocument,
    initialPage: Int = 1,
    onAddBookmark: (page: Int, title: String, note: String) -> Unit,
    onNavigateToExport: () -> Unit,
    onBackClick: () -> Unit
) {
    var currentPageNumber by remember { mutableIntStateOf(initialPage.coerceIn(1, document.pageCount)) }
    var viewMode by remember { mutableStateOf(ReaderViewMode.ORIGINAL_PAGE) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    val currentPage = document.pages.getOrNull(currentPageNumber - 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${document.type.titleArabic} • صفحة $currentPageNumber من ${document.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showBookmarkDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "إضافة علامة قراءة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToExport) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "تصدير المستند"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Page Scrubbing Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صفحة $currentPageNumber",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = currentPageNumber.toFloat(),
                            onValueChange = { currentPageNumber = it.toInt() },
                            valueRange = 1f..document.pageCount.toFloat(),
                            steps = maxOf(0, document.pageCount - 2),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "${document.pageCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Navigation Actions and Zoom Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Page (Next in RTL)
                        Button(
                            onClick = {
                                if (currentPageNumber > 1) currentPageNumber--
                            },
                            enabled = currentPageNumber > 1
                        ) {
                            Text("السابقة")
                        }

                        // Zoom Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.8f) }) {
                                Icon(Icons.Default.Remove, contentDescription = "تصغير")
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(3.0f) }) {
                                Icon(Icons.Default.Add, contentDescription = "تكبير")
                            }
                            if (zoomScale != 1f) {
                                IconButton(onClick = { zoomScale = 1f }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "إعادة التعيين")
                                }
                            }
                        }

                        // Next Page (Previous in RTL)
                        Button(
                            onClick = {
                                if (currentPageNumber < document.pageCount) currentPageNumber++
                            },
                            enabled = currentPageNumber < document.pageCount
                        ) {
                            Text("التالية")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // View Mode Selector (Original vs OCR Text)
            TabRow(
                selectedTabIndex = viewMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReaderViewMode.entries.forEach { mode ->
                    Tab(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        text = {
                            Text(
                                text = mode.titleArabic,
                                fontWeight = if (viewMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Reader Display Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDE9DF))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (viewMode) {
                    ReaderViewMode.ORIGINAL_PAGE -> {
                        // Original Document View with Zoom/Pan
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF8F2)),
                            border = BorderStroke(1.dp, Color(0xFFD4C8B5)),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(0.72f)
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.0f)
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Header of facsimile page
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = document.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF7D7260)
                                    )
                                    Text(
                                        text = "صفحة $currentPageNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7D7260)
                                    )
                                }

                                // Magazine multi-column layout detection representation
                                if (document.type == ContentType.MAGAZINE && currentPage?.detectedSections?.isNotEmpty() == true) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        currentPage.detectedSections.forEach { sec ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color(0xFF65D5B3), RoundedCornerShape(6.dp))
                                                    .background(Color(0x1865D5B3))
                                                    .padding(8.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "📰 ${sec.title}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E5647)
                                                    )
                                                    Text(
                                                        text = sec.ocrText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF333333),
                                                        maxLines = 2
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Manuscript / Document Body Lines
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = currentPage?.ocrText?.ifBlank {
                                                "صفحة $currentPageNumber من وثيقة «${document.title}». نص الوثيقة التاريخية المحفوظ في الأرشيف المحلي بدقة عالية."
                                            } ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 26.sp),
                                            color = Color(0xFF2C2822),
                                            textAlign = TextAlign.Justify
                                        )
                                    }
                                }

                                // Footnote / Archival Stamp
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "دار الكتب والوثائق — نسخة رقمية مصدقة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9E927E)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0x33A67C52),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "مفهرس",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6E4E2C),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ReaderViewMode.EXTRACTED_OCR -> {
                        // Clean Text View Mode (like an ebook / article)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "النص المستخرج — صفحة $currentPageNumber",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OcrStatusBadge(status = currentPage?.ocrStatus ?: OcrStatus.NOT_PROCESSED)
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                val textContent = currentPage?.ocrText?.ifBlank {
                                    "لم يتم استخراج النص لهذه الصفحة بعد. يمكنك تشغيل محرك OCR المحلي لاستخراج الحروف والكلمات."
                                } ?: ""

                                Text(
                                    text = textContent,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showBookmarkDialog) {
        var bookmarkTitle by remember { mutableStateOf("إشارة صفحة $currentPageNumber") }
        var bookmarkNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = {
                Text("إضافة إشارة مرجعية", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("حفظ علامة قراءة للصفحة $currentPageNumber من «${document.title}»")
                    OutlinedTextField(
                        value = bookmarkTitle,
                        onValueChange = { bookmarkTitle = it },
                        label = { Text("عنوان الإشارة") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        label = { Text("ملاحظة علمية أو تعليق (اختياري)") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddBookmark(currentPageNumber, bookmarkTitle, bookmarkNote)
                        showBookmarkDialog = false
                    }
                ) {
                    Text("حفظ الإشارة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
