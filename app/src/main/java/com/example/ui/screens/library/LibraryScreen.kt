package com.example.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ContentType
import com.example.domain.model.LibraryDocument
import com.example.ui.components.DocumentThumbnail
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OcrStatusBadge
import com.example.ui.components.TypeBadge

enum class LibrarySortOption(val titleArabic: String) {
    RECENTLY_ADDED("آخر إضافة"),
    RECENTLY_OPENED("آخر فتح"),
    TITLE("الاسم"),
    PAGE_COUNT("عدد الصفحات"),
    CONTENT_TYPE("النوع")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<LibraryDocument>,
    onDocumentClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    var isGridView by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var selectedSort by remember { mutableStateOf(LibrarySortOption.RECENTLY_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filterOptions = listOf("الكل", "كتب", "مجلات", "مستندات", "مجموعات", "المفضلة")

    // Filter documents
    val filteredDocuments = documents.filter { doc ->
        when (selectedFilter) {
            "كتب" -> doc.type == ContentType.BOOK
            "مجلات" -> doc.type == ContentType.MAGAZINE
            "مستندات" -> doc.type == ContentType.DOCUMENT
            "مجموعات" -> doc.type == ContentType.COLLECTION
            "المفضلة" -> doc.isFavorite
            else -> true
        }
    }.sortedWith { a, b ->
        when (selectedSort) {
            LibrarySortOption.RECENTLY_ADDED -> b.dateAdded.compareTo(a.dateAdded)
            LibrarySortOption.RECENTLY_OPENED -> b.lastOpened.compareTo(a.lastOpened)
            LibrarySortOption.TITLE -> a.title.compareTo(b.title)
            LibrarySortOption.PAGE_COUNT -> b.pageCount.compareTo(a.pageCount)
            LibrarySortOption.CONTENT_TYPE -> a.type.ordinal.compareTo(b.type.ordinal)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "المكتبة الرقمية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "عرض القائمة" else "عرض الشبكة"
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "ترتيب")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            LibrarySortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.titleArabic,
                                            fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedSort = option
                                        showSortMenu = false
                                    }
                                )
                            }
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
        ) {
            // Horizontal Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            // Document Count & Current Sort Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredDocuments.size} عنصر في القائمة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ترتيب: ${selectedSort.titleArabic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Empty state if list is empty
            if (filteredDocuments.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderSpecial,
                    title = "مكتبتك فارغة",
                    description = "لا توجد وثائق مطابقة للتصنيف المحدد. ابدأ بتصوير مستند جديد أو استيراد ملف.",
                    primaryButtonText = "📷 تصوير",
                    onPrimaryButtonClick = onNavigateToScan,
                    secondaryButtonText = "📁 استيراد",
                    onSecondaryButtonClick = onNavigateToImport,
                    modifier = Modifier.weight(1f)
                )
            } else if (isGridView) {
                // Grid View Layout
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        LibraryGridItem(
                            document = doc,
                            onClick = { onDocumentClick(doc.id) },
                            onFavoriteClick = { onToggleFavorite(doc.id) }
                        )
                    }
                }
            } else {
                // List View Layout
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        LibraryListItem(
                            document = doc,
                            onClick = { onDocumentClick(doc.id) },
                            onFavoriteClick = { onToggleFavorite(doc.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryListItem(
    document: LibraryDocument,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentThumbnail(
                pageNumber = document.lastReadPage,
                imagePath = document.thumbnailPath,
                modifier = Modifier.width(68.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeBadge(type = document.type)
                    OcrStatusBadge(status = document.ocrStatus)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (document.author != null) {
                    Text(
                        text = document.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${document.pageCount} صفحة • أُضيف: ${document.dateAdded}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "المفضلة",
                    tint = if (document.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun LibraryGridItem(
    document: LibraryDocument,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                DocumentThumbnail(
                    pageNumber = document.lastReadPage,
                    imagePath = document.thumbnailPath,
                    modifier = Modifier.fillMaxWidth()
                )
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "المفضلة",
                        tint = if (document.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TypeBadge(type = document.type)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = document.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            OcrStatusBadge(status = document.ocrStatus)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${document.pageCount} صفحة",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
