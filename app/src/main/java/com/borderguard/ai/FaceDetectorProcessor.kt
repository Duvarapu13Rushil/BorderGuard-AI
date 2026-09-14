package com.borderguard.ai

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

data class DetectedFace(
    val boundingBox: Rect,
    val croppedBitmap: Bitmap
)

object FaceDetectorProcessor {

    private val detectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

    private val detector =
        FaceDetection.getClient(detectorOptions)

    fun detectFace(
        bitmap: Bitmap,
        onSuccess: (DetectedFace) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->

                when {
                    faces.isEmpty() -> {
                        onFailure(
                            Exception("No face detected")
                        )
                    }

                    faces.size > 1 -> {

                        println("MULTIPLE FACES DETECTED: ${faces.size}")

                        faces.forEachIndexed { index, detectedFace ->
                            println(
                                "FACE $index: " +
                                        "left=${detectedFace.boundingBox.left}, " +
                                        "top=${detectedFace.boundingBox.top}, " +
                                        "right=${detectedFace.boundingBox.right}, " +
                                        "bottom=${detectedFace.boundingBox.bottom}, " +
                                        "area=${detectedFace.boundingBox.width() * detectedFace.boundingBox.height()}"
                            )
                        }

                        val largestFace = faces.maxByOrNull {
                            it.boundingBox.width() * it.boundingBox.height()
                        }

                        if (largestFace == null) {
                            onFailure(Exception("Could not select a face"))
                        } else {

                            try {
                                val croppedFace =
                                    cropFaceWithMargin(
                                        bitmap,
                                        largestFace.boundingBox
                                    )

                                onSuccess(
                                    DetectedFace(
                                        boundingBox = largestFace.boundingBox,
                                        croppedBitmap = croppedFace
                                    )
                                )

                            } catch (e: Exception) {
                                onFailure(e)
                            }
                        }
                    }

                    else -> {
                        val face = faces[0]
                        val box = face.boundingBox

                        try {
                            val croppedFace =
                                cropFaceWithMargin(bitmap, box)

                            onSuccess(
                                DetectedFace(
                                    boundingBox = box,
                                    croppedBitmap = croppedFace
                                )
                            )

                        } catch (e: Exception) {
                            onFailure(e)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    private fun cropFaceWithMargin(
        bitmap: Bitmap,
        box: Rect
    ): Bitmap {

        val marginX = (box.width() * 0.20f).toInt()
        val marginY = (box.height() * 0.30f).toInt()

        val left =
            (box.left - marginX).coerceAtLeast(0)

        val top =
            (box.top - marginY).coerceAtLeast(0)

        val right =
            (box.right + marginX).coerceAtMost(bitmap.width)

        val bottom =
            (box.bottom + marginY).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            throw Exception("Invalid face crop")
        }

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            width,
            height
        )
    }
}