package com.usmandiscountstore.app.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * MobileFaceNet Face Embedding Helper (Qualcomm model)
 * - Model: mobile_facenet.tflite
 * - Input: [1, 112, 112, 3] float32 (normalized to [-1, 1])
 * - Output: [1, N] float32 (N=512 for Qualcomm, or 192 for older)
 */
object FaceEmbeddingHelper {

    private const val TAG = "FaceEmbedding"
    private const val MODEL_FILE = "mobile_facenet.tflite"
    private const val DEFAULT_INPUT_SIZE = 112

    private var interpreter: Interpreter? = null
    private var inputWidth = DEFAULT_INPUT_SIZE
    private var inputHeight = DEFAULT_INPUT_SIZE
    private var embeddingSize = 512
    private var isReady = false

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
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(mappedByteBuffer, options)

            // Detect input shape
            val inputShape = interpreter!!.getInputTensor(0).shape()
            Log.d(TAG, "Input shape: ${inputShape.toList()}")
            // Usually [1, 112, 112, 3] (NHWC) or [1, 3, 112, 112] (NCHW)
            when {
                inputShape.size == 4 -> {
                    if (inputShape[1] == 3 || inputShape[1] == 1) {
                        // NCHW
                        inputHeight = inputShape[2]
                        inputWidth = inputShape[3]
                    } else {
                        // NHWC
                        inputHeight = inputShape[1]
                        inputWidth = inputShape[2]
                    }
                }
            }

            // Detect output shape
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            Log.d(TAG, "Output shape: ${outputShape.toList()}")
            embeddingSize = outputShape.last()

            isReady = true
            Log.d(TAG, "Model ready: ${inputWidth}x${inputHeight}, emb=$embeddingSize")
        } catch (e: Exception) {
            Log.e(TAG, "Model init failed", e)
            e.printStackTrace()
        }
    }

    fun isReady(): Boolean = isReady

    /**
     * Extract L2-normalized embedding from face bitmap
     */
    fun getEmbedding(bitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        if (!isReady) return null
        return try {
            val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

            val inputBuffer = ByteBuffer.allocateDirect(
                1 * inputHeight * inputWidth * 3 * 4
            )
            inputBuffer.order(ByteOrder.nativeOrder())
            inputBuffer.rewind()

            val pixels = IntArray(inputWidth * inputHeight)
            resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF) / 127.5f - 1.0f
                val g = ((pixel shr 8) and 0xFF) / 127.5f - 1.0f
                val b = (pixel and 0xFF) / 127.5f - 1.0f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }
            inputBuffer.rewind()

            val output = Array(1) { FloatArray(embeddingSize) }
            interp.run(inputBuffer, output)

            val emb = output[0]
            val norm = sqrt(emb.sumOf { (it * it).toDouble() }).toFloat()
            if (norm > 0f) for (i in emb.indices) emb[i] /= norm
            emb
        } catch (e: Exception) {
            Log.e(TAG, "Embedding failed", e)
            null
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
