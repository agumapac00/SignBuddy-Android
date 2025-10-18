package com.example.signbuddy.ml

import android.content.Context
import android.graphics.*
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ASLModelHelper {

    private var interpreter: Interpreter? = null
    private const val INPUT_SIZE = 320 // ✅ matches model input, smaller = faster inference
    private const val TAG = "ASLModelHelper"

    // ✅ Pre-configured image processor for resizing input images
    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    /**
     * ✅ Loads the TFLite model from assets (if not already loaded).
     */
    fun loadModel(context: Context) {
        if (interpreter != null) return
        try {
            val modelBuffer = loadModelFile(context, "asl_model.tflite")
            val options = Interpreter.Options().apply {
                // Use fewer threads to reduce memory pressure
                setNumThreads(2)
                // Enable GPU delegate if available (optional)
                // setUseNNAPI(true)
            }

            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "✅ ASL Model loaded successfully.")
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "❌ Out of memory loading ASL model", e)
            System.gc()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load ASL model", e)
        }
    }

    /**
     * ✅ Load .tflite file from assets as MappedByteBuffer
     */
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun getInterpreter(): Interpreter? = interpreter

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    /**
     * ✅ Converts ImageProxy → Bitmap, handles rotation and front-camera mirroring.
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy, rotationDegrees: Int, flip: Boolean): Bitmap {
        val image = imageProxy.image ?: throw IllegalStateException("Image is null")

        try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val imageBytes = out.toByteArray()
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null || bitmap.isRecycled) {
                throw IllegalStateException("Failed to decode bitmap from image bytes")
            }

            // ✅ Rotate bitmap
            val rotationMatrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
            
            // Recycle original bitmap if it's different from rotated one
            if (bitmap != rotatedBitmap) {
                bitmap.recycle()
            }
            bitmap = rotatedBitmap

            // ✅ Mirror horizontally if using front camera
            if (flip) {
                val flipMatrix = Matrix().apply { preScale(-1f, 1f) }
                val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, flipMatrix, true)
                
                // Recycle original bitmap if it's different from flipped one
                if (bitmap != flippedBitmap) {
                    bitmap.recycle()
                }
                bitmap = flippedBitmap
            }

            return bitmap
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory converting ImageProxy to Bitmap", e)
            System.gc()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            throw e
        }
    }
}
