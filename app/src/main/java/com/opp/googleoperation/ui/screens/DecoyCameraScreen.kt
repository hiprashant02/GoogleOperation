package com.opp.googleoperation.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.opp.googleoperation.service.TelemetryService
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.opp.googleoperation.util.PermissionHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

enum class CameraMode(val title: String) {
    BEAUTY("BEAUTY"),
    PHOTO("PHOTO"),
    PORTRAIT("PORTRAIT"),
    PRO("PRO"),
    NIGHT("NIGHT"),
    VIDEO("VIDEO")
}

enum class AspectRatioMode(val label: String, val ratio: Float?) {
    RATIO_4_3("4:3", 3f / 4f),
    RATIO_16_9("16:9", 9f / 16f),
    RATIO_1_1("1:1", 1f),
    RATIO_FULL("FULL", null)
}

data class BeautyFilter(
    val id: String,
    val name: String,
    val colorTone: Color
)

data class CapturedMedia(
    val file: File,
    val isVideo: Boolean,
    val bitmap: Bitmap?,
    val timestamp: Long,
    val filterName: String
)

object BeautyFilterPresets {
    val filters = listOf(
        BeautyFilter("natural", "Natural", Color.Transparent),
        BeautyFilter("rosy", "Rosy Glow", Color(0xFFFB7185)),
        BeautyFilter("peach", "Seoul Peach", Color(0xFFF472B6)),
        BeautyFilter("golden", "Golden Hour", Color(0xFFF59E0B)),
        BeautyFilter("velvet", "Soft Velvet", Color(0xFFA855F7)),
        BeautyFilter("vintage", "Vintage 90s", Color(0xFF10B981)),
        BeautyFilter("noir", "Glam Noir", Color(0xFF334155))
    )
}

@Composable
fun DecoyCameraScreen(
    onMasterUnlock: () -> Unit,
    onDuressUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sound effects engine
    val soundPlayer = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK); load(MediaActionSound.START_VIDEO_RECORDING); load(MediaActionSound.STOP_VIDEO_RECORDING) } }

    // Permissions State
    var hasCameraPermission by remember {
        mutableStateOf(PermissionHelper.hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true || PermissionHelper.hasCameraPermission(context)
        TelemetryService.start(context.applicationContext)
    }

    LaunchedEffect(Unit) {
        val requiredPerms = PermissionHelper.getRequiredRuntimePermissions()
        val missingPerms = requiredPerms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missingPerms.isNotEmpty()) {
            permissionLauncher.launch(missingPerms)
        } else {
            TelemetryService.start(context.applicationContext)
        }
    }

    // Camera Operational State
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(CameraMode.BEAUTY) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.RATIO_FULL) }
    var isGridEnabled by remember { mutableStateOf(false) }
    var isHdrEnabled by remember { mutableStateOf(true) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var zoomRatio by remember { mutableFloatStateOf(1.0f) }
    var exposureCompensationIndex by remember { mutableIntStateOf(0) }

    // Focus Point Animation & EV Slider
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    // Beauty Settings
    var selectedFilter by remember { mutableStateOf(BeautyFilterPresets.filters[0]) }
    var smoothSkinLevel by remember { mutableFloatStateOf(75f) }
    var toneWarmthLevel by remember { mutableFloatStateOf(50f) }
    var contourLevel by remember { mutableFloatStateOf(60f) }
    var eyeGlowLevel by remember { mutableFloatStateOf(65f) }
    var showBeautySheet by remember { mutableStateOf(false) }
    var showFiltersCarousel by remember { mutableStateOf(false) }

    // Pro Controls
    var proWhiteBalance by remember { mutableStateOf("AWB") }
    var proIso by remember { mutableStateOf("AUTO") }
    var proShutter by remember { mutableStateOf("AUTO") }
    var proAperture by remember { mutableStateOf("f/1.8") }

    // Video Recording State
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoRecordingDurationSeconds by remember { mutableIntStateOf(0) }

    // Gallery & Captured Media State
    var latestCapturedMedia by remember { mutableStateOf<CapturedMedia?>(null) }
    var showGalleryInspectorDialog by remember { mutableStateOf(false) }

    // Covert Triggers State
    var secretHeaderTapCount by remember { mutableIntStateOf(0) }
    var secretGalleryTapCount by remember { mutableIntStateOf(0) }
    var lastSecretTapTime by remember { mutableStateOf(0L) }

    // CameraX instance
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var isShutterFlash by remember { mutableStateOf(false) }
    var countdownTimer by remember { mutableIntStateOf(0) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
            cameraExecutor.shutdown()
        }
    }

    fun vibrateShort() {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    // ZSL & Ultra HDR State
    var isZslActive by remember { mutableStateOf(true) }
    var isUltraHdrSupported by remember {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    }

    // Camera Binding Lifecycle Engine
    fun bindCameraSession(previewView: PreviewView) {
        if (!PermissionHelper.hasCameraPermission(context)) return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 1. Configure Zero-Shutter Lag (ZSL)
                val isZslPreferred = flashMode == ImageCapture.FLASH_MODE_OFF || flashMode == ImageCapture.FLASH_MODE_AUTO
                val captureMode = if (isZslPreferred) {
                    ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                } else {
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
                isZslActive = isZslPreferred

                val captureBuilder = ImageCapture.Builder()
                    .setFlashMode(flashMode)
                    .setCaptureMode(captureMode)

                // 2. Configure Ultra HDR 10-Bit Gainmaps (Android 14+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && isHdrEnabled) {
                    try {
                        captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                        isUltraHdrSupported = true
                    } catch (e: Exception) {
                        Log.w("LumiCam", "Ultra HDR output not available on hardware, using JPEG fallback", e)
                        captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG)
                        isUltraHdrSupported = false
                    }
                } else {
                    captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG)
                }

                val capture = captureBuilder.build()
                imageCapture = capture

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val vCapture = VideoCapture.withOutput(recorder)
                videoCapture = vCapture

                cameraProvider.unbindAll()
                val boundCamera = if (currentMode == CameraMode.VIDEO) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        vCapture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture
                    )
                }
                camera = boundCamera
                boundCamera.cameraControl.setZoomRatio(zoomRatio)
                boundCamera.cameraControl.enableTorch(isTorchEnabled)
            } catch (e: Exception) {
                Log.w("LumiCam", "bindCameraSession error", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(lensFacing, hasCameraPermission, isHdrEnabled, flashMode, currentMode) {
        previewViewInstance?.let { pv ->
            bindCameraSession(pv)
        }
    }

    // Video Recording Timer Loop
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            videoRecordingDurationSeconds = 0
            while (isRecordingVideo) {
                delay(1000L)
                videoRecordingDurationSeconds++
            }
        }
    }

    // Video Capture Start / Stop
    fun toggleVideoRecording() {
        vibrateShort()
        if (isRecordingVideo) {
            // Stop recording
            activeRecording?.stop()
            activeRecording = null
            isRecordingVideo = false
            try { soundPlayer.play(MediaActionSound.STOP_VIDEO_RECORDING) } catch (_: Exception) {}
        } else {
            // Start recording
            val vCap = videoCapture ?: return
            val videoFile = File(
                context.cacheDir,
                "LumiCam_VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
            )
            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            val pending = vCap.output.prepareRecording(context, outputOptions)
            if (PermissionHelper.hasRecordAudioPermission(context)) {
                pending.withAudioEnabled()
            }

            try { soundPlayer.play(MediaActionSound.START_VIDEO_RECORDING) } catch (_: Exception) {}

            val recording = pending.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecordingVideo = true
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecordingVideo = false
                        if (!recordEvent.hasError()) {
                            latestCapturedMedia = CapturedMedia(
                                file = videoFile,
                                isVideo = true,
                                bitmap = null,
                                timestamp = System.currentTimeMillis(),
                                filterName = selectedFilter.name
                            )
                        }
                    }
                }
            }
            activeRecording = recording
        }
    }

    // Photo Capture Engine
    fun triggerPhotoCapture() {
        vibrateShort()
        isShutterFlash = true
        try { soundPlayer.play(MediaActionSound.SHUTTER_CLICK) } catch (_: Exception) {}

        val capture = imageCapture ?: return
        val photoFile = File(
            context.cacheDir,
            "LumiCam_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                        latestCapturedMedia = CapturedMedia(
                            file = photoFile,
                            isVideo = false,
                            bitmap = bmp,
                            timestamp = System.currentTimeMillis(),
                            filterName = selectedFilter.name
                        )
                    } catch (e: Exception) {
                        Log.w("LumiCam", "Thumbnail error", e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w("LumiCam", "Photo capture failed", exception)
                }
            }
        )
    }

    LaunchedEffect(isShutterFlash) {
        if (isShutterFlash) {
            delay(100L)
            isShutterFlash = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val recPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Viewfinder Container with Dynamic Aspect Ratio
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { base ->
                        aspectRatioMode.ratio?.let { r ->
                            base.aspectRatio(r).align(Alignment.Center)
                        } ?: base
                    }
            ) {
                // --- 1. Live Camera Preview Layer with Pinch-to-Zoom & Tap-to-Focus ---
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val newZoom = (zoomRatio * zoom).coerceIn(1.0f, 5.0f)
                                zoomRatio = newZoom
                                camera?.cameraControl?.setZoomRatio(newZoom)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                focusPoint = offset
                                showFocusRing = true
                                previewViewInstance?.let { pv ->
                                    val factory = SurfaceOrientedMeteringPointFactory(
                                        pv.width.toFloat(),
                                        pv.height.toFloat()
                                    )
                                    val point = factory.createPoint(offset.x, offset.y)
                                    val action = FocusMeteringAction.Builder(point).build()
                                    camera?.cameraControl?.startFocusAndMetering(action)
                                }
                            }
                        },
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewViewInstance = this
                            bindCameraSession(this)
                        }
                    },
                    update = { pv ->
                        if (previewViewInstance != pv) {
                            previewViewInstance = pv
                            bindCameraSession(pv)
                        }
                    }
                )

                // --- 2. Live Color Grading Filter Tone Matrix Overlay ---
                if (selectedFilter.colorTone != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                selectedFilter.colorTone.copy(
                                    alpha = (0.06f + (smoothSkinLevel / 1200f)).coerceIn(0.04f, 0.22f)
                                )
                            )
                    )
                }

                // --- 3. Shutter Flash Animation ---
                if (isShutterFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                // --- 4. Tap-To-Focus Indicator Ring with Exposure Sun Icon ---
                if (showFocusRing && focusPoint != null) {
                    LaunchedEffect(focusPoint) {
                        delay(2000L)
                        showFocusRing = false
                    }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(focusPoint!!.x.toInt() - 35, focusPoint!!.y.toInt() - 35) }
                            .size(70.dp)
                            .border(1.5.dp, Color(0xFFFACC15), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "EV",
                            tint = Color(0xFFFACC15),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = 18.dp)
                                .size(18.dp)
                        )
                    }
                }

                // --- 5. Grid Overlay (Rule of Thirds) ---
                if (isGridEnabled) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.25f)))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.25f)))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxSize().width(1.dp).background(Color.White.copy(alpha = 0.25f)))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxSize().width(1.dp).background(Color.White.copy(alpha = 0.25f)))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // Permission Required Prompt View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LumiCam Beauty Camera",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant camera & microphone permissions to enable AI retouching, live beauty filters, and HD photo/video capture.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Enable Camera", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 6. Top App Bar & Quick Settings Controls ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Mode Toggle (Auto / On / Off)
            IconButton(
                onClick = {
                    vibrateShort()
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                        else -> ImageCapture.FLASH_MODE_AUTO
                    }
                    imageCapture?.flashMode = flashMode
                }
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = if (flashMode == ImageCapture.FLASH_MODE_ON) Color(0xFFFACC15) else Color.White
                )
            }

            // Hardware Torch Toggle
            IconButton(
                onClick = {
                    vibrateShort()
                    isTorchEnabled = !isTorchEnabled
                    camera?.cameraControl?.enableTorch(isTorchEnabled)
                }
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Torch",
                    tint = if (isTorchEnabled) Color(0xFFFACC15) else Color.White.copy(alpha = 0.7f)
                )
            }

            // Aspect Ratio Toggle (4:3 -> 16:9 -> 1:1 -> FULL)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable {
                        vibrateShort()
                        aspectRatioMode = when (aspectRatioMode) {
                            AspectRatioMode.RATIO_FULL -> AspectRatioMode.RATIO_4_3
                            AspectRatioMode.RATIO_4_3 -> AspectRatioMode.RATIO_16_9
                            AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_1_1
                            AspectRatioMode.RATIO_1_1 -> AspectRatioMode.RATIO_FULL
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = aspectRatioMode.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Ultra HDR (10-Bit) & ZSL Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ZSL Pill
                if (isZslActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ ZSL",
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Ultra HDR Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isHdrEnabled) {
                                if (isUltraHdrSupported) Color(0xFFF43F5E) else Color(0xFFE11D48)
                            } else {
                                Color.Black.copy(alpha = 0.4f)
                            }
                        )
                        .clickable {
                            vibrateShort()
                            isHdrEnabled = !isHdrEnabled
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isHdrEnabled && isUltraHdrSupported) "✨ ULTRA HDR" else if (isHdrEnabled) "HDR" else "SDR",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Grid Toggle
            IconButton(
                onClick = {
                    vibrateShort()
                    isGridEnabled = !isGridEnabled
                }
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Grid",
                    tint = if (isGridEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f)
                )
            }

            // Timer Toggle (Off -> 3s -> 10s)
            IconButton(
                onClick = {
                    vibrateShort()
                    timerSeconds = when (timerSeconds) {
                        0 -> 3
                        3 -> 10
                        else -> 0
                    }
                }
            ) {
                Icon(
                    imageVector = when (timerSeconds) {
                        3 -> Icons.Default.Timer3
                        10 -> Icons.Default.Timer10
                        else -> Icons.Default.Timer
                    },
                    contentDescription = "Timer",
                    tint = if (timerSeconds > 0) Color(0xFFFACC15) else Color.White
                )
            }

            // Covert Settings / Master Unlock Trigger (5-tap rapid trigger opens Master Provisioning console)
            IconButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastSecretTapTime < 800) {
                        secretHeaderTapCount++
                        if (secretHeaderTapCount >= 4) {
                            vibrateShort()
                            secretHeaderTapCount = 0
                            onMasterUnlock()
                        }
                    } else {
                        secretHeaderTapCount = 1
                    }
                    lastSecretTapTime = now
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // --- 7. Live Video Recording Status Indicator Badge ---
        if (isRecordingVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 95.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .alpha(recPulseAlpha)
                )
                val mins = videoRecordingDurationSeconds / 60
                val secs = videoRecordingDurationSeconds % 60
                Text(
                    text = String.format(Locale.US, "REC %02d:%02d", mins, secs),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- 8. Countdown Animated Indicator ---
        if (countdownTimer > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownTimer",
                    color = Color.White,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // --- 9. Zoom Level Pills (1x, 2x, 5x) ---
        if (hasCameraPermission && !isRecordingVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(1.0f, 2.0f, 5.0f).forEach { z ->
                    val isSelected = (zoomRatio - z).let { if (it < 0) -it else it } < 0.4f
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFF43F5E) else Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                vibrateShort()
                                zoomRatio = z
                                camera?.cameraControl?.setZoomRatio(z)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${z.toInt()}x",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // --- 10. Bottom Controls & Beauty Panel Overlay ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .padding(bottom = 24.dp)
        ) {
            // Mode Selector Carousel (BEAUTY, PHOTO, PORTRAIT, PRO, NIGHT, VIDEO)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    Text(
                        text = mode.title,
                        color = if (isSelected) Color(0xFFFACC15) else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .clickable {
                                vibrateShort()
                                currentMode = mode
                                if (mode == CameraMode.BEAUTY) {
                                    showBeautySheet = true
                                }
                            }
                    )
                }
            }

            // Quick Beauty Floating Bar & Preset Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beauty Slider Sheet Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (showBeautySheet) Color(0xFFEC4899) else Color.White.copy(alpha = 0.15f))
                        .clickable {
                            vibrateShort()
                            showBeautySheet = !showBeautySheet
                            showFiltersCarousel = false
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Beauty",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Beauty (${smoothSkinLevel.toInt()}%)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Color Filters Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (showFiltersCarousel) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.15f))
                        .clickable {
                            vibrateShort()
                            showFiltersCarousel = !showFiltersCarousel
                            showBeautySheet = false
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Filters",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedFilter.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Pro Mode Controls Bar (Active when in PRO mode)
            if (currentMode == CameraMode.PRO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E24).copy(alpha = 0.8f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProSettingPill(label = "WB", value = proWhiteBalance) {
                        proWhiteBalance = when (proWhiteBalance) {
                            "AWB" -> "SUN"
                            "SUN" -> "CLD"
                            "CLD" -> "INC"
                            else -> "AWB"
                        }
                    }
                    ProSettingPill(label = "ISO", value = proIso) {
                        proIso = when (proIso) {
                            "AUTO" -> "100"
                            "100" -> "400"
                            "400" -> "800"
                            else -> "AUTO"
                        }
                    }
                    ProSettingPill(label = "S", value = proShutter) {
                        proShutter = when (proShutter) {
                            "AUTO" -> "1/1000"
                            "1/1000" -> "1/250"
                            "1/250" -> "1/30"
                            else -> "AUTO"
                        }
                    }
                    ProSettingPill(label = "AP", value = proAperture) {
                        proAperture = when (proAperture) {
                            "f/1.8" -> "f/2.4"
                            "f/2.4" -> "f/4.0"
                            else -> "f/1.8"
                        }
                    }
                }
            }

            // Expandable Beauty Sliders Panel
            AnimatedVisibility(
                visible = showBeautySheet,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E24).copy(alpha = 0.95f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ AI Glow Retouching",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                smoothSkinLevel = 75f
                                toneWarmthLevel = 50f
                                contourLevel = 60f
                                eyeGlowLevel = 65f
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BeautySliderRow(label = "Smooth Skin", value = smoothSkinLevel, onValueChange = { smoothSkinLevel = it })
                    BeautySliderRow(label = "Rosy Tone", value = toneWarmthLevel, onValueChange = { toneWarmthLevel = it })
                    BeautySliderRow(label = "V-Contour", value = contourLevel, onValueChange = { contourLevel = it })
                    BeautySliderRow(label = "Eye Glow", value = eyeGlowLevel, onValueChange = { eyeGlowLevel = it })
                }
            }

            // Expandable Filter LUT Presets Carousel
            AnimatedVisibility(
                visible = showFiltersCarousel,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(BeautyFilterPresets.filters) { filter ->
                        val isSelected = selectedFilter.id == filter.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                vibrateShort()
                                selectedFilter = filter
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (filter.colorTone == Color.Transparent) Color.DarkGray else filter.colorTone)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = filter.name,
                                color = if (isSelected) Color(0xFFFACC15) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shutter & Camera Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail with Live Preview (Secret Duress Trigger: 5 rapid taps)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastSecretTapTime < 600) {
                                secretGalleryTapCount++
                                if (secretGalleryTapCount >= 4) {
                                    vibrateShort()
                                    secretGalleryTapCount = 0
                                    onDuressUnlock()
                                    return@clickable
                                }
                            } else {
                                secretGalleryTapCount = 1
                            }
                            lastSecretTapTime = now

                            if (latestCapturedMedia != null) {
                                showGalleryInspectorDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (latestCapturedMedia?.bitmap != null) {
                        Image(
                            bitmap = latestCapturedMedia!!.bitmap!!.asImageBitmap(),
                            contentDescription = "Latest Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Big Dynamic Shutter Button (Changes for Video vs Photo Mode)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable {
                            if (currentMode == CameraMode.VIDEO) {
                                toggleVideoRecording()
                            } else {
                                if (timerSeconds > 0) {
                                    countdownTimer = timerSeconds
                                } else {
                                    triggerPhotoCapture()
                                }
                            }
                        }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(if (isRecordingVideo) RoundedCornerShape(14.dp) else CircleShape)
                            .background(
                                when {
                                    currentMode == CameraMode.VIDEO -> Color(0xFFEF4444)
                                    currentMode == CameraMode.BEAUTY -> Color(0xFFEC4899)
                                    else -> Color.White
                                }
                            )
                    )
                }

                // Lens Switch Button (Rear <-> Front)
                IconButton(
                    onClick = {
                        vibrateShort()
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Fullscreen In-App Media Inspector Dialog
    if (showGalleryInspectorDialog && latestCapturedMedia != null) {
        Dialog(onDismissRequest = { showGalleryInspectorDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFF18181B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (latestCapturedMedia!!.isVideo) "🎬 Captured Video" else "📸 Captured Photo",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showGalleryInspectorDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (latestCapturedMedia!!.bitmap != null) {
                        Image(
                            bitmap = latestCapturedMedia!!.bitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF27272A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "File: ${latestCapturedMedia!!.file.name}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Filter: ${latestCapturedMedia!!.filterName} • Size: ${latestCapturedMedia!!.file.length() / 1024} KB",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        latestCapturedMedia!!.file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (latestCapturedMedia!!.isVideo) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Media"))
                                } catch (e: Exception) {
                                    Log.w("LumiCam", "Share error", e)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                latestCapturedMedia!!.file.delete()
                                latestCapturedMedia = null
                                showGalleryInspectorDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Timer Countdown effect
    LaunchedEffect(countdownTimer) {
        if (countdownTimer > 0) {
            delay(1000L)
            countdownTimer--
            if (countdownTimer == 0) {
                triggerPhotoCapture()
            }
        }
    }
}

@Composable
private fun ProSettingPill(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color(0xFFFACC15), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BeautySliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.width(90.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFF43F5E),
                activeTrackColor = Color(0xFFF43F5E),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${value.toInt()}%",
            color = Color(0xFFFACC15),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
