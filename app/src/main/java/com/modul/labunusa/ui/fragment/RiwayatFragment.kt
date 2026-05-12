package com.modul.LabuNusa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.modul.LabuNusa.data.BasisDataAplikasi
import com.modul.LabuNusa.data.EntitasRiwayat
import com.modul.LabuNusa.databinding.FragmentRiwayatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    private var semuaData: List<EntitasRiwayat> = emptyList()
    private var filterAktif = "SEMUA" // SEMUA | SEHAT | PENYAKIT | NON_DAUN

    private lateinit var adapter: RiwayatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? com.modul.LabuNusa.MainActivity)?.daftarkanHeader(
            1, binding.headerRiwayat
        )

        siapkanAdapter()
        siapkanFilter()
        siapkanToolbarSeleksi()
        muatData()
    }

    private fun siapkanAdapter() {
        adapter = RiwayatAdapter(
            onHapus = { entitas -> hapusSatu(entitas) },
            onKlik = { entitas -> tampilkanDetailPenyakit(entitas) },
            onSeleksiChange = { jumlah ->
                binding.tvJumlahSeleksi.text = "$jumlah item dipilih"
            },
            onModeSeleksiChange = { aktif ->
                if (aktif) {
                    binding.toolbarSeleksi.visibility = View.VISIBLE
                    binding.scrollFilter.visibility = View.GONE
                } else {
                    binding.toolbarSeleksi.visibility = View.GONE
                    binding.scrollFilter.visibility = View.VISIBLE
                }
            }
        )
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter
    }

    private fun siapkanFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            filterAktif = when {
                checkedIds.contains(binding.chipSehat.id) -> "SEHAT"
                checkedIds.contains(binding.chipPenyakit.id) -> "PENYAKIT"
                checkedIds.contains(binding.chipNonDaun.id) -> "NON_DAUN"
                else -> "SEMUA"
            }
            terapkanFilter()
        }
    }

    private fun siapkanToolbarSeleksi() {
        binding.btnBatalSeleksi.setOnClickListener {
            adapter.keluarModeSeleksi()
        }

        binding.btnPilihSemua.setOnClickListener {
            adapter.pilihSemua()
        }

        binding.btnHapusSeleksi.setOnClickListener {
            val terpilih = adapter.ambilItemTerpilih()
            if (terpilih.isEmpty()) {
                Toast.makeText(requireContext(), "Pilih item terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            RiwayatAdapter.tampilkanDialogHapus(
                ctx = requireContext(),
                judul = "Hapus ${terpilih.size} Riwayat",
                pesan = "Yakin ingin menghapus ${terpilih.size} riwayat yang dipilih?\nTindakan ini tidak dapat dibatalkan.",
                onKonfirmasi = { hapusMassal(terpilih) }
            )
        }
    }

    private fun terapkanFilter() {
        val filtered = when (filterAktif) {
            "SEHAT" -> semuaData.filter {
                it.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
            }
            "PENYAKIT" -> semuaData.filter {
                !it.hasilKlasifikasi.contains("Sehat", ignoreCase = true) &&
                !it.hasilKlasifikasi.contains("Bukan", ignoreCase = true)
            }
            "NON_DAUN" -> semuaData.filter {
                it.hasilKlasifikasi.contains("Bukan", ignoreCase = true)
            }
            else -> semuaData
        }

        if (filtered.isEmpty() && semuaData.isNotEmpty()) {
            binding.tvKosong.visibility = View.VISIBLE
            binding.rvRiwayat.visibility = View.GONE
        } else if (filtered.isNotEmpty()) {
            binding.tvKosong.visibility = View.GONE
            binding.rvRiwayat.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private fun muatData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dao = BasisDataAplikasi.bukaDatabase(requireContext()).aksesRiwayat()
                dao.ambilSemua()
                    .flowOn(Dispatchers.IO)
                    .catch { if (_binding != null) tampilkanKosong() }
                    .collectLatest { daftar ->
                        if (_binding == null) return@collectLatest
                        semuaData = daftar
                        if (daftar.isEmpty()) tampilkanKosong()
                        else terapkanFilter()
                    }
            } catch (_: Exception) {
                if (_binding != null) tampilkanKosong()
            }
        }
    }

    private fun hapusSatu(entitas: EntitasRiwayat) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            BasisDataAplikasi.bukaDatabase(requireContext()).aksesRiwayat().hapus(entitas)
            try { java.io.File(entitas.lokasiGambar).takeIf { it.exists() }?.delete() } catch (_: Exception) {}
        }
    }

    private fun hapusMassal(daftar: List<EntitasRiwayat>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val dao = BasisDataAplikasi.bukaDatabase(requireContext()).aksesRiwayat()
            daftar.forEach { entitas ->
                dao.hapus(entitas)
                try { java.io.File(entitas.lokasiGambar).takeIf { it.exists() }?.delete() } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    adapter.keluarModeSeleksi()
                    Toast.makeText(requireContext(), "${daftar.size} riwayat dihapus", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun tampilkanKosong() {
        binding.tvKosong.visibility = View.VISIBLE
        binding.rvRiwayat.visibility = View.GONE
    }

    private fun tampilkanDetailPenyakit(entitas: EntitasRiwayat) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.modul.LabuNusa.R.layout.dialog_detail_riwayat, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        val imgDetail = dialogView.findViewById<android.widget.ImageView>(com.modul.LabuNusa.R.id.imgDetail)
        val tvWaktuDetail = dialogView.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvWaktuDetail)
        val tvTagDetail = dialogView.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvTagDetail)
        val tvLabelDetail = dialogView.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvLabelDetail)
        val tvSkorDetail = dialogView.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvSkorDetail)
        val tvMitigasiDetail = dialogView.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvMitigasiDetail)
        val btnTutupDetail = dialogView.findViewById<android.view.View>(com.modul.LabuNusa.R.id.btnTutupDetail)

        val format = java.text.SimpleDateFormat("dd MMM yyyy • HH:mm", java.util.Locale("id", "ID"))
        tvWaktuDetail.text = format.format(java.util.Date(entitas.waktuScan))
        tvLabelDetail.text = entitas.hasilKlasifikasi
        tvSkorDetail.text = "Kepercayaan: ${(entitas.skorAkurasi * 100).toInt()}%"
        tvMitigasiDetail.text = com.modul.LabuNusa.utils.SaranPenanganan.ambilSaran(entitas.hasilKlasifikasi)

        val isSehat = entitas.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
        val isBukan = entitas.hasilKlasifikasi.contains("Bukan", ignoreCase = true)
        val warnaId = when { isBukan -> com.modul.LabuNusa.R.color.teks_redup; isSehat -> com.modul.LabuNusa.R.color.hijau_primer; else -> com.modul.LabuNusa.R.color.merah_penyakit }
        val warna = androidx.core.content.ContextCompat.getColor(requireContext(), warnaId)
        val kategoriTeks = when { isBukan -> "INVALID"; isSehat -> "SEHAT"; else -> "PENYAKIT" }
        tvTagDetail.text = kategoriTeks
        tvTagDetail.setBackgroundColor(warna)
        tvLabelDetail.setTextColor(warna)

        val fileAnotasi = entitas.lokasiGambarAnotasi?.let { java.io.File(it) }
        val fileGambar = java.io.File(entitas.lokasiGambar)
        when {
            fileAnotasi != null && fileAnotasi.exists() -> {
                imgDetail.load(fileAnotasi) { crossfade(true) }
                imgDetail.setOnClickListener { tampilkanFotoPenuh(fileAnotasi) }
            }
            fileGambar.exists() -> {
                imgDetail.load(fileGambar) { crossfade(true) }
                imgDetail.setOnClickListener { tampilkanFotoPenuh(fileGambar) }
            }
            else -> imgDetail.setImageResource(com.modul.LabuNusa.R.drawable.ic_daun_labu)
        }

        btnTutupDetail.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun tampilkanFotoPenuh(file: java.io.File) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imgView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        if (bmp != null) imgView.setImageBitmap(bmp)
        dialog.setContentView(imgView)
        dialog.window?.apply {
            setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT
            )
            addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        imgView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
