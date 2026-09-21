package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

/**
 * Clean architectural page thumbnail representation
 */
@Composable
fun DocumentThumbnail(
    pageNumber: Int,
    modifier: Modifier = Modifier,
    imagePath: String? = null,
    rotationDegrees: Int = 0,
    onRotateClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    label: String? = null
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .background(Color(0xFFF6F3EC))
                .rotate(rotationDegrees.toFloat())
        ) {
            val hasValidImage = !imagePath.isNullOrBlank() && File(imagePath).exists()
            if (hasValidImage) {
                AsyncImage(
                    model = File(imagePath!!),
                    contentDescription = label ?: "صفحة $pageNumber",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Simulated archival document content lines
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Header line
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = Color(0xFFC7BCAB),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(6.dp)
                    ) {}
                    Spacer(modifier = Modifier.height(2.dp))
                    repeat(7) { idx ->
                        val widthFraction = when (idx) {
                            2 -> 0.75f
                            4 -> 0.85f
                            6 -> 0.5f
                            else -> 0.95f
                        }
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFFDDD5C7),
                            modifier = Modifier
                                .fillMaxWidth(widthFraction)
                                .height(4.dp)
                        ) {}
                    }
                }
            }

            // Page Number Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = label ?: "صفحة $pageNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Optional rotate & delete action buttons in corners
            if (onRotateClick != null || onDeleteClick != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onRotateClick != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            IconButton(onClick = onRotateClick, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "تدوير",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (onDeleteClick != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            IconButton(onClick = onDeleteClick, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
