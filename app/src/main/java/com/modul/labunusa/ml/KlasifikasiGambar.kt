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

/**
 * PengklasifikasiGambar – TFLite inference untuk model MobileNetV2 LabuNusa.
 *
 * MODEL: mobilenetv2_labuV2_float32.tflite
 *   Dilatih ulang dengan arsitektur:
 *     inputs → preprocess_input(embedded) → MobileNetV2(frozen) → GAP → Dropout → Dense(softmax)
 *
 * KONTRAK INPUT (harus cocok dengan pipeline Colab):
 *   • Shape  : [1, 224, 224, 3]  float32
 *   • Nilai  : R, G, B masing-masing 0.0f – 255.0f  (RAW, tanpa normalisasi di Android)
 *   • Urutan : R → G → B  per piksel
 *
 * Kenapa Android TIDAK perlu normalisasi?
 *   Pada kode Colab, `preprocess_input` diterapkan sebagai operasi Keras pada tensor input
 *   (x = preprocess_input(inputs)) — bukan di pipeline dataset eksternal. Artinya layer
 *   normalisasi tersebut ikut ter-embed saat model di-export ke TFLite.
 *   Model menerima 0-255 RAW dan secara internal mengonversi ke -1..1 sendiri.
 *
 * LABEL ORDER (harus sama dengan urutan output node model):
 *   0: Bercak_Daun
 *   1: Bukan_Daun
 *   2: Daun Sehat
 *   3: Embun Tepung
 *   4: Layu_Fusarium
 */
class PengklasifikasiGambar(private val konteks: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val modelSiap = AtomicBoolean(false)

    /** Ringkasan debug terakhir, boleh ditampilkan di UI saat testing. */
    var debugInfo: String = ""

    // ─── Inisialisasi ────────────────────────────────────────────────────────

    fun inisialisasiModel() {
        try {
            val fd = konteks.assets.openFd(MODEL_FILE)
            val channel = FileInputStream(fd.fileDescriptor).channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            channel.close()

            val opts = Interpreter.Options().apply {
                numThreads = 2
            }
            interpreter = Interpreter(buffer, opts)

            // Baca labels – urutan HARUS sama dengan output node model
            labels = konteks.assets.open(LABELS_FILE)
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // ── Debug: cetak metadata tensor ──────────────────────────────
            val inTensor  = interpreter!!.getInputTensor(0)
            val outTensor = interpreter!!.getOutputTensor(0)
            Log.d(TAG, "=== MODEL LOADED ===")
            Log.d(TAG, "Input  tensor : dtype=${inTensor.dataType()}  shape=${inTensor.shape().toList()}")
            Log.d(TAG, "Output tensor : dtype=${outTensor.dataType()} shape=${outTensor.shape().toList()}")
            Log.d(TAG, "Labels (${labels.size}) : $labels")
            Log.d(TAG, "Expected input: FLOAT32 [1,224,224,3]  nilai 0-255 RAW (preprocess_input embedded di model)")

            modelSiap.set(true)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat model '$MODEL_FILE'", e)
            modelSiap.set(false)
            interpreter = null
        }
    }

    // ─── Klasifikasi ─────────────────────────────────────────────────────────

    /**
     * Terima bitmap berformat ARGB_8888 (ukuran bebas), resize ke 224×224,
     * jadikan float32 RAW 0-255, jalankan inference, kembalikan label + skor.
     *
     * Fungsi ini AMAN dipanggil dari thread IO maupun Main (tidak mutasi UI).
     */
    fun klasifikasiDaun(bitmap: Bitmap): HasilKlasifikasi {
        if (!modelSiap.get() || interpreter == null) {
            Log.w(TAG, "klasifikasiDaun dipanggil sebelum model siap!")
            return HasilKlasifikasi("Model belum siap", 0f)
        }
        if (labels.isEmpty()) {
            Log.w(TAG, "Labels kosong!")
            return HasilKlasifikasi("Labels kosong", 0f)
        }

        return try {
            // ── STEP 1: Paksa software ARGB_8888 ────────────────────────
            // Hardware bitmap (config=ARGB_8888 tapi allocator=HARDWARE) → getPixels() semua 0.
            // .copy() dengan isMutable=false menjamin software rendering.
            val src = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            Log.d(TAG, "[IN ] Bitmap asal  : ${src.width}×${src.height}  config=${src.config}")

            // ── STEP 2: Direct resize (stretch) ke 224×224 ──────────────
            //
            // MENGAPA TIDAK center-crop?
            //   Keras ImageDataGenerator / image_dataset_from_directory dengan
            //   target_size=(224,224) melakukan DIRECT RESIZE (stretch) ke seluruh gambar,
            //   BUKAN center-crop. Jika Android melakukan center-crop, distribusi konten
            //   yang diterima model berbeda dari data training:
            //
            //   • Foto galeri  (sering mendekati 1:1) → crop kecil → relatif aman
            //   • Foto kamera  (4:3 atau 16:9)        → crop besar → isi berubah → melenceng ❌
            //
            //   Solusi: stretch seluruh gambar ke 224×224 agar identik dengan preprocessing
            //   training. Model sudah belajar dari gambar yang di-stretch, sehingga stretch
            //   juga yang harus diberikan saat inference.
            val W = INPUT_SIZE; val H = INPUT_SIZE
            val scaled = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            Canvas(scaled).drawBitmap(
                src,
                Rect(0, 0, src.width, src.height),            // src: seluruh gambar
                Rect(0, 0, W, H),                             // dst: 224×224 (stretch)
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            Log.d(TAG, "[IN ] Resize stretch: ${src.width}×${src.height} → ${W}×${H}")

            // ── STEP 3: Pixel → ByteBuffer float32 RAW 0-255 ───────────
            //   Format : R G B  per piksel, urutan row-major
            //   TIDAK ADA pembagian 255f, TIDAK ADA pengurangan mean
            val pixels = IntArray(W * H)
            scaled.getPixels(pixels, 0, W, 0, 0, W, H)

            val buf = ByteBuffer
                .allocateDirect(1 * W * H * 3 * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())

            for (px in pixels) {
                buf.putFloat(((px shr 16) and 0xFF).toFloat()) // R  0-255
                buf.putFloat(((px shr  8) and 0xFF).toFloat()) // G  0-255
                buf.putFloat(( px         and 0xFF).toFloat()) // B  0-255
            }
            buf.rewind()

            // ── Debug: sample pixel di pojok kiri-atas & tengah ─────────
            val r0 = buf.getFloat(0);  val g0 = buf.getFloat(4);  val b0 = buf.getFloat(8)
            val ci = (H / 2) * W + (W / 2)
            val cp = pixels[ci]
            val rc = (cp shr 16) and 0xFF
            val gc = (cp shr  8) and 0xFF
            val bc =  cp         and 0xFF
            Log.d(TAG, "[IN ] Pixel[0,0]    : R=%.0f G=%.0f B=%.0f".format(r0, g0, b0))
            Log.d(TAG, "[IN ] Pixel[center] : R=$rc G=$gc B=$bc")
            Log.d(TAG, "[IN ] ByteBuffer    : capacity=${buf.capacity()} bytes, position=${buf.position()}")
            buf.rewind()

            // ── STEP 4: Inference ────────────────────────────────────────
            val numClass = interpreter!!.getOutputTensor(0).shape().last()
            val out = Array(1) { FloatArray(numClass) }
            interpreter!!.run(buf, out)
            val scores = out[0]

            // ── STEP 5: Log semua skor ───────────────────────────────────
            val scoreLines = scores.mapIndexed { i, v ->
                val lbl = labels.getOrElse(i) { "class_$i" }
                "  [$i] ${lbl.padEnd(16)} = ${"%.6f".format(v)}"
            }
            Log.d(TAG, "[OUT] Semua skor (${scores.size} kelas):")
            scoreLines.forEach { Log.d(TAG, it) }

            // ── STEP 6: Argmax ───────────────────────────────────────────
            val maxIdx   = scores.indices.maxByOrNull { scores[it] } ?: 0
            val maxScore = scores[maxIdx]
            val rawLabel = labels.getOrElse(maxIdx) { "Tidak Dikenali" }
            Log.d(TAG, "[OUT] Prediksi: '$rawLabel' idx=$maxIdx skor=${"%.4f".format(maxScore * 100)}%")

            // Simpan ringkasan debug
            debugInfo = buildString {
                appendLine("=== DEBUG PENGKLASIFIKASI ===")
                appendLine("Model : mobilenetv2_labuV2_float32.tflite")
                appendLine("Input : FLOAT32 [1,$W,$H,3]  nilai 0-255 RAW (preprocess_input embedded)")
                appendLine("Pixel[0,0]  : R=${"%.0f".format(r0)} G=${"%.0f".format(g0)} B=${"%.0f".format(b0)}")
                appendLine("Pixel[mid]  : R=$rc G=$gc B=$bc")
                appendLine("--- Scores ---")
                scoreLines.forEach { appendLine(it) }
                appendLine("Prediksi: $rawLabel (${"%.1f".format(maxScore * 100)}%)")
            }

            // Ganti underscore → spasi agar cocok dengan SaranPenanganan
            val labelBersih = rawLabel.replace("_", " ")
            HasilKlasifikasi(labelBersih, maxScore.coerceIn(0f, 1f))

        } catch (e: Exception) {
            Log.e(TAG, "Error saat inference", e)
            HasilKlasifikasi("Error: ${e.message?.take(60)}", 0f)
        }
    }

    /** Apakah model sudah berhasil dimuat? */
    fun isModelSiap(): Boolean = modelSiap.get()

    companion object {
        private const val TAG         = "Pengklasifikasi"
        private const val MODEL_FILE  = "mobilenetv2_labuV2_float32.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE  = 224
    }
}
