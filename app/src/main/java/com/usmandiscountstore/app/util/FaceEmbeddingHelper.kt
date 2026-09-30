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
            Log.d(TAG, "Input shape: ${inputShape.toList()}")
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
            Log.d(TAG, "Model ready: ${inputWidth}x${inputHeight}, emb=$embeddingSize, NHWC=$isNHWC")
        } catch (e: Exception) {
            Log.e(TAG, "Model init failed", e)
        }
    }

    fun isReady(): Boolean = isReady

    fun getEmbedding(bitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: run {
            Log.e(TAG, "Interpreter null")
            return null
        }
        if (!isReady) {
            Log.e(TAG, "Model not ready")
            return null
        }

        var resized: Bitmap? = null
        return try {
            // Pehle bari bitmap ko chhota karein
            val smallBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                val scale = 512f / maxOf(bitmap.width, bitmap.height)
                val newW = (bitmap.width * scale).toInt()
                val newH = (bitmap.height * scale).toInt()
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
            Log.d(TAG, "Embedding OK: ${emb.size} values")
            emb
        } catch (e: Exception) {
            Log.e(TAG, "Embedding failed", e)
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
