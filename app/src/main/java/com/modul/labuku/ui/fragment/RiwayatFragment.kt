package com.modul.labuku.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.modul.labuku.data.BasisDataAplikasi
import com.modul.labuku.databinding.FragmentRiwayatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import coil.load

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RiwayatAdapter(
            onHapus = { entitas ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    BasisDataAplikasi.dapatkanBasisData(requireContext()).daoRiwayat().hapusRiwayat(entitas)
                    // Hapus file gambar lokal agar tidak memenuhi memori
                    try {
                        val file = java.io.File(entitas.jalurGambarLokal)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            },
            onKlik = { entitas ->
                tampilkanDetailPenyakit(entitas)
            }
        )

        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dao = BasisDataAplikasi.dapatkanBasisData(requireContext()).daoRiwayat()
                dao.ambilSemuaRiwayat()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        e.printStackTrace()
                        if (_binding != null) tampilkanKosong()
                    }
                    .collectLatest { daftar ->
                        if (_binding == null) return@collectLatest
                        if (daftar.isEmpty()) {
                            tampilkanKosong()
                        } else {
                            binding.tvKosong.visibility = View.GONE
                            binding.rvRiwayat.visibility = View.VISIBLE
                            adapter.submitList(daftar)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding != null) tampilkanKosong()
            }
        }
    }

    private fun tampilkanKosong() {
        binding.tvKosong.visibility = View.VISIBLE
        binding.rvRiwayat.visibility = View.GONE
    }

    private fun tampilkanDetailPenyakit(entitas: com.modul.labuku.data.EntitasRiwayat) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(com.modul.labuku.R.layout.dialog_detail_riwayat, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
            
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val imgDetail = dialogView.findViewById<android.widget.ImageView>(com.modul.labuku.R.id.imgDetail)
        val tvWaktuDetail = dialogView.findViewById<android.widget.TextView>(com.modul.labuku.R.id.tvWaktuDetail)
        val tvTagDetail = dialogView.findViewById<android.widget.TextView>(com.modul.labuku.R.id.tvTagDetail)
        val tvLabelDetail = dialogView.findViewById<android.widget.TextView>(com.modul.labuku.R.id.tvLabelDetail)
        val tvSkorDetail = dialogView.findViewById<android.widget.TextView>(com.modul.labuku.R.id.tvSkorDetail)
        val tvMitigasiDetail = dialogView.findViewById<android.widget.TextView>(com.modul.labuku.R.id.tvMitigasiDetail)
        val btnTutupDetail = dialogView.findViewById<android.view.View>(com.modul.labuku.R.id.btnTutupDetail)

        val format = java.text.SimpleDateFormat("dd MMM yyyy • HH:mm", java.util.Locale("id", "ID"))
        tvWaktuDetail.text = format.format(java.util.Date(entitas.waktuPotret))
        
        tvLabelDetail.text = entitas.hasilKlasifikasi
        val persen = (entitas.skorAkurasi * 100).toInt()
        tvSkorDetail.text = "Akurasi: $persen%"
        
        tvMitigasiDetail.text = com.modul.labuku.utils.HelperMitigasi.dapatkanMitigasi(entitas.hasilKlasifikasi)
        
        val isSehat = entitas.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
        val isBukan = entitas.hasilKlasifikasi.contains("Bukan", ignoreCase = true)
        
        val warnaId = when {
            isBukan -> com.modul.labuku.R.color.teks_redup
            isSehat -> com.modul.labuku.R.color.hijau_primer
            else -> com.modul.labuku.R.color.merah_penyakit
        }
        val warna = androidx.core.content.ContextCompat.getColor(requireContext(), warnaId)
        
        tvTagDetail.text = when {
            isBukan -> "INVALID"
            isSehat -> "SEHAT"
            else -> "PENYAKIT"
        }
        tvTagDetail.setBackgroundColor(warna)
        tvLabelDetail.setTextColor(warna)
        
        val fileGambar = java.io.File(entitas.jalurGambarLokal)
        if (fileGambar.exists()) {
            imgDetail.load(fileGambar) { crossfade(true) }
        } else {
            imgDetail.setImageResource(com.modul.labuku.R.drawable.ic_daun_labu)
        }
        
        btnTutupDetail.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
