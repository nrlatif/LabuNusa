package com.modul.LabuNusa.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentifikasiFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding
        get() = _binding!!

    private var kamera: ImageCapture? = null
    private var cameraControl: Camera? = null
    private var pengklasifikasi: PengklasifikasiGambar? = null
    private lateinit var eksekutorKamera: ExecutorService

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

        // Tap-to-focus pada viewFinder
        binding.viewFinder.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val meteringPoint = binding.viewFinder.meteringPointFactory
                    .createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(meteringPoint)
                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                cameraControl?.cameraControl?.startFocusAndMetering(action)
                v.performClick()
            }
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (::eksekutorKamera.isInitialized) eksekutorKamera.shutdown()
    }

    private fun mulaiKamera() {
        if (!isAdded) return
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener(
                {
                    if (!isAdded || _binding == null) return@addListener
                    val provider = future.get()
                    val preview =
                            Preview.Builder().build().also {
                                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                            }
                    kamera =
                            ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setTargetResolution(Size(1280, 720))
                                    .build()
                    try {
                        provider.unbindAll()
                        cameraControl = provider.bindToLifecycle(
                                viewLifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                kamera
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Kamera tidak tersedia", e)
                        toast("Kamera tidak tersedia")
                    }
                },
                ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun ambilFoto() {
        val cam = kamera ?: return toast("Kamera belum siap")

        if (pengklasifikasi?.isModelSiap() != true) {
            toast("Model sedang dimuat, coba lagi...")
            Log.w(TAG, "ambilFoto() dipanggil sebelum model siap")
            return
        }

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

    private fun analisis(bitmap: Bitmap) {
        if (_binding == null) return

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
            if (_binding == null) return@launch
            tampilkanHasil(hasil, bitmap)
        }
    }

    private fun tampilkanHasil(hasil: HasilKlasifikasi, bitmap: Bitmap) {
        binding.tvLabelHasil.text = hasil.label
        binding.tvSkorHasil.text = "AKURASI: ${(hasil.skor * 100).toInt()}%"
        binding.tvMitigasi.text = com.modul.LabuNusa.utils.SaranPenanganan.ambilSaran(hasil.label)

        val isBukan = hasil.label.contains("Bukan", ignoreCase = true)
        val isSehat = hasil.label.contains("Sehat", ignoreCase = true)

        val warna =
                ContextCompat.getColor(
                        requireContext(),
                        when {
                            isBukan -> com.modul.LabuNusa.R.color.teks_redup
                            isSehat -> com.modul.LabuNusa.R.color.hijau_primer
                            else -> com.modul.LabuNusa.R.color.merah_penyakit
                        }
                )
        binding.tvLabelHasil.setTextColor(warna)
        binding.tvTagHasil.setBackgroundColor(warna)
        binding.tvTagHasil.text =
                when {
                    isBukan -> "NON-DAUN"
                    isSehat -> "SEHAT"
                    else -> "PENYAKIT"
                }

        simpanRiwayat(bitmap, hasil)
    }

    private fun tutupHasil() {
        binding.cardHasil.visibility = View.GONE
        binding.imgPratinjau.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.targetBidik.visibility = View.VISIBLE
        binding.tvPanduanBidik.visibility = View.VISIBLE
        binding.layoutAksi.visibility = View.VISIBLE
    }

    private fun simpanRiwayat(bitmap: Bitmap, hasil: HasilKlasifikasi) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ctx = requireContext().applicationContext
                val file = File(ctx.filesDir, "LabuNusa_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                BasisDataAplikasi.bukaDatabase(ctx)
                        .aksesRiwayat()
                        .simpan(
                                EntitasRiwayat(
                                        lokasiGambar = file.absolutePath,
                                        hasilKlasifikasi = hasil.label,
                                        skorAkurasi = hasil.skor
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
    }
}
