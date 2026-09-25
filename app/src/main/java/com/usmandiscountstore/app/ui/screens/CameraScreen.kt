package com.usmandiscountstore.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.usmandiscountstore.app.ui.theme.BrandGreen
import com.usmandiscountstore.app.ui.theme.BrandGreenLight
import com.usmandiscountstore.app.ui.theme.TextDark
import com.usmandiscountstore.app.ui.theme.TextGray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Camera Screen — Live selfie capture with face detection.
 *
 * @param title Screen title
 * @param onPhotoCaptured Called with saved file path
 * @param onBack Back button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    title: String = "Live Photo",
    onPhotoCaptured: (File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm) permLauncher.launch(Manifest.permission.CAMERA)
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var faceCount by remember { mutableIntStateOf(0) }
    var faceDetectorReady by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(Color.Black)) {

            if (!hasCameraPerm) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission chahiye", color = Color.White)
                }
                return@Scaffold
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        // Add face analysis via ImageAnalysis? For simplicity we skip continuous analysis
                        // and only detect on captured photo.

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Face guide oval
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .border(3.dp, BrandGreen.copy(alpha = 0.7f), CircleShape)
            )

            // Bottom capture button
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Chehra oval ke andar rakhein",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        statusMsg = "Processing..."

                        val dir = File(context.getExternalFilesDir(null), "staff_selfies")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(
                            dir,
                            "PUNCH_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                        )
                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()

                        capture.takePicture(
                            opts,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    // Analyze face
                                    val img = InputImage.fromFilePath(context, Uri.fromFile(file))
                                    faceDetector.process(img)
                                        .addOnSuccessListener { faces: List<Face> ->
                                            if (faces.isEmpty()) {
                                                file.delete()
                                                statusMsg = "❌ Chehra nazar nahi aya. Dobara try karein."
                                            } else if (faces.size > 1) {
                                                file.delete()
                                                statusMsg = "❌ Ek se zyada chehre hain. Akele aayein."
                                            } else {
                                                val f = faces[0]
                                                val leftEye = f.leftEyeOpenProbability ?: 1f
                                                val rightEye = f.rightEyeOpenProbability ?: 1f
                                                if (leftEye < 0.2f && rightEye < 0.2f) {
                                                    file.delete()
                                                    statusMsg = "❌ Aankhein khuli rakhein."
                                                } else {
                                                    statusMsg = "✅ Photo capture ho gayi"
                                                    onPhotoCaptured(file)
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            file.delete()
                                            statusMsg = "❌ Face detect fail"
                                        }
                                }

                                override fun onError(e: ImageCaptureException) {
                                    statusMsg = "❌ Photo capture fail: ${e.message}"
                                }
                            }
                        )
                    },
                    modifier = Modifier.height(60.dp).width(60.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Camera, "Capture", modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(8.dp))
                statusMsg?.let {
                    Text(it, color = if (it.startsWith("✅")) Color(0xFF4CAF50) else Color(0xFFFF7043), fontSize = 12.sp)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { faceDetector.close() }
    }
}
