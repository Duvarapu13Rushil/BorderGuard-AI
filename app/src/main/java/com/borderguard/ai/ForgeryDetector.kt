package com.borderguard.ai

import android.content.Context
import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

class ForgeryDetector(context: Context) {

    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets
            .open("sidtd_forgery_detector_finetuned.tflite")
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        interpreter = Interpreter(modelBuffer)
    }

    fun detect(bitmap: Bitmap): Float {
        // Resize to the exact model input size: 224 x 224
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            224,
            224,
            true
        )

        // Model expects [1, 224, 224, 3] float32
        // Pixel values must remain in the range 0-255.
        val inputBuffer = ByteBuffer
            .allocateDirect(1 * 224 * 224 * 3 * 4)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(224 * 224)
        resizedBitmap.getPixels(
            pixels,
            0,
            224,
            0,
            0,
            224,
            224
        )

        for (pixel in pixels) {
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF

            inputBuffer.putFloat(red.toFloat())
            inputBuffer.putFloat(green.toFloat())
            inputBuffer.putFloat(blue.toFloat())
        }

        inputBuffer.rewind()

        // Model output: [1, 1]
        // Because class mapping is:
        // fake = 0
        // real = 1
        val output = Array(1) { FloatArray(1) }

        interpreter.run(inputBuffer, output)

        return output[0][0]
    }

    fun close() {
        interpreter.close()
    }
}