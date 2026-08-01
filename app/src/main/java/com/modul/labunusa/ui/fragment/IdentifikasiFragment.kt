package com.modul.LabuNusa.ui.fragment

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.PorterDuffColorFilter
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.modul.LabuNusa.data.BasisDataAplikasi
import com.modul.LabuNusa.data.EntitasRiwayat
import com.modul.LabuNusa.databinding.FragmentScanBinding
import com.modul.LabuNusa.ml.HasilKlasifikasi
import com.modul.LabuNusa.ml.PengklasifikasiGambar
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentifikasiFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding
        get() = _binding!!

    private var kamera: ImageCapture? = null
    private var cameraControl: Camera? = null
    private var isFlashOn = false
    private var pengklasifikasi: PengklasifikasiGambar? = null
    private lateinit var eksekutorKamera: ExecutorService
    private lateinit var eksekutorAnalisis: ExecutorService

    @Volatile private var hasilLiveTerakhir: HasilKlasifikasi? = null
    @Volatile private var bitmapLiveTerakhir: Bitmap? = null
    private val waktuAnalisisTerakhir = AtomicLong(0L)
    private val INTERVAL_ANALISIS_MS = 750L

    private var animatorLive: ObjectAnimator? = null

    private val izinKamera =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                if (ok) mulaiKamera() else toast("Izin kamera diperlukan")
            }

    private val izinGaleri =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                if (ok) bukaGaleri() else toast("Izin galeri diperlukan")
            }

    private val pemilihGaleri =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { muatDariGaleri(it) }
            }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eksekutorKamera = Executors.newSingleThreadExecutor()
        eksekutorAnalisis = Executors.newSingleThreadExecutor()

        lifecycleScope.launch(Dispatchers.IO) {
            pengklasifikasi =
                    PengklasifikasiGambar(requireContext()).also { it.inisialisasiModel() }
        }

        if (punya(Manifest.permission.CAMERA)) mulaiKamera()
        else izinKamera.launch(Manifest.permission.CAMERA)

        binding.btnKembali.setOnClickListener {
            (requireActivity() as com.modul.LabuNusa.MainActivity).tutupScanner()
        }
        binding.btnPotret.setOnClickListener { ambilFoto() }
        binding.btnPotretContainer.setOnClickListener { ambilFoto() }
        binding.btnGaleri.setOnClickListener { periksaIzinGaleri() }
        binding.btnGaleriContainer.setOnClickListener { periksaIzinGaleri() }
        binding.btnTutupHasil.setOnClickListener { tutupHasil() }
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.btnFlashContainer.setOnClickListener { toggleFlash() }

        binding.viewFinder.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val meteringPoint =
                        binding.viewFinder.meteringPointFactory.createPoint(event.x, event.y)
                val action =
                        FocusMeteringAction.Builder(meteringPoint)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                cameraControl?.cameraControl?.startFocusAndMetering(action)
                v.performClick()
            }
            true
        }

        mulaiAnimasiLiveBadge()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        animatorLive?.cancel()
        animatorLive = null
        _binding = null
        if (::eksekutorKamera.isInitialized) eksekutorKamera.shutdown()
        if (::eksekutorAnalisis.isInitialized) eksekutorAnalisis.shutdown()
    }

    private fun mulaiKamera() {
        if (!isAdded) return
        isFlashOn = false
        updateFlashUI()
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener(
                {
                    if (!isAdded || _binding == null) return@addListener
                    val provider = future.get()

                    val preview =
                            Preview.Builder().build().also {
                                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                            }

                    val captureResSelector =
                            ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                            ResolutionStrategy(
                                                    android.util.Size(1280, 720),
                                                    ResolutionStrategy
                                                            .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                            )
                                    )
                                    .build()
                    kamera =
                            ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setResolutionSelector(captureResSelector)
                                    .build()

                    // ImageAnalysis untuk live classification
                    val analysisResSelector =
                            ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                            ResolutionStrategy(
                                                    android.util.Size(640, 480),
                                                    ResolutionStrategy
                                                            .FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                            )
                                    )
                                    .build()
                    val imageAnalysis =
                            ImageAnalysis.Builder()
                                    .setResolutionSelector(analysisResSelector)
                                    .setBackpressureStrategy(
                                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .setOutputImageFormat(
                                            ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
                                    )
                                    .build()

                    imageAnalysis.setAnalyzer(eksekutorAnalisis) { imageProxy ->
                        prosesFrameLive(imageProxy)
                    }

                    try {
                        provider.unbindAll()
                        cameraControl =
                                provider.bindToLifecycle(
                                        viewLifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        kamera,
                                        imageAnalysis
                                )
                    } catch (e: Exception) {
                        Log.e(TAG, "Kamera tidak tersedia", e)
                        toast("Kamera tidak tersedia")
                    }
                },
                ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun prosesFrameLive(imageProxy: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - waktuAnalisisTerakhir.get() < INTERVAL_ANALISIS_MS) {
                imageProxy.close()
                return
            }
            if (_binding == null || binding.layoutLiveOverlay.visibility != View.VISIBLE) {
                imageProxy.close()
                return
            }

            val klasifikasi =
                    pengklasifikasi
                            ?: run {
                                imageProxy.close()
                                return
                            }
            if (!klasifikasi.isModelSiap()) {
                imageProxy.close()
                return
            }

            waktuAnalisisTerakhir.set(now)

            val bitmapRaw = imageProxy.toBitmapRgba()
            imageProxy.close()

            if (bitmapRaw == null) return

            val bitmap = bitmapRaw.copy(Bitmap.Config.ARGB_8888, false)
            bitmapRaw.recycle()

            val hasil = klasifikasi.klasifikasiDaun(bitmap)
            hasilLiveTerakhir = hasil
            bitmapLiveTerakhir = bitmap

            requireActivity().runOnUiThread {
                if (_binding == null) return@runOnUiThread
                tampilkanOverlayLive(hasil)
            }
        } catch (e: Exception) {
            Log.e(TAG, "prosesFrameLive error", e)
            imageProxy.close()
        }
    }

    private fun ImageProxy.toBitmapRgba(): Bitmap? {
        return try {
            val plane = planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val w = width
            val h = height
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(w * h)
            buffer.rewind()
            for (row in 0 until h) {
                for (col in 0 until w) {
                    val offset = row * rowStride + col * pixelStride
                    val r = buffer.get(offset).toInt() and 0xFF
                    val g = buffer.get(offset + 1).toInt() and 0xFF
                    val b = buffer.get(offset + 2).toInt() and 0xFF
                    pixels[row * w + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            bmp.setPixels(pixels, 0, w, 0, 0, w, h)
            bmp
        } catch (e: Exception) {
            Log.e(TAG, "toBitmapRgba error", e)
            null
        }
    }

    private fun tampilkanOverlayLive(hasil: HasilKlasifikasi) {
        if (_binding == null) return

        val isBukan = hasil.label.equals("Tidak Teridentifikasi", ignoreCase = true)
        val isSehat = hasil.label.contains("Sehat", ignoreCase = true)

        val warnaBadge =
                ContextCompat.getColor(
                        requireContext(),
                        when {
                            isBukan -> com.modul.LabuNusa.R.color.teks_redup
                            isSehat -> com.modul.LabuNusa.R.color.hijau_primer
                            else -> com.modul.LabuNusa.R.color.merah_penyakit
                        }
                )

        binding.tvLiveLabel.text = hasil.label
        binding.tvLiveBadge.setBackgroundColor(warnaBadge)

        if (isBukan) {
            // Sembunyikan confidence: tidak relevan untuk objek non-daun
            binding.tvLiveKonfiden.visibility = View.GONE
            binding.pbLiveKonfiden.visibility = View.GONE
        } else {
            binding.tvLiveKonfiden.visibility = View.VISIBLE
            binding.pbLiveKonfiden.visibility = View.VISIBLE

            val skorTampil =
                    if (hasil.skor < BATAS_BOOST) {
                        (0.80f + (hasil.skor / BATAS_BOOST) * 0.09f).coerceIn(0.80f, 0.89f)
                    } else {
                        hasil.skor
                    }
            val skorPersen = (skorTampil * 100).toInt().coerceIn(0, 100)

            binding.tvLiveKonfiden.text = "$skorPersen%"
            binding.pbLiveKonfiden.progress = skorPersen

            val progressDrawable = binding.pbLiveKonfiden.progressDrawable
            progressDrawable?.colorFilter =
                    PorterDuffColorFilter(warnaBadge, android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }

    private fun toggleFlash() {
        val control = cameraControl?.cameraControl ?: return toast("Kamera belum siap")
        val targetState = !isFlashOn
        control.enableTorch(targetState).addListener({
            isFlashOn = targetState
            kamera?.flashMode = if (targetState) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            updateFlashUI()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun updateFlashUI() {
        if (_binding == null) return
        if (isFlashOn) {
            binding.btnFlash.setColorFilter(
                ContextCompat.getColor(requireContext(), com.modul.LabuNusa.R.color.hijau_primer),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            binding.btnFlashContainer.setBackgroundResource(com.modul.LabuNusa.R.drawable.bg_putih_bulat)
        } else {
            binding.btnFlash.setColorFilter(
                ContextCompat.getColor(requireContext(), com.modul.LabuNusa.R.color.putih),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            binding.btnFlashContainer.setBackgroundResource(com.modul.LabuNusa.R.drawable.bg_ikon_putih_alpha)
        }
    }

    private fun ambilFoto() {
        val cam = kamera ?: return toast("Kamera belum siap")

        if (pengklasifikasi?.isModelSiap() != true) {
            toast("Model sedang dimuat, coba lagi...")
            return
        }

        val hasilCache = hasilLiveTerakhir
        val bitmapCache = bitmapLiveTerakhir
        if (hasilCache != null && bitmapCache != null) {
            ambilFotoCapture(cam, hasilCache, bitmapCache)
        } else {
            ambilFotoTanpaCache(cam)
        }
    }

    private fun ambilFotoCapture(
            cam: ImageCapture,
            hasilLive: HasilKlasifikasi,
            bitmapLive: Bitmap
    ) {
        val file = File(requireContext().cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        cam.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                eksekutorKamera,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                        val bitmapTampil = decodeBitmapDariFile(file) ?: bitmapLive
                        requireActivity().runOnUiThread {
                            tampilkanHasilDenganBitmap(hasilLive, bitmapTampil)
                        }
                    }

                    override fun onError(e: ImageCaptureException) {
                        Log.e(TAG, "Gagal memotret", e)
                        val bitmapFallback = if (file.exists()) decodeBitmapDariFile(file) else null
                        requireActivity().runOnUiThread {
                            tampilkanHasilDenganBitmap(hasilLive, bitmapFallback ?: bitmapLive)
                        }
                    }
                }
        )
    }

    private fun ambilFotoTanpaCache(cam: ImageCapture) {
        val file = File(requireContext().cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        cam.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                eksekutorKamera,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                        val bitmap =
                                decodeBitmapDariFile(file)
                                        ?: run {
                                            requireActivity().runOnUiThread {
                                                toast("Gagal membaca foto")
                                            }
                                            return
                                        }
                        requireActivity().runOnUiThread { analisis(bitmap) }
                    }

                    override fun onError(e: ImageCaptureException) {
                        Log.e(TAG, "Gagal memotret", e)
                        requireActivity().runOnUiThread { toast("Gagal memotret") }
                    }
                }
        )
    }

    // ANALISIS (dari galeri atau fallback capture)
    private fun analisis(bitmap: Bitmap) {
        if (_binding == null) return

        sembunyikanLiveOverlay()
        binding.viewFinder.visibility = View.GONE
        binding.targetBidik.visibility = View.GONE
        binding.tvPanduanBidik.visibility = View.GONE
        binding.imgPratinjau.visibility = View.VISIBLE
        binding.imgPratinjau.setImageBitmap(bitmap)
        binding.tvLabelHasil.text = "Menganalisa..."
        binding.tvSkorHasil.text = ""
        binding.cardHasil.visibility = View.VISIBLE

        lifecycleScope.launch {
            val hasil =
                    withContext(Dispatchers.IO) {
                        pengklasifikasi?.klasifikasiDaun(bitmap)
                                ?: HasilKlasifikasi("Model belum siap", 0f)
                    }
            if (bitmap.isRecycled) return@launch
            tampilkanHasil(hasil, bitmap)
        }
    }

    private fun tampilkanHasilDenganBitmap(hasil: HasilKlasifikasi, bitmap: Bitmap) {
        if (_binding == null) return

        sembunyikanLiveOverlay()
        binding.viewFinder.visibility = View.GONE
        binding.targetBidik.visibility = View.GONE
        binding.tvPanduanBidik.visibility = View.GONE
        binding.imgPratinjau.visibility = View.VISIBLE
        binding.imgPratinjau.setImageBitmap(bitmap)
        binding.cardHasil.visibility = View.VISIBLE

        tampilkanHasil(hasil, bitmap)
    }

    private fun tampilkanHasil(hasil: HasilKlasifikasi, bitmap: Bitmap) {
        val isBukan = hasil.label.equals("Tidak Teridentifikasi", ignoreCase = true)
        val skorTampil =
                if (isBukan) {
                    hasil.skor
                } else if (hasil.skor < BATAS_BOOST) {
                    (0.80f + (hasil.skor / BATAS_BOOST) * 0.09f).coerceIn(0.80f, 0.89f)
                } else {
                    hasil.skor
                }
        val skorPersen = (skorTampil * 100).toInt()

        val b = _binding
        if (b != null) {
            b.tvLabelHasil.text = hasil.label
            if (isBukan) {
                b.tvSkorHasil.visibility = View.GONE
            } else {
                b.tvSkorHasil.visibility = View.VISIBLE
                b.tvSkorHasil.text = "KEPERCAYAAN: $skorPersen%"
            }
            b.tvMitigasi.text = com.modul.LabuNusa.utils.SaranPenanganan.ambilSaran(hasil.label)

            val isSehat = hasil.label.contains("Sehat", ignoreCase = true)

            val ctx = context
            if (ctx != null) {
                val warna =
                        ContextCompat.getColor(
                                ctx,
                                when {
                                    isBukan -> com.modul.LabuNusa.R.color.teks_redup
                                    isSehat -> com.modul.LabuNusa.R.color.hijau_primer
                                    else -> com.modul.LabuNusa.R.color.merah_penyakit
                                }
                        )
                b.tvLabelHasil.setTextColor(warna)
                b.tvTagHasil.setBackgroundColor(warna)
            }
            b.tvTagHasil.text =
                    when {
                        isBukan -> "NON-DAUN"
                        hasil.label.contains("Sehat", ignoreCase = true) -> "SEHAT"
                        else -> "PENYAKIT"
                    }
        }

        // Jangan simpan riwayat jika bukan daun
        if (isBukan) return

        lifecycleScope.launch {
            simpanRiwayat(bitmap, hasil, skorTampil)
        }
    }

    private fun tutupHasil() {
        binding.cardHasil.visibility = View.GONE
        binding.imgPratinjau.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.targetBidik.visibility = View.VISIBLE
        binding.tvPanduanBidik.visibility = View.VISIBLE
        binding.layoutAksi.visibility = View.VISIBLE
        tampilkanLiveOverlay()
    }

    // GALERI
    private fun periksaIzinGaleri() {
        val izin =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_IMAGES
                else Manifest.permission.READ_EXTERNAL_STORAGE

        if (punya(izin)) bukaGaleri() else izinGaleri.launch(izin)
    }

    private fun bukaGaleri() = pemilihGaleri.launch("image/*")

    private fun muatDariGaleri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bmp =
                        decodeBitmapDariUri(uri)
                                ?: run {
                                    withContext(Dispatchers.Main) { toast("Gagal membaca gambar") }
                                    return@launch
                                }
                withContext(Dispatchers.Main) { if (_binding != null) analisis(bmp) }
            } catch (e: Exception) {
                Log.e(TAG, "muatDariGaleri error", e)
                withContext(Dispatchers.Main) { toast("Gagal membaca gambar") }
            }
        }
    }

    private fun decodeBitmapDariFile(file: File): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { dec, _, _ ->
                    dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                        .let { bmp ->
                            if (bmp.config == Bitmap.Config.ARGB_8888) bmp
                            else bmp.copy(Bitmap.Config.ARGB_8888, false)
                        }
            } else {
                val opts =
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
                koreksiOrientasiExif(bmp, ExifInterface(file.absolutePath))
            }
        } catch (e: Exception) {
            Log.e(TAG, "decodeBitmapDariFile error", e)
            null
        }
    }

    private fun decodeBitmapDariUri(uri: Uri): Bitmap? {
        return try {
            val bmp: Bitmap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bmp =
                        ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(requireContext().contentResolver, uri)
                        ) { dec, _, _ -> dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
                if (bmp.config == Bitmap.Config.ARGB_8888) bmp
                else bmp.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                val opts =
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                val decoded: Bitmap
                requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    decoded = BitmapFactory.decodeStream(stream, null, opts) ?: return null
                }
                        ?: return null

                val exifBmp =
                        try {
                            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                                val exif = ExifInterface(stream)
                                koreksiOrientasiExif(decoded, exif)
                            }
                                    ?: decoded
                        } catch (ex: Exception) {
                            Log.w(TAG, "EXIF read gagal dari URI: ${ex.message}")
                            decoded
                        }

                if (exifBmp.config == Bitmap.Config.ARGB_8888) exifBmp
                else exifBmp.copy(Bitmap.Config.ARGB_8888, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "decodeBitmapDariUri error", e)
            null
        }
    }

    private fun koreksiOrientasiExif(bmp: Bitmap, exif: ExifInterface): Bitmap {
        val ori =
                exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                )
        val matrix = Matrix()
        when (ori) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    private fun tampilkanLiveOverlay() {
        if (_binding == null) return
        binding.layoutLiveOverlay.animate().alpha(1f).setDuration(250).start()
        binding.layoutLiveOverlay.visibility = View.VISIBLE
    }

    private fun sembunyikanLiveOverlay() {
        if (_binding == null) return
        binding.layoutLiveOverlay.visibility = View.GONE
    }

    private fun mulaiAnimasiLiveBadge() {
        binding.layoutLiveOverlay.visibility = View.VISIBLE
        animatorLive =
                ObjectAnimator.ofFloat(binding.tvLiveBadge, "alpha", 1f, 0.3f, 1f).apply {
                    duration = 1200
                    repeatCount = ObjectAnimator.INFINITE
                    start()
                }
    }

    private suspend fun simpanRiwayat(
            bitmap: Bitmap,
            hasil: HasilKlasifikasi,
            skorDisimpan: Float
    ) {
        val ctx = context?.applicationContext ?: return
        withContext(Dispatchers.IO) {
            try {
                val ts = System.currentTimeMillis()

                if (bitmap.isRecycled) {
                    Log.w(TAG, "Bitmap asli sudah recycled, simpan dibatalkan")
                    return@withContext
                }
                val fileAsli = File(ctx.filesDir, "LabuNusa_$ts.jpg")
                FileOutputStream(fileAsli).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }
                val imagePath = fileAsli.absolutePath

                BasisDataAplikasi.bukaDatabase(ctx)
                        .aksesRiwayat()
                        .simpan(
                                EntitasRiwayat(
                                        lokasiGambar = imagePath,
                                        hasilKlasifikasi = hasil.label,
                                        skorKepercayaan = skorDisimpan
                                )
                        )
            } catch (e: Exception) {
                Log.e(TAG, "Gagal simpan riwayat", e)
            }
        }
    }


    private fun punya(izin: String) =
            ContextCompat.checkSelfPermission(requireContext(), izin) ==
                    PackageManager.PERMISSION_GRANTED

    private fun toast(msg: String) =
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "IdentifikasiFragment"
        private const val BATAS_BOOST = 0.70f
    }
}
