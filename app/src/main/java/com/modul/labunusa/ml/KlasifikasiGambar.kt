package com.modul.LabuNusa.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import org.tensorflow.lite.Interpreter

data class HasilKlasifikasi(val label: String, val skor: Float)

class PengklasifikasiGambar(private val konteks: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val modelSiap = AtomicBoolean(false)

    fun inisialisasiModel() {
        try {
            val fd = konteks.assets.openFd(MODEL_FILE)
            val channel = FileInputStream(fd.fileDescriptor).channel
            val buffer =
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            channel.close()

            interpreter = Interpreter(buffer, Interpreter.Options().apply { numThreads = 2 })

            labels =
                    konteks.assets
                            .open(LABELS_FILE)
                            .bufferedReader()
                            .readLines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

            modelSiap.set(true)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat model '$MODEL_FILE'", e)
            modelSiap.set(false)
            interpreter = null
        }
    }

    fun klasifikasiDaun(bitmap: Bitmap): HasilKlasifikasi {
        if (!modelSiap.get() || interpreter == null) {
            return HasilKlasifikasi("Model belum siap", 0f)
        }
        if (labels.isEmpty()) {
            return HasilKlasifikasi("Labels kosong", 0f)
        }

        return try {
            val src = bitmap.copy(Bitmap.Config.ARGB_8888, false)

            val W = INPUT_SIZE
            val H = INPUT_SIZE
            val scaled = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            Canvas(scaled)
                    .drawBitmap(
                            src,
                            Rect(0, 0, src.width, src.height),
                            Rect(0, 0, W, H),
                            Paint(Paint.FILTER_BITMAP_FLAG)
                    )

            val pixels = IntArray(W * H)
            scaled.getPixels(pixels, 0, W, 0, 0, W, H)

            val buf =
                    ByteBuffer.allocateDirect(1 * W * H * 3 * Float.SIZE_BYTES)
                            .order(ByteOrder.nativeOrder())

            for (px in pixels) {
                buf.putFloat(((px shr 16) and 0xFF).toFloat())
                buf.putFloat(((px shr 8) and 0xFF).toFloat())
                buf.putFloat((px and 0xFF).toFloat())
            }
            buf.rewind()

            val numClass = interpreter!!.getOutputTensor(0).shape().last()
            val out = Array(1) { FloatArray(numClass) }
            interpreter!!.run(buf, out)
            val scores = out[0]

            val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
            val maxScore = scores[maxIdx]

            if (maxScore < THRESHOLD_DAUN) {
                return HasilKlasifikasi("Tidak Teridentifikasi", maxScore.coerceIn(0f, 1f))
            }

            val rawLabel = labels.getOrElse(maxIdx) { "Tidak Dikenali" }
            HasilKlasifikasi(rawLabel.replace("_", " "), maxScore.coerceIn(0f, 1f))
        } catch (e: Exception) {
            Log.e(TAG, "Error saat inference", e)
            HasilKlasifikasi("Error: ${e.message?.take(60)}", 0f)
        }
    }

    fun isModelSiap(): Boolean = modelSiap.get()

    companion object {
        private const val TAG = "Pengklasifikasi"
        private const val MODEL_FILE = "mobilenetv2_labuV3_float32V5.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224
        const val THRESHOLD_DAUN = 0.7f
    }
}
