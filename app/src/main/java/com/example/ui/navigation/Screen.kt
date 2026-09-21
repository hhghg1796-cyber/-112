package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleArabic: String, val icon: ImageVector? = null) {
    // Primary Bottom Navigation Tabs
    object Home : Screen("home", "الرئيسية", Icons.Default.Home)
    object Library : Screen("library", "المكتبة", Icons.Default.Folder)
    object Favorites : Screen("favorites", "المفضلة", Icons.Default.Star)
    object Search : Screen("search", "البحث", Icons.Default.Search)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)

    // Scanner & Import Workflows
    object ScannerEntry : Screen("scanner_entry", "تصوير بالكاميرا")
    object ScanSession : Screen("scan_session", "جلسة المسح")
    object FileImport : Screen("file_import", "استيراد من الملفات")
    object ImageImport : Screen("image_import", "استيراد صورة")
    object Review : Screen("review", "مراجعة المستند")
    object Processing : Screen("processing", "معاينة المعالجة")

    // Document Viewing & Actions
    object DocumentDetails : Screen("details/{docId}", "تفاصيل المستند") {
        fun createRoute(docId: String) = "details/$docId"
    }
    object Reader : Screen("reader/{docId}", "القارئ") {
        fun createRoute(docId: String) = "reader/$docId"
    }
    object OcrResult : Screen("ocr/{docId}/{page}", "استخراج النص (OCR)") {
        fun createRoute(docId: String, page: Int = 1) = "ocr/$docId/$page"
    }
    object PdfExport : Screen("export_pdf/{docId}", "تصدير PDF") {
        fun createRoute(docId: String) = "export_pdf/$docId"
    }
    object MagazineExport : Screen("export_magazine/{docId}", "تصدير المجلة") {
        fun createRoute(docId: String) = "export_magazine/$docId"
    }
    object BookExport : Screen("export_book/{docId}", "تصدير الكتاب") {
        fun createRoute(docId: String) = "export_book/$docId"
    }
    object Bookmarks : Screen("bookmarks", "إشارات القراءة", Icons.Default.Bookmark)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Library, Favorites, Search, Settings)
    }
}
