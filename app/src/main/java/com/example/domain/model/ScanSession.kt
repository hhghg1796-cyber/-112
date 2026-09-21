package com.example.domain.model

enum class ScanMode(val titleArabic: String) {
    SINGLE("مفرد"),
    MULTI("متعدد الصفحات"),
    BATCH("دفعة سريعة")
}

enum class FlashMode(val titleArabic: String) {
    AUTO("تلقائي"),
    ON("تشغيل"),
    OFF("إيقاف")
}

data class ScannedPage(
    val id: String,
    val pageNumber: Int,
    val imageUri: String? = null,
    val imagePath: String? = imageUri,
    val thumbnailPath: String? = null,
    val rotationDegrees: Int = 0,
    val hasDetectedDocument: Boolean = true
)

data class FileImportItem(
    val id: String,
    val fileName: String,
    val fileExtension: String,
    val sizeFormatted: String,
    val pageCountEstimate: Int?,
    val isSelected: Boolean = true
)

enum class ExportQuality(val labelArabic: String, val descriptionArabic: String) {
    HIGH("عالية (أرشيفية)", "دقة 300 نقطة/بوصة — مناسبة للطباعة والتوثيق المكتبي"),
    MEDIUM("متوسطة (متوازنة)", "دقة 150 نقطة/بوصة — حجم مناسب للمشاركة والتصفح"),
    LIGHT("خفيفة (سريعة)", "دقة 96 نقطة/بوصة — حجم مضغوط للغاية للبريد")
}

data class ExportConfig(
    val type: ContentType = ContentType.DOCUMENT,
    val quality: ExportQuality = ExportQuality.HIGH,
    val includeOcr: Boolean = true,
    val includePageNumbers: Boolean = true,
    val includeCover: Boolean = true,
    val includeMetadata: Boolean = true,
    val customTitle: String = ""
)
