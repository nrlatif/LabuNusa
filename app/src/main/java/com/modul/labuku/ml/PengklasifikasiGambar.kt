package com.modul.labuku.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class HasilKlasifikasi(val label: String, val skor: Float)

class PengklasifikasiGambar(private val konteks: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var imageSizeX = 224
    private var imageSizeY = 224
    private var isQuantized = false

    private var initError: String? = null

    fun inisialisasiModel() {
        try {
            // Memuat file model secara mentah (kompatibel untuk model dengan atau tanpa metadata)
            val fileDescriptor = konteks.assets.openFd("model_labu.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            
            // Memuat labels.txt secara manual
            val tempLabels = mutableListOf<String>()
            konteks.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { if (it.trim().isNotEmpty()) tempLabels.add(it.trim()) }
            }
            labels = tempLabels
            
            // Baca arsitektur input tensor otomatis dari model
            val inputTensor = interpreter?.getInputTensor(0)
            if (inputTensor != null) {
                val shape = inputTensor.shape()
                if (shape.size >= 3) {
                    imageSizeY = shape[1] // Tinggi (Height)
                    imageSizeX = shape[2] // Lebar (Width)
                }
                isQuantized = (inputTensor.dataType() == DataType.UINT8)
            }
            
            Log.d("Pengklasifikasi", "Berhasil memuat model TFLite ($imageSizeX x $imageSizeY) Quantized: $isQuantized")
        } catch (e: Exception) {
            initError = "Error: " + (e.message ?: e.toString())
            Log.e("Pengklasifikasi", "Gagal memuat model.", e)
            interpreter = null
        }
    }

    fun klasifikasiDaun(gambarUtama: Bitmap): HasilKlasifikasi {
        if (interpreter == null) {
            return HasilKlasifikasi(initError ?: "Sedang Memuat Model...", 0.0f)
        }
        if (labels.isEmpty()) {
            return HasilKlasifikasi("Error: Labels Kosong", 0.0f)
        }

        try {
            // 1. Pre-processing: Skala (Squash) langsung seperti Keras load_img
            // (Tanpa center crop agar 100% sama dengan perilaku Python di Colab)
            val scaledBitmap = Bitmap.createScaledBitmap(gambarUtama, imageSizeX, imageSizeY, true)
            
            // 2. Allocate buffer dynamically depending on quantization
            val bytesPerChannel = if (isQuantized) 1 else 4
            val inputBuffer = ByteBuffer.allocateDirect(1 * imageSizeX * imageSizeY * 3 * bytesPerChannel)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            val intValues = IntArray(imageSizeX * imageSizeY)
            scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
            
            var pixel = 0
            for (i in 0 until imageSizeY) { // iterasi tinggi
                for (j in 0 until imageSizeX) { // iterasi lebar
                    val valPixel = intValues[pixel++]
                    if (isQuantized) {
                        // Integer casting 0-255
                        inputBuffer.put((valPixel shr 16 and 0xFF).toByte()) // R
                        inputBuffer.put((valPixel shr 8 and 0xFF).toByte())  // G
                        inputBuffer.put((valPixel and 0xFF).toByte())        // B
                    } else {
                        // Float normalize. 
                        // Normalisasi MobileNetV2 seperti di Colab (-1 s/d 1)
                        inputBuffer.putFloat(((valPixel shr 16 and 0xFF) / 127.5f) - 1.0f)
                        inputBuffer.putFloat(((valPixel shr 8 and 0xFF) / 127.5f) - 1.0f)
                        inputBuffer.putFloat(((valPixel and 0xFF) / 127.5f) - 1.0f)
                    }
                }
            }

            // Mencegah error JNI dengan me-reset posisi Buffer ke awal
            inputBuffer.rewind()

            // 3. Setup output buffers
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape() 
            val numLabels = outputShape[outputShape.size - 1] // usually [1, num_labels]
            
            val isOutputQuantized = (outputTensor.dataType() == DataType.UINT8 || outputTensor.dataType() == DataType.INT8)
            var maxIndex = -1
            var maxScore = -Float.MAX_VALUE // Menggunakan MIN_VALUE untuk mengantisipasi Logits model yang bisa negatif

            var savedFloats: FloatArray? = null

            // 4. Inference & Post-processing (Argmax)
            if (isOutputQuantized) {
                val outputBuffer = Array(1) { ByteArray(numLabels) }
                interpreter?.run(inputBuffer, outputBuffer)
                val bytes = outputBuffer[0]
                val tmpFloats = FloatArray(numLabels)
                for (i in bytes.indices) {
                    val rawVal = if (outputTensor.dataType() == DataType.INT8) {
                        bytes[i].toInt() // -128..127 signed
                    } else {
                        bytes[i].toInt() and 0xFF // 0..255 unsigned
                    }
                    val floatVal = rawVal / 255.0f
                    tmpFloats[i] = floatVal
                    if (floatVal > maxScore) {
                        maxScore = floatVal
                        maxIndex = i
                    }
                }
                savedFloats = tmpFloats
            } else {
                val outputBuffer = Array(1) { FloatArray(numLabels) }
                interpreter?.run(inputBuffer, outputBuffer)
                val floats = outputBuffer[0]
                savedFloats = floats
                for (i in floats.indices) {
                    if (floats[i] > maxScore) {
                        maxScore = floats[i]
                        maxIndex = i
                    }
                }
            }

            // Mencegah akurasi lebih dari 100% jika model outputnya berbentuk logits
            if (maxScore > 1.0f) {
                // Konversi Logits semu untuk tampilan yang ramah di UI
                maxScore = 0.99f 
            } else if (maxScore < 0.0f) {
                maxScore = 0.01f
            }

            // 5. Interpret valid classifications
            if (maxIndex != -1 && maxIndex < labels.size) {
                val cleanLabel = labels[maxIndex].replace("_", " ")
                
                return HasilKlasifikasi(cleanLabel, maxScore)
            }
        } catch (e: Exception) {
            Log.e("Pengklasifikasi", "Kesalahan saat klasifikasi", e)
        }

        return HasilKlasifikasi("Tidak Dikenali", 0.0f)
    }
}
