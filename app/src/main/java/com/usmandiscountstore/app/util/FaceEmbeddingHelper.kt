package com.usmandiscountstore.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

object FaceEmbeddingHelper {

    private const val TAG = "FaceEmbedding"
    private const val MODEL_FILE = "mobile_facenet.tflite"
    private const val DEFAULT_INPUT_SIZE = 112

    private var interpreter: Interpreter? = null
    private var inputWidth = DEFAULT_INPUT_SIZE
    private var inputHeight = DEFAULT_INPUT_SIZE
    private var embeddingSize = 512
    private var isNHWC = true
    private var isReady = false

    // Face detector for cropping
    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.1f)
                .build()
        )
    }

    fun init(context: Context) {
        if (interpreter != null) return
        try {
            val assetFile = context.assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(assetFile.fileDescriptor)
            val channel = inputStream.channel
            val mappedByteBuffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFile.startOffset,
                assetFile.declaredLength
            )
            interpreter = Interpreter(mappedByteBuffer, Interpreter.Options().apply { setNumThreads(4) })

            val inputShape = interpreter!!.getInputTensor(0).shape()
            Log.d(TAG, "Model input shape: ${inputShape.toList()}")

            if (inputShape.size == 4) {
                if (inputShape[1] == 3) {
                    isNHWC = false
                    inputHeight = inputShape[2]
                    inputWidth = inputShape[3]
                } else {
                    isNHWC = true
                    inputHeight = inputShape[1]
                    inputWidth = inputShape[2]
                }
            }

            embeddingSize = interpreter!!.getOutputTensor(0).shape().last()
            isReady = true
            Log.d(TAG, "✅ Model ready: ${inputWidth}x${inputHeight}, emb=$embeddingSize, NHWC=$isNHWC")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Model init failed", e)
        }
    }

    fun isReady(): Boolean = isReady

    /**
     * Context ke saath - face detect karke crop karta hai phir embedding banata hai
     */
    suspend fun getEmbeddingFromPhoto(context: Context, bitmap: Bitmap): FloatArray? {
        return try {
            // Step 1: Face detect karein
            val faceBox = detectLargestFace(bitmap)
            if (faceBox == null) {
                Log.e(TAG, "❌ Koi chehra nahi mila")
                return null
            }
            Log.d(TAG, "✅ Face detected: $faceBox")

            // Step 2: Face crop karein (thori padding ke saath)
            val croppedFace = cropFace(bitmap, faceBox)
            if (croppedFace == null) {
                Log.e(TAG, "❌ Crop fail")
                return null
            }
            Log.d(TAG, "✅ Face cropped: ${croppedFace.width}x${croppedFace.height}")

            // Step 3: Embedding banayein
            getEmbedding(croppedFace)
        } catch (e: Exception) {
            Log.e(TAG, "❌ getEmbeddingFromPhoto error", e)
            null
        }
    }

    /**
     * Bitmap se sab se bara chehra detect karke uska bounding box return karta hai
     */
    private suspend fun detectLargestFace(bitmap: Bitmap): Rect? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = com.google.android.gms.tasks.Tasks.await(faceDetector.process(inputImage))
            
            if (faces.isEmpty()) return null
            
            // Sab se bara chehra lein
            val largest = faces.maxByOrNull { 
                it.boundingBox.width() * it.boundingBox.height() 
            } ?: return null
            
            largest.boundingBox
        } catch (e: Exception) {
            Log.e(TAG, "Face detect error", e)
            null
        }
    }

    /**
     * Bounding box ke hisaab se face crop karta hai (square shape mein)
     */
    private fun cropFace(bitmap: Bitmap, box: Rect): Bitmap? {
        return try {
            // Square crop banao (thori padding ke saath)
            val padding = (box.width() * 0.2f).toInt()
            
            var left = box.left - padding
            var top = box.top - padding
            var right = box.right + padding
            var bottom = box.bottom + padding
            
            // Bounds check
            left = left.coerceAtLeast(0)
            top = top.coerceAtLeast(0)
            right = right.coerceAtMost(bitmap.width)
            bottom = bottom.coerceAtMost(bitmap.height)
            
            // Square banao
            val width = right - left
            val height = bottom - top
            val size = maxOf(width, height)
            
            val cx = (left + right) / 2
            val cy = (top + bottom) / 2
            
            val sqLeft = (cx - size / 2).coerceAtLeast(0)
            val sqTop = (cy - size / 2).coerceAtLeast(0)
            val sqRight = (cx + size / 2).coerceAtMost(bitmap.width)
            val sqBottom = (cy + size / 2).coerceAtMost(bitmap.height)
            
            val cropW = sqRight - sqLeft
            val cropH = sqBottom - sqTop
            if (cropW <= 0 || cropH <= 0) return null
            
            Bitmap.createBitmap(bitmap, sqLeft, sqTop, cropW, cropH)
        } catch (e: Exception) {
            Log.e(TAG, "Crop error", e)
            null
        }
    }

    /**
     * Cropped face bitmap se embedding banata hai
     */
    fun getEmbedding(bitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: run {
            Log.e(TAG, "❌ Interpreter null")
            return null
        }
        if (!isReady) {
            Log.e(TAG, "❌ Model not ready")
            return null
        }

        var resized: Bitmap? = null
        return try {
            val smallBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                val scale = 512f / maxOf(bitmap.width, bitmap.height)
                val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }

            resized = Bitmap.createScaledBitmap(smallBitmap, inputWidth, inputHeight, true)

            val inputBuffer = ByteBuffer.allocateDirect(1 * inputHeight * inputWidth * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            inputBuffer.rewind()

            val pixels = IntArray(inputWidth * inputHeight)
            resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            if (isNHWC) {
                for (pixel in pixels) {
                    inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1.0f)
                    inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1.0f)
                    inputBuffer.putFloat((pixel and 0xFF) / 127.5f - 1.0f)
                }
            } else {
                val r = FloatArray(pixels.size)
                val g = FloatArray(pixels.size)
                val b = FloatArray(pixels.size)
                for (i in pixels.indices) {
                    r[i] = ((pixels[i] shr 16) and 0xFF) / 127.5f - 1.0f
                    g[i] = ((pixels[i] shr 8) and 0xFF) / 127.5f - 1.0f
                    b[i] = (pixels[i] and 0xFF) / 127.5f - 1.0f
                }
                for (v in r) inputBuffer.putFloat(v)
                for (v in g) inputBuffer.putFloat(v)
                for (v in b) inputBuffer.putFloat(v)
            }
            inputBuffer.rewind()

            val output = Array(1) { FloatArray(embeddingSize) }
            interp.run(inputBuffer, output)

            val emb = output[0]
            val norm = sqrt(emb.sumOf { (it * it).toDouble() }).toFloat()
            if (norm > 0f) for (i in emb.indices) emb[i] /= norm
            Log.d(TAG, "✅ Embedding OK: ${emb.size} values, norm=$norm")
            emb
        } catch (e: Exception) {
            Log.e(TAG, "❌ Embedding failed", e)
            e.printStackTrace()
            null
        } finally {
            try { resized?.recycle() } catch (_: Exception) {}
        }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }

    fun embedToString(embedding: FloatArray): String =
        embedding.joinToString(",") { it.toString() }

    fun stringToEmbed(s: String): FloatArray? {
        if (s.isBlank()) return null
        return try {
            s.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) { null }
    }

    fun release() {
        interpreter?.close()
        interpreter = null
        isReady = false
    }
}
