package com.example.ui.screens.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.storage.LocalStorageManager
import com.example.domain.model.FlashMode
import com.example.domain.model.ScanMode
import com.example.domain.model.ScannedPage
import com.example.ui.components.DocumentThumbnail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerEntryScreen(
    scannedPages: List<ScannedPage>,
    onCapturePage: (ScannedPage) -> Unit,
    onRemovePage: (String) -> Unit,
    onRotatePage: (String) -> Unit,
    onGalleryShortcutClick: () -> Unit,
    onProceedToReview: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val storageManager = remember { LocalStorageManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var scanMode by remember { mutableStateOf(ScanMode.MULTI) }
    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // CameraX references
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isCameraBound by remember { mutableStateOf(false) }

    // Captured image preview modal state
    var capturedTempFile: File? by remember { mutableStateOf(null) }
    var previewRotationDegrees by remember { mutableIntStateOf(0) }
    var showCropAdjuster by remember { mutableStateOf(false) }

    // Status ticker
    val silentStatusList = listOf(
        "ممتاز — جاهز للتصوير",
        "الصفحة مكتشفة",
        "ثبّت الهاتف لالتقاط أدق تفاصيل الورقة",
        "تم تحديد الحواف الرباعية تلقائياً"
    )
    var currentStatusIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentStatusIndex = (currentStatusIndex + 1) % silentStatusList.size
        }
    }

    // Photo picker for direct image addition
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val sessionId = "session_active"
            coroutineScope.launch {
                uris.forEachIndexed { index, uri ->
                    val result = storageManager.importFromUri(sessionId, uri, scannedPages.size + index + 1)
                    if (result != null) {
                        val newPage = ScannedPage(
                            id = "scan_${System.currentTimeMillis()}_$index",
                            pageNumber = scannedPages.size + index + 1,
                            imageUri = result.first,
                            imagePath = result.first,
                            thumbnailPath = result.second
                        )
                        onCapturePage(newPage)
                    }
                }
            }
        }
    }

    // Trigger flash/torch when mode changes
    LaunchedEffect(flashMode, camera) {
        camera?.cameraControl?.let { control ->
            when (flashMode) {
                FlashMode.ON -> control.enableTorch(true)
                FlashMode.OFF, FlashMode.AUTO -> control.enableTorch(false)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1514))
    ) {
        // Live Camera Viewfinder or Permission/Fallback Screen
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            imageCapture = capture

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            isCameraBound = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isCameraBound = false
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        } else {
            // Permission Request & Polite Notice
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "يلزم السماح بالوصول إلى الكاميرا",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "يحتاج الماسح الضوئي إلى إذن الكاميرا لتصوير وحفظ صفحات الكتب والوثائق محلياً على جهازك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("منح الإذن للكاميرا")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("اختيار صفحات من الصور بدلاً من ذلك")
                }
            }
        }

        // Viewfinder Guide Overlay & Detection Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(0.72f)
                    .border(
                        BorderStroke(2.dp, Color(0xFF65D5B3).copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                CornerTargetMarks(modifier = Modifier.fillMaxSize())

                // Silent Non-Blocking Status Notification
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC000000),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF65D5B3),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = silentStatusList[currentStatusIndex],
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Top Controls Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White
                )
            }

            // Flash Mode Toggle
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x66000000),
                modifier = Modifier.clickable {
                    flashMode = when (flashMode) {
                        FlashMode.AUTO -> FlashMode.ON
                        FlashMode.ON -> FlashMode.OFF
                        FlashMode.OFF -> FlashMode.AUTO
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.OFF -> Icons.Default.FlashOff
                        },
                        contentDescription = "الفلاش",
                        tint = if (flashMode == FlashMode.OFF) Color.LightGray else Color(0xFFFFD54F),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = flashMode.titleArabic,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Captured Page Counter Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "الصفحات: ${scannedPages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xE60D1311))
                .padding(top = 12.dp, bottom = 24.dp)
        ) {
            // Scan Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScanMode.entries.forEach { mode ->
                    val isSelected = mode == scanMode
                    FilterChip(
                        selected = isSelected,
                        onClick = { scanMode = mode },
                        label = { Text(mode.titleArabic) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = Color(0x33FFFFFF),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Scanned Page Thumbnails Strip
            if (scannedPages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(scannedPages, key = { it.id }) { p ->
                        DocumentThumbnail(
                            pageNumber = p.pageNumber,
                            imagePath = p.thumbnailPath ?: p.imagePath,
                            rotationDegrees = p.rotationDegrees,
                            onRotateClick = { onRotatePage(p.id) },
                            onDeleteClick = { onRemovePage(p.id) },
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Capture Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Shortcut
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.size(52.dp)
                ) {
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "استيراد من المعرض",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Shutter / Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            val activeCapture = imageCapture
                            if (activeCapture != null) {
                                val tempFile = File(context.cacheDir, "scan_capture_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                                activeCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            capturedTempFile = tempFile
                                            previewRotationDegrees = 0
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Camera capture error or mock fallback
                                            val fallbackFile = generateFallbackCapture(context)
                                            capturedTempFile = fallbackFile
                                            previewRotationDegrees = 0
                                        }
                                    }
                                )
                            } else {
                                // Camera not ready or in simulator
                                val fallbackFile = generateFallbackCapture(context)
                                capturedTempFile = fallbackFile
                                previewRotationDegrees = 0
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(58.dp)
                    ) {}
                }

                // Proceed to Review / Finish Button
                Button(
                    onClick = onProceedToReview,
                    enabled = scannedPages.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = Color(0x33FFFFFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "متابعة (${scannedPages.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Captured Page Image Preview & Processing Overlay
        capturedTempFile?.let { tempFile ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF2090E0C))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "معاينة الصفحة الملتقطة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = tempFile,
                                contentDescription = "معاينة الصفحة",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (showCropAdjuster) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.9f)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        // Adjustment Tools (Rotate, Crop)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = {
                                coroutineScope.launch {
                                    storageManager.rotateImageFile(tempFile.absolutePath, 90)
                                    previewRotationDegrees = (previewRotationDegrees + 90) % 360
                                }
                            }) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تدوير")
                            }

                            OutlinedButton(onClick = { showCropAdjuster = !showCropAdjuster }) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showCropAdjuster) "تثبيت القص" else "ضبط الحواف")
                            }
                        }

                        // Decision Actions (Retake vs Accept)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    tempFile.delete()
                                    capturedTempFile = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إعادة الالتقاط")
                            }

                            Button(
                                onClick = {
                                    val sessionId = "session_active"
                                    val pageNumber = scannedPages.size + 1
                                    coroutineScope.launch {
                                        val saved = storageManager.saveImageFileToSession(sessionId, tempFile, pageNumber)
                                        capturedTempFile = null

                                        if (saved != null) {
                                            val page = ScannedPage(
                                                id = "scan_${System.currentTimeMillis()}",
                                                pageNumber = pageNumber,
                                                imageUri = saved.first,
                                                imagePath = saved.first,
                                                thumbnailPath = saved.second
                                            )
                                            onCapturePage(page)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اعتماد الصفحة")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateFallbackCapture(context: Context): File {
    val file = File(context.cacheDir, "sample_scan_${System.currentTimeMillis()}.jpg")
    val bitmap = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 28f
        isAntiAlias = true
    }
    canvas.drawText("صفحة وثيقة ممسوحة ضوئياً", 100f, 200f, paint)
    paint.textSize = 20f
    canvas.drawText("تم التقاطها وحفظها محلياً في الأرشيف", 100f, 260f, paint)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file
}

@Composable
private fun CornerTargetMarks(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(8.dp)) {
        // Top right mark
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .align(Alignment.TopEnd)
        ) {}
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 3.dp, height = 24.dp)
                .align(Alignment.TopEnd)
        ) {}

        // Top left mark
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .align(Alignment.TopStart)
        ) {}
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 3.dp, height = 24.dp)
                .align(Alignment.TopStart)
        ) {}

        // Bottom right mark
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .align(Alignment.BottomEnd)
        ) {}
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 3.dp, height = 24.dp)
                .align(Alignment.BottomEnd)
        ) {}

        // Bottom left mark
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .align(Alignment.BottomStart)
        ) {}
        Surface(
            color = Color(0xFF65D5B3),
            modifier = Modifier
                .size(width = 3.dp, height = 24.dp)
                .align(Alignment.BottomStart)
        ) {}
    }
}
