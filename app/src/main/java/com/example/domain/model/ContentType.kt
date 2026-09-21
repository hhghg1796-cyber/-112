package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Core content types for Arabic digital library and document processing.
 * User can switch this at any point without restarting the workflow.
 */
enum class ContentType(
    val titleArabic: String,
    val subtitleArabic: String,
    val icon: ImageVector,
    val detailedDescription: String,
    val futurePipelineSummary: String
) {
    DOCUMENT(
        titleArabic = "مستند",
        subtitleArabic = "أوراق رسمية ووثائق فردية",
        icon = Icons.Default.Description,
        detailedDescription = "معالجة الصفحة كوثيقة قياسية مع الحفاظ على هيكل الصفحة الكامل (صفحة إدخال واحدة ← صفحة إخراج واحدة)، وإجراء OCR لكامل الصفحة.",
        futurePipelineSummary = "حفظ الصفحة كاملة • استخراج النص الكامل • تصدير مستندي"
    ),
    MAGAZINE(
        titleArabic = "مجلة",
        subtitleArabic = "تخطيط متعدد الأعمدة والمقالات",
        icon = Icons.Default.Newspaper,
        detailedDescription = "تحليل التخطيط لاكتشاف المقالات والأعمدة والمربعات وتحديد مسار القراءة تلقائياً. كل مقال يمكن أن يصبح صفحة PDF مستقلة مع OCR منفصل.",
        futurePipelineSummary = "اكتشاف الأعمدة والمقالات • قص المقاطع • مسار القراءة الذكي"
    ),
    BOOK(
        titleArabic = "كتاب",
        subtitleArabic = "كتب ومخطوطات كاملة بترتيب الصفحات",
        icon = Icons.Default.AutoStories,
        detailedDescription = "الحفاظ على ترقيم وترتيب الصفحات، دعم البيانات الببليوغرافية للمؤلف والمجلد، استخراج فهرس المحتويات وتكوين أرشيف كتاب كامل.",
        futurePipelineSummary = "ترتيب الصفحات • بيانات المؤلف والفهرس • مجلد متكامل"
    ),
    COLLECTION(
        titleArabic = "مجموعة",
        subtitleArabic = "حزمة وثائق مجمعة قابلة لإعادة الترتيب",
        icon = Icons.Default.FolderSpecial,
        detailedDescription = "جمع صفحات ووثائق متعددة المصادر في حزمة واحدة، مع إمكانية الفرز وإعادة الترتيب وتنظيمها معاً في ملف تصدير مشترك.",
        futurePipelineSummary = "دمج متعدد المصادر • إعادة ترتيب مرنة • تصدير موحد"
    );

    companion object {
        val allTypes: List<ContentType> = entries
    }
}
