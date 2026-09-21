package com.example.ui.screens.ocr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.LibraryDocument
import com.example.domain.model.OcrStatus
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OcrStatusBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    document: LibraryDocument,
    pageNumber: Int = 1,
    onPerformOcr: (suspend (pageNumber: Int) -> String)? = null,
    onBackClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val initialPage = document.pages.getOrNull(pageNumber - 1)
    var currentText by remember(document.id, pageNumber) {
        mutableStateOf(initialPage?.ocrText ?: "")
    }

    var internalOcrStatus by remember(document.id, pageNumber) {
        mutableStateOf(
            if (currentText.isNotBlank()) OcrStatus.COMPLETED
            else (initialPage?.ocrStatus ?: document.ocrStatus)
        )
    }

    var localSearchQuery by remember { mutableStateOf("") }

    val triggerOcr: () -> Unit = {
        internalOcrStatus = OcrStatus.PROCESSING
        scope.launch {
            try {
                val result = onPerformOcr?.invoke(pageNumber) ?: ""
                if (result.isNotBlank()) {
                    currentText = result
                    internalOcrStatus = OcrStatus.COMPLETED
                    snackbarHostState.showSnackbar("تم استخراج النص بنجاح بواسطة محرك OCR المحلي")
                } else {
                    if (currentText.isBlank()) {
                        internalOcrStatus = OcrStatus.COMPLETED
                        snackbarHostState.showSnackbar("اكتمل التحليل: لم يتم اكتشاف نص واضح في الصفحة")
                    } else {
                        internalOcrStatus = OcrStatus.COMPLETED
                    }
                }
            } catch (e: Exception) {
                internalOcrStatus = OcrStatus.FAILED
                snackbarHostState.showSnackbar("تعذر معالجة النص: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(pageNumber, document.id) {
        if (currentText.isBlank() && onPerformOcr != null) {
            triggerOcr()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "استخراج النص (OCR)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${document.title} • صفحة $pageNumber",
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
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentText))
                            scope.launch {
                                snackbarHostState.showSnackbar("تم نسخ النص المستخرج إلى الحافظة")
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ النص")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = triggerOcr,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة الاستخراج")
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentText))
                            scope.launch {
                                snackbarHostState.showSnackbar("تم نسخ النص إلى الحافظة")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ النص")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Status Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "حالة التعرف الضوئي على الحروف:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = internalOcrStatus.descriptionArabic,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OcrStatusBadge(status = internalOcrStatus)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (internalOcrStatus) {
                OcrStatus.PROCESSING -> {
                    LoadingStateView(
                        title = "جارٍ استخراج النص العربي…",
                        subtitle = "يقوم المحرك المحلي بتحليل الكلمات والتشكيل والحواشي دون اتصال بالإنترنت",
                        modifier = Modifier.weight(1f)
                    )
                }
                OcrStatus.FAILED -> {
                    ErrorStateView(
                        message = "تعذر استخراج النص من هذه الصفحة",
                        technicalDetails = "قد تكون الصورة بحاجة لإعادة التصوير بإضاءة أوضح أو ضبط الحواف الرباعية.",
                        onRetry = triggerOcr,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    // Search in text field
                    OutlinedTextField(
                        value = localSearchQuery,
                        onValueChange = { localSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث داخل النص المستخرج في هذه الصفحة…") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Content Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = currentText.ifBlank {
                                    "لم يتم استخراج النص بعد لهذه الصفحة. انقر على «إعادة الاستخراج» لتشغيل محرك OCR المحلي."
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
