package com.borderguard.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object FaceEmbeddingProcessor {

    private const val INPUT_SIZE = 112
    private const val EMBEDDING_SIZE = 192

    /*
     * This MobileFaceNet model expects TWO images
     * in a single inference:
     *
     * [2, 112, 112, 3]
     *
     * We only need one embedding at a time,
     * so we put the same face image into both
     * batch slots and use the first output.
     */

    private const val BATCH_SIZE = 2

    private var interpreter: Interpreter? = null


    /*
     * =========================================================
     * INITIALIZE MODEL
     * =========================================================
     */

    fun initialize(context: Context) {

        if (interpreter != null) {
            return
        }

        val modelBytes =
            context.assets
                .open("MobileFaceNet.tflite")
                .use {
                    it.readBytes()
                }


        val modelBuffer =
            ByteBuffer
                .allocateDirect(
                    modelBytes.size
                )
                .order(
                    ByteOrder.nativeOrder()
                )

        modelBuffer.put(modelBytes)

        modelBuffer.rewind()


        interpreter =
            Interpreter(modelBuffer)


        /*
         * Print model input/output information
         * so we can verify it in Logcat.
         */

        val inputTensor =
            interpreter!!.getInputTensor(0)

        val outputTensor =
            interpreter!!.getOutputTensor(0)

        println(
            "MOBILEFACENET INPUT SHAPE: " +
                    inputTensor.shape().contentToString()
        )

        println(
            "MOBILEFACENET INPUT TYPE: " +
                    inputTensor.dataType()
        )

        println(
            "MOBILEFACENET OUTPUT SHAPE: " +
                    outputTensor.shape().contentToString()
        )

        println(
            "MOBILEFACENET OUTPUT TYPE: " +
                    outputTensor.dataType()
        )
    }


    /*
     * =========================================================
     * GET FACE EMBEDDING
     * =========================================================
     */

    fun getEmbedding(
        bitmap: Bitmap
    ): FloatArray {

        val tflite =
            interpreter
                ?: throw IllegalStateException(
                    "FaceEmbeddingProcessor is not initialized"
                )


        /*
         * Resize face to MobileFaceNet input size.
         */

        val resizedBitmap =
            Bitmap.createScaledBitmap(
                bitmap,
                INPUT_SIZE,
                INPUT_SIZE,
                true
            )


        /*
         * -----------------------------------------------------
         * INPUT BUFFER
         * -----------------------------------------------------
         *
         * Model expects:
         *
         * 2 x 112 x 112 x 3
         *
         * Each value is FLOAT32 = 4 bytes.
         */

        val inputBuffer =
            ByteBuffer
                .allocateDirect(
                    BATCH_SIZE *
                            INPUT_SIZE *
                            INPUT_SIZE *
                            3 *
                            4
                )
                .order(
                    ByteOrder.nativeOrder()
                )


        /*
         * Read pixels once.
         */

        val pixels =
            IntArray(
                INPUT_SIZE *
                        INPUT_SIZE
            )

        resizedBitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )


        /*
         * -----------------------------------------------------
         * CREATE TWO IDENTICAL BATCH ENTRIES
         * -----------------------------------------------------
         *
         * The model expects two images.
         *
         * We don't actually need a pair here,
         * so we duplicate the same face image.
         */

        repeat(BATCH_SIZE) {

            for (pixel in pixels) {

                val red =
                    (pixel shr 16) and 0xFF

                val green =
                    (pixel shr 8) and 0xFF

                val blue =
                    pixel and 0xFF


                /*
                 * MobileFaceNet normalization:
                 *
                 * 0..255 -> -1..1
                 */

                inputBuffer.putFloat(
                    (red / 128.0f) - 1.0f
                )

                inputBuffer.putFloat(
                    (green / 128.0f) - 1.0f
                )

                inputBuffer.putFloat(
                    (blue / 128.0f) - 1.0f
                )
            }
        }


        inputBuffer.rewind()


        /*
         * -----------------------------------------------------
         * OUTPUT
         * -----------------------------------------------------
         *
         * Expected:
         *
         * [2][192]
         */

        val output =
            Array(BATCH_SIZE) {
                FloatArray(
                    EMBEDDING_SIZE
                )
            }


        /*
         * RUN MODEL
         */

        tflite.run(
            inputBuffer,
            output
        )


        /*
         * Use the first embedding.
         *
         * Since both inputs were identical,
         * output[0] and output[1] should be
         * effectively the same.
         */

        return normalizeEmbedding(
            output[0]
        )
    }


    /*
     * =========================================================
     * L2 NORMALIZATION
     * =========================================================
     */

    private fun normalizeEmbedding(
        embedding: FloatArray
    ): FloatArray {

        var sum = 0f

        for (value in embedding) {

            sum +=
                value * value
        }


        val magnitude =
            sqrt(sum)


        if (magnitude == 0f) {

            return embedding
        }


        return FloatArray(
            embedding.size
        ) { index ->

            embedding[index] /
                    magnitude
        }
    }


    /*
     * =========================================================
     * COSINE SIMILARITY
     * =========================================================
     */

    fun cosineSimilarity(
        embedding1: FloatArray,
        embedding2: FloatArray
    ): Float {

        if (
            embedding1.size !=
            embedding2.size
        ) {

            throw IllegalArgumentException(
                "Embedding sizes must match"
            )
        }


        var dotProduct = 0f


        for (i in embedding1.indices) {

            dotProduct +=
                embedding1[i] *
                        embedding2[i]
        }


        return dotProduct
    }


    /*
     * =========================================================
     * CLOSE MODEL
     * =========================================================
     */

    fun close() {

        interpreter?.close()

        interpreter = null
    }
}