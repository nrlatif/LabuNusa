package com.modul.LabuNusa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.modul.LabuNusa.R
import com.modul.LabuNusa.data.BasisDataAplikasi
import com.modul.LabuNusa.data.EntitasRiwayat
import com.modul.LabuNusa.databinding.FragmentBerandaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding
        get() = _binding!!
    private var entitasTerakhir: EntitasRiwayat? = null

    private val daftarTips =
            listOf(
                    "Periksa daun labu setiap 2–3 hari sekali untuk mendeteksi gejala penyakit sejak dini.",
                    "Siram tanaman labu di pagi hari agar daun tidak lembab saat malam yang dapat memicu jamur.",
                    "Pastikan jarak tanam cukup longgar agar sirkulasi udara baik dan mencegah embun tepung.",
                    "Gunakan pupuk organik secara rutin untuk menjaga daya tahan tanaman terhadap penyakit.",
                    "Bersihkan gulma di sekitar tanaman labu untuk mengurangi inang hama dan penyakit.",
                    "Putar lokasi tanam setiap musim untuk mencegah penumpukan patogen di tanah.",
                    "Hindari melukai batang atau daun saat memangkas agar tidak menjadi jalur masuk penyakit.",
                    "Semprotkan pestisida nabati seperti ekstrak nimba sebagai pencegahan alami penyakit daun.",
                    "Periksa bagian bawah daun — spora jamur sering muncul pertama kali di sisi bawah daun.",
                    "Tanaman yang terinfeksi berat sebaiknya segera dicabut agar tidak menular ke tanaman lain."
            )

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? com.modul.LabuNusa.MainActivity)?.daftarkanHeader(
                0,
                binding.headerBeranda
        )

        tampilkanSalamDanTanggal()
        tampilkanTipsHariIni()
        muatStatistikDariDatabase()

        binding.cardMulaiAnalisis.setOnClickListener {
            try {
                requireActivity().findViewById<android.view.View>(R.id.fab_scan).performClick()
            } catch (e: Exception) {
                android.util.Log.e("BerandaFragment", "Gagal membuka kamera", e)
            }
        }

        binding.cardHasilTerakhir.setOnClickListener {
            entitasTerakhir?.let { tampilkanDetailTerakhir(it) }
        }
    }

    private fun tampilkanSalamDanTanggal() {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val salam = "Selamat Datang di"
        binding.tvSalam.text = salam

        val formatTanggal = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID"))
        binding.tvTanggal.text = formatTanggal.format(Date()).uppercase()
    }

    private fun tampilkanTipsHariIni() {
        val indeksTips = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - 1) % daftarTips.size
        binding.tvTipsHariIni.text = daftarTips[indeksTips]
    }

    private fun muatStatistikDariDatabase() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    BasisDataAplikasi.bukaDatabase(requireContext())
                            .aksesRiwayat()
                            .ambilSemua()
                            .collect { semua ->
                                if (_binding == null) return@collect

                                val total = semua.size
                                val sehat =
                                        semua.count {
                                            it.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
                                        }
                                val sakit =
                                        semua.count {
                                            !it.hasilKlasifikasi.contains(
                                                    "Sehat",
                                                    ignoreCase = true
                                            ) &&
                                                    !it.hasilKlasifikasi.contains(
                                                            "Bukan",
                                                            ignoreCase = true
                                                    )
                                        }

                                binding.tvTotalAnalisis.text = total.toString()
                                binding.tvJumlahSehat.text = sehat.toString()
                                binding.tvJumlahSakit.text = sakit.toString()

                                if (semua.isNotEmpty()) {
                                    val terakhir = semua.first()
                                    entitasTerakhir = terakhir

                                    binding.layoutDataTerakhir.visibility = View.VISIBLE
                                    binding.tvBelumAdaRiwayat.visibility = View.GONE

                                    binding.tvHasilTerakhir.text = terakhir.hasilKlasifikasi
                                    binding.tvWaktuTerakhir.text =
                                            hitungSelisihWaktu(terakhir.waktuScan)

                                    val file = File(terakhir.lokasiGambar)
                                    if (file.exists()) {
                                        binding.imgHasilTerakhir.load(file) { crossfade(true) }
                                    }

                                    val isSehat =
                                            terakhir.hasilKlasifikasi.contains(
                                                    "Sehat",
                                                    ignoreCase = true
                                            )
                                    val isBukan =
                                            terakhir.hasilKlasifikasi.contains(
                                                    "Bukan",
                                                    ignoreCase = true
                                            )
                                    val (tagTeks, tagWarna) =
                                            when {
                                                isBukan -> "NON-DAUN" to R.color.teks_redup
                                                isSehat -> "SEHAT" to R.color.hijau_primer
                                                else -> "PENYAKIT" to R.color.merah_penyakit
                                            }
                                    binding.tvTagTerakhir.text = tagTeks
                                    binding.tvTagTerakhir.backgroundTintList =
                                            android.content.res.ColorStateList.valueOf(
                                                    ContextCompat.getColor(
                                                            requireContext(),
                                                            tagWarna
                                                    )
                                            )
                                    binding.tvHasilTerakhir.setTextColor(
                                            ContextCompat.getColor(requireContext(), tagWarna)
                                    )
                                } else {
                                    binding.layoutDataTerakhir.visibility = View.GONE
                                    binding.tvBelumAdaRiwayat.visibility = View.VISIBLE
                                }
                            }
                } catch (e: Exception) {
                    android.util.Log.e("BerandaFragment", "Gagal memuat statistik", e)
                }
            }
        }
    }

    private fun tampilkanDetailTerakhir(entitas: EntitasRiwayat) {
        val dialogView =
                android.view.LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_detail_riwayat, null)
        val dialog =
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()
        dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        val imgDetail = dialogView.findViewById<android.widget.ImageView>(R.id.imgDetail)
        val tvWaktuDetail = dialogView.findViewById<android.widget.TextView>(R.id.tvWaktuDetail)
        val tvTagDetail = dialogView.findViewById<android.widget.TextView>(R.id.tvTagDetail)
        val tvLabelDetail = dialogView.findViewById<android.widget.TextView>(R.id.tvLabelDetail)
        val tvSkorDetail = dialogView.findViewById<android.widget.TextView>(R.id.tvSkorDetail)
        val tvMitigasi = dialogView.findViewById<android.widget.TextView>(R.id.tvMitigasiDetail)
        val btnTutup = dialogView.findViewById<android.view.View>(R.id.btnTutupDetail)

        val format = java.text.SimpleDateFormat("dd MMM yyyy • HH:mm", java.util.Locale("id", "ID"))
        tvWaktuDetail.text = format.format(java.util.Date(entitas.waktuScan))
        tvLabelDetail.text = entitas.hasilKlasifikasi
        tvSkorDetail.text = "Kepercayaan: ${(entitas.skorKepercayaan * 100).toInt()}%"
        tvMitigasi.text =
                com.modul.LabuNusa.utils.SaranPenanganan.ambilSaran(entitas.hasilKlasifikasi)

        val isSehat = entitas.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
        val isBukan = entitas.hasilKlasifikasi.contains("Bukan", ignoreCase = true)
        val warnaId =
                when {
                    isBukan -> R.color.teks_redup
                    isSehat -> R.color.hijau_primer
                    else -> R.color.merah_penyakit
                }
        val warna = ContextCompat.getColor(requireContext(), warnaId)
        tvTagDetail.text =
                when {
                    isBukan -> "INVALID"
                    isSehat -> "SEHAT"
                    else -> "PENYAKIT"
                }
        tvTagDetail.setBackgroundColor(warna)
        tvLabelDetail.setTextColor(warna)

        val file = java.io.File(entitas.lokasiGambar)
        if (file.exists()) imgDetail.load(file) { crossfade(true) }
        else imgDetail.setImageResource(R.drawable.ic_daun_labu)

        btnTutup.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun hitungSelisihWaktu(waktuMs: Long): String {
        val selisihMs = System.currentTimeMillis() - waktuMs
        val menit = TimeUnit.MILLISECONDS.toMinutes(selisihMs)
        val jam = TimeUnit.MILLISECONDS.toHours(selisihMs)
        val hari = TimeUnit.MILLISECONDS.toDays(selisihMs)
        return when {
            menit < 1 -> "Baru saja"
            menit < 60 -> "$menit menit lalu"
            jam < 24 -> "$jam jam lalu"
            hari == 1L -> "Kemarin"
            else -> "$hari hari lalu"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
