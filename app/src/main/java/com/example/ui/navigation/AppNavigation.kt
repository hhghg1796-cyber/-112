package com.example.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.ContentType
import com.example.domain.model.FileImportItem
import com.example.domain.model.ScannedPage
import com.example.domain.repository.LibraryRepository
import com.example.ui.screens.export.PdfExportScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.importflow.FileImportScreen
import com.example.ui.screens.importflow.ImageImportScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.ocr.OcrScreen
import com.example.ui.screens.processing.ProcessingPipelineScreen
import com.example.ui.screens.reader.DocumentDetailsScreen
import com.example.ui.screens.reader.ReaderScreen
import com.example.ui.screens.review.DocumentReviewScreen
import com.example.ui.screens.scanner.ScannerEntryScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun MainApp(
    repository: LibraryRepository,
    navController: NavHostController = rememberNavController()
) {
    val documents by repository.documents.collectAsState(initial = emptyList())
    val bookmarks by repository.bookmarks.collectAsState(initial = emptyList())
    val scannedPages by repository.activeScanPages.collectAsState(initial = emptyList())
    val importFiles by repository.activeImportFiles.collectAsState(initial = emptyList())

    var activeProcessingType by remember { mutableStateOf(ContentType.DOCUMENT) }
    var pendingDocumentTitle by remember { mutableStateOf("وثيقة ممسوحة جديدة") }
    var pendingAuthor by remember { mutableStateOf("") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show Bottom Bar only on primary top-level tabs
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.Favorites.route,
        Screen.Search.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.titleArabic)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.titleArabic,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            // TAB 1: Home
            composable(Screen.Home.route) {
                HomeScreen(
                    documents = documents,
                    onNavigateToScan = { navController.navigate(Screen.ScannerEntry.route) },
                    onNavigateToFileImport = { navController.navigate(Screen.FileImport.route) },
                    onNavigateToImageImport = { navController.navigate(Screen.ImageImport.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onDocumentClick = { docId ->
                        navController.navigate(Screen.DocumentDetails.createRoute(docId))
                    },
                    onResumeReadingClick = { docId ->
                        navController.navigate(Screen.Reader.createRoute(docId))
                    }
                )
            }

            // TAB 2: Library
            composable(Screen.Library.route) {
                LibraryScreen(
                    documents = documents,
                    onDocumentClick = { docId ->
                        navController.navigate(Screen.DocumentDetails.createRoute(docId))
                    },
                    onToggleFavorite = { docId -> repository.toggleFavorite(docId) },
                    onNavigateToScan = { navController.navigate(Screen.ScannerEntry.route) },
                    onNavigateToImport = { navController.navigate(Screen.FileImport.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                )
            }

            // TAB 3: Favorites & Bookmarks
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    favoriteDocuments = documents.filter { it.isFavorite },
                    bookmarks = bookmarks,
                    onDocumentClick = { docId ->
                        navController.navigate(Screen.DocumentDetails.createRoute(docId))
                    },
                    onBookmarkClick = { docId, page ->
                        repository.updateLastReadPage(docId, page)
                        navController.navigate(Screen.Reader.createRoute(docId))
                    },
                    onToggleFavorite = { docId -> repository.toggleFavorite(docId) },
                    onRemoveBookmark = { bmId -> repository.removeBookmark(bmId) },
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) }
                )
            }

            // TAB 4: Search
            composable(Screen.Search.route) {
                SearchScreen(
                    repository = repository,
                    onResultClick = { docId, page ->
                        repository.updateLastReadPage(docId, page)
                        navController.navigate(Screen.Reader.createRoute(docId))
                    }
                )
            }

            // TAB 5: Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // SCANNER ENTRY & LIVE SESSION
            composable(Screen.ScannerEntry.route) {
                ScannerEntryScreen(
                    scannedPages = scannedPages,
                    onCapturePage = {
                        val newPage = ScannedPage(
                            id = "scan_${System.currentTimeMillis()}",
                            pageNumber = scannedPages.size + 1
                        )
                        repository.addScannedPage(newPage)
                    },
                    onRemovePage = { id -> repository.removeScannedPage(id) },
                    onRotatePage = { id -> repository.rotateScannedPage(id) },
                    onGalleryShortcutClick = { navController.navigate(Screen.ImageImport.route) },
                    onProceedToReview = { navController.navigate(Screen.Review.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // FILE IMPORT FLOW
            composable(Screen.FileImport.route) {
                val filePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    val newFiles = uris.mapIndexed { idx, uri ->
                        val path = uri.path ?: ""
                        val name = path.substringAfterLast('/').ifBlank { "document_${idx + 1}.pdf" }
                        FileImportItem(
                            id = "file_${System.currentTimeMillis()}_$idx",
                            fileName = name,
                            fileExtension = name.substringAfterLast('.', "PDF").uppercase(),
                            sizeFormatted = "ملف محلي",
                            pageCountEstimate = 1
                        )
                    }
                    if (newFiles.isNotEmpty()) {
                        repository.addImportFiles(newFiles)
                    }
                }

                FileImportScreen(
                    files = importFiles,
                    selectedType = activeProcessingType,
                    onTypeChanged = { activeProcessingType = it },
                    onAddMoreFiles = {
                        filePicker.launch(arrayOf("application/pdf", "image/*", "*/*"))
                    },
                    onRemoveFile = { id -> repository.removeImportFile(id) },
                    onProceed = { navController.navigate(Screen.Review.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // IMAGE IMPORT FLOW
            composable(Screen.ImageImport.route) {
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    scope.launch {
                        uris.forEachIndexed { index, uri ->
                            val pageNum = scannedPages.size + index + 1
                            val savedPaths = repository.getStorageManager()?.importFromUri(
                                sessionId = "session_${System.currentTimeMillis()}",
                                uri = uri,
                                pageNumber = pageNum
                            )
                            val newPage = ScannedPage(
                                id = "img_${System.currentTimeMillis()}_$index",
                                imageUri = savedPaths?.first ?: uri.toString(),
                                imagePath = savedPaths?.first,
                                thumbnailPath = savedPaths?.second,
                                pageNumber = pageNum
                            )
                            repository.addScannedPage(newPage)
                        }
                    }
                }

                ImageImportScreen(
                    images = scannedPages,
                    selectedType = activeProcessingType,
                    onTypeChanged = { activeProcessingType = it },
                    onAddMoreImages = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = { id -> repository.removeScannedPage(id) },
                    onRotateImage = { id -> repository.rotateScannedPage(id) },
                    onProceed = { navController.navigate(Screen.Review.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // DOCUMENT REVIEW BEFORE PROCESSING
            composable(Screen.Review.route) {
                DocumentReviewScreen(
                    pages = scannedPages,
                    selectedType = activeProcessingType,
                    onTypeChanged = { activeProcessingType = it },
                    onAddPages = { navController.navigate(Screen.ScannerEntry.route) },
                    onRotatePage = { id -> repository.rotateScannedPage(id) },
                    onDeletePage = { id -> repository.removeScannedPage(id) },
                    onProceedToProcessing = { title, author ->
                        pendingDocumentTitle = title
                        pendingAuthor = author
                        navController.navigate(Screen.Processing.route)
                    },
                    onCancel = {
                        repository.clearScanSession()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // PROCESSING PIPELINE (Multi-step verification)
            composable(Screen.Processing.route) {
                ProcessingPipelineScreen(
                    documentTitle = pendingDocumentTitle,
                    contentType = activeProcessingType,
                    pageCount = scannedPages.size.coerceAtLeast(1),
                    onExecutePipeline = {
                        val newDocId = repository.saveScannedPagesAsDocument(
                            title = pendingDocumentTitle,
                            type = activeProcessingType,
                            author = pendingAuthor.ifBlank { null },
                            description = null
                        )
                        try {
                            repository.performOcrForPage(newDocId, 1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        newDocId
                    },
                    onOpenReader = { docId ->
                        val targetId = docId ?: documents.firstOrNull()?.id
                        if (targetId != null) {
                            navController.navigate(Screen.Reader.createRoute(targetId)) {
                                popUpTo(Screen.Home.route)
                            }
                        } else {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // DOCUMENT DETAILS
            composable(
                route = Screen.DocumentDetails.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                val doc = repository.getDocumentById(docId)
                if (doc != null) {
                    DocumentDetailsScreen(
                        document = doc,
                        onOpenReader = { page ->
                            repository.updateLastReadPage(docId, page)
                            navController.navigate(Screen.Reader.createRoute(docId))
                        },
                        onOpenOcr = { page ->
                            navController.navigate(Screen.OcrResult.createRoute(docId, page))
                        },
                        onOpenExport = {
                            navController.navigate(Screen.PdfExport.createRoute(docId))
                        },
                        onToggleFavorite = { repository.toggleFavorite(docId) },
                        onDeleteDocument = {
                            repository.deleteDocument(docId)
                            navController.popBackStack()
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // READER SCREEN
            composable(
                route = Screen.Reader.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                val doc = repository.getDocumentById(docId)
                if (doc != null) {
                    ReaderScreen(
                        document = doc,
                        initialPage = doc.lastReadPage,
                        onAddBookmark = { page, title, note ->
                            repository.addBookmark(docId, page, title, note)
                        },
                        onNavigateToExport = {
                            navController.navigate(Screen.PdfExport.createRoute(docId))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // OCR EXTRACTED TEXT SCREEN
            composable(
                route = Screen.OcrResult.route,
                arguments = listOf(
                    navArgument("docId") { type = NavType.StringType },
                    navArgument("page") { type = NavType.IntType; defaultValue = 1 }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                val page = backStackEntry.arguments?.getInt("page") ?: 1
                val doc = repository.getDocumentById(docId)
                if (doc != null) {
                    OcrScreen(
                        document = doc,
                        pageNumber = page,
                        onPerformOcr = { pageNum ->
                            repository.performOcrForPage(docId, pageNum)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // PDF EXPORT SCREEN
            composable(
                route = Screen.PdfExport.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                val doc = repository.getDocumentById(docId)
                if (doc != null) {
                    PdfExportScreen(
                        document = doc,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
