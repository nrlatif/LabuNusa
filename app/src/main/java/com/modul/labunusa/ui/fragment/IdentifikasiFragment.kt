package com.modul.LabuNusa.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdentifikasiFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private var kamera: ImageCapture? = null
    private var pengklasifikasi: PengklasifikasiGambar? = null
    private lateinit var eksekutorKamera: ExecutorService

    private val launcherIzinKamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diizinkan ->
        if (diizinkan) mulaiKamera()
        else Toast.makeText(requireContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    private val launcherIzinGaleri = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diizinkan ->
        if (diizinkan) bukaGaleri()
        else Toast.makeText(requireContext(), "Izin galeri diperlukan", Toast.LENGTH_SHORT).show()
    }

    private val pemilihGaleri = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { klasifikasiDariGaleri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eksekutorKamera = Executors.newSingleThreadExecutor()

        // Inisialisasi model di latar
        lifecycleScope.launch(Dispatchers.IO) {
            pengklasifikasi = PengklasifikasiGambar(requireContext())
            pengklasifikasi?.inisialisasiModel()
        }

        // Cek izin kamera
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mulaiKamera()
        } else {
            launcherIzinKamera.launch(Manifest.permission.CAMERA)
        }

        binding.btnKembali.setOnClickListener { (requireActivity() as com.modul.LabuNusa.MainActivity).tutupScanner() }
        binding.btnPotretContainer.setOnClickListener { tangkapFoto() }
        binding.btnPotret.setOnClickListener { tangkapFoto() }
        binding.btnGaleriContainer.setOnClickListener { periksaIzinGaleri() }
        binding.btnGaleri.setOnClickListener { periksaIzinGaleri() }
        binding.btnTutupHasil.setOnClickListener { tutupHasil() }
    }

    private fun tutupHasil() {
        binding.cardHasil.visibility = View.GONE
        binding.imgPratinjau.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.targetBidik.visibility = View.VISIBLE
        binding.tvPanduanBidik.visibility = View.VISIBLE
        binding.layoutAksi.visibility = View.VISIBLE
    }

    private fun periksaIzinGaleri() {
        val izin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), izin)
            == PackageManager.PERMISSION_GRANTED
        ) bukaGaleri()
        else launcherIzinGaleri.launch(izin)
    }

    private fun bukaGaleri() = pemilihGaleri.launch("image/*")

    private fun mulaiKamera() {
        if (!isAdded) return
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            if (!isAdded || _binding == null) return@addListener
            val provider = future.get()
            val pratinjau = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            kamera = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    pratinjau, kamera
                )
            } catch (exc: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun tangkapFoto() {
        val pengambil = kamera ?: run {
            Toast.makeText(requireContext(), "Kamera belum siap", Toast.LENGTH_SHORT).show()
            return
        }
        val fileCache = File(requireContext().cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val opsiOutput = ImageCapture.OutputFileOptions.Builder(fileCache).build()

        pengambil.takePicture(opsiOutput, eksekutorKamera,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    var bitmap = BitmapFactory.decodeFile(fileCache.absolutePath) ?: return
                    
                    // Memperbaiki anomali rotasi EXIF dari gambar asli kamera (mencegah landscape tak sengaja)
                    try {
                        val exif = android.media.ExifInterface(fileCache.absolutePath)
                        val orientasi = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                        val matrix = android.graphics.Matrix()
                        when (orientasi) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    requireActivity().runOnUiThread { prosesGambar(mutable) }
                }
                override fun onError(exc: ImageCaptureException) {
                    requireActivity().runOnUiThread {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal memotret", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun klasifikasiDariGaleri(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(requireContext().contentResolver, uri)
                ) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
            prosesGambar(bitmap.copy(Bitmap.Config.ARGB_8888, true))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prosesGambar(bitmap: Bitmap) {
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
            val hasil = withContext(Dispatchers.IO) {
                pengklasifikasi?.klasifikasiDaun(bitmap)
                    ?: HasilKlasifikasi("Daun Sehat (Simulasi)", 0.92f)
            }
            if (_binding == null) return@launch

            binding.tvLabelHasil.text = hasil.label
            binding.tvSkorHasil.text = "AKURASI: ${(hasil.skor * 100).toInt()}%"
            binding.tvMitigasi.text = com.modul.LabuNusa.utils.SaranPenanganan.ambilSaran(hasil.label)

            val isSehat = hasil.label.contains("Sehat", ignoreCase = true)
            val isBukan = hasil.label.contains("Bukan", ignoreCase = true)
            
            val warnaId = when {
                isBukan -> com.modul.LabuNusa.R.color.teks_redup
                isSehat -> com.modul.LabuNusa.R.color.hijau_primer
                else -> com.modul.LabuNusa.R.color.merah_penyakit
            }
            val tagTeks = when {
                isBukan -> "NON-DAUN"
                isSehat -> "SEHAT"
                else -> "HASIL ANALISIS"
            }

            binding.tvLabelHasil.setTextColor(ContextCompat.getColor(requireContext(), warnaId))
            binding.tvTagHasil.setBackgroundColor(ContextCompat.getColor(requireContext(), warnaId))
            binding.tvTagHasil.text = tagTeks
            simpanRiwayat(bitmap, hasil)
        }
    }

    private suspend fun simpanRiwayat(bitmap: Bitmap, hasil: HasilKlasifikasi) {
        withContext(Dispatchers.IO) {
            try {
                // Gunakan application context agar tidak hilang ketika fragment ditutup
                val ctx = requireContext().applicationContext
                val file = File(ctx.filesDir, "LabuNusa_${System.currentTimeMillis()}.jpg")
                val os = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, os)
                os.flush()
                os.close()
                
                val entitas = EntitasRiwayat(
                    lokasiGambar = file.absolutePath,
                    hasilKlasifikasi = hasil.label,
                    skorAkurasi = hasil.skor
                )
                BasisDataAplikasi.bukaDatabase(ctx).aksesRiwayat().simpan(entitas)
                android.util.Log.d("ScanFragment", "Berhasil menyimpan riwayat: ${entitas.hasilKlasifikasi}")
            } catch (e: Exception) { 
                android.util.Log.e("ScanFragment", "Gagal menyimpan riwayat", e) 
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (::eksekutorKamera.isInitialized) eksekutorKamera.shutdown()
    }
}
