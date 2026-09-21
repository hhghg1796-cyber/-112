package com.example.ui.screens.processing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.engine.DocumentProcessor
import com.example.domain.model.ContentType
import com.example.ui.components.TypeBadge
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingPipelineScreen(
    documentTitle: String,
    contentType: ContentType,
    pageCount: Int,
    onExecutePipeline: (suspend () -> String)? = null,
    onOpenReader: (docId: String?) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onBackClick: () -> Unit
) {
    val steps = listOf(
        DocumentProcessor.PipelineStep.DESKEW,
        DocumentProcessor.PipelineStep.ENHANCEMENT,
        DocumentProcessor.PipelineStep.LAYOUT_ANALYSIS,
        DocumentProcessor.PipelineStep.OCR,
        DocumentProcessor.PipelineStep.SAVE
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var createdDocId by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val isComplete = currentStepIndex >= steps.size

    LaunchedEffect(Unit) {
        if (onExecutePipeline != null) {
            currentStepIndex = 0
            delay(400)
            currentStepIndex = 1
            delay(400)
            currentStepIndex = 2
            delay(400)
            currentStepIndex = 3
            delay(500)
            currentStepIndex = 4
            val docId = try {
                onExecutePipeline()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            createdDocId = docId
            delay(300)
            currentStepIndex = 5
        } else {
            while (currentStepIndex < steps.size) {
                delay(600)
                currentStepIndex++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isComplete) "اكتملت المعالجة والأرشفة" else "معالجة وأرشفة المستند",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isComplete) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Document Info Summary Header
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = documentTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$pageCount صفحات ممسوحة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TypeBadge(type = contentType)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Overall Progress Indicator
                val progress = if (isComplete) 1f else (currentStepIndex.toFloat() / steps.size.toFloat())
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Steps List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    steps.forEachIndexed { index, step ->
                        val isDone = index < currentStepIndex
                        val isCurrent = index == currentStepIndex

                        PipelineStepRow(
                            stepNumber = index + 1,
                            title = step.titleArabic,
                            description = step.descriptionArabic,
                            isDone = isDone,
                            isCurrent = isCurrent
                        )
                    }
                }
            }

            // Bottom Actions when Complete
            if (isComplete) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onOpenReader(createdDocId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح المستند في القارئ")
                    }
                    OutlinedButton(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("العودة إلى المكتبة")
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "يتم تنفيذ مراحل المعالجة محلياً بالكامل لضمان خصوصية الوثائق وسرية المحتوى.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineStepRow(
    stepNumber: Int,
    title: String,
    description: String,
    isDone: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step Status Icon
        Surface(
            shape = CircleShape,
            color = when {
                isDone -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    isDone -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تم",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    isCurrent -> CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    else -> Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isDone -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
