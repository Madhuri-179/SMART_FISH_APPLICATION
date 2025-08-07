package com.swapna.fishidentificationapp

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

class FishClassifier(context: Context) {
    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputImageSize = 224  // depends on your model
    private val inputChannels = 3

    init {
        val model = loadModelFile(context, "fish_classifier.tflite")
        interpreter = Interpreter(model)
        labels = context.assets.open("labels.txt").bufferedReader().readLines()
    }

    private fun loadModelFile(context: Context, modelFile: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(bitmap: Bitmap): String {
        val resized = bitmap.scale(inputImageSize, inputImageSize)
        val byteBuffer = convertBitmapToByteBuffer(resized)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(byteBuffer, output)

        val predictions = output[0]
        val maxIndex = predictions.indices.maxByOrNull { predictions[it] } ?: -1
        val confidence = predictions[maxIndex]

        return if (maxIndex != -1) {
            "${labels[maxIndex]} (Confidence: ${(confidence * 100).format(2)}%)"
        } else {
            "Unknown"
        }
    }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageSize * inputImageSize * inputChannels)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageSize * inputImageSize)
        bitmap.getPixels(pixels, 0, inputImageSize, 0, 0, inputImageSize, inputImageSize)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }
}
