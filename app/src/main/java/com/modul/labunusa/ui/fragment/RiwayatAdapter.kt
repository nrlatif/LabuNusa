package com.modul.LabuNusa.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.modul.LabuNusa.R
import com.modul.LabuNusa.data.EntitasRiwayat
import com.modul.LabuNusa.databinding.ItemRiwayatBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RiwayatAdapter(
    private val onHapus: (EntitasRiwayat) -> Unit,
    private val onKlik: (EntitasRiwayat) -> Unit
) : ListAdapter<EntitasRiwayat, RiwayatAdapter.RiwayatViewHolder>(DiffCallback) {

    inner class RiwayatViewHolder(private val binding: ItemRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun ikat(entitas: EntitasRiwayat) {
            binding.tvHasilRiwayat.text = entitas.hasilKlasifikasi
            val persen = (entitas.skorAkurasi * 100).toInt()
            binding.tvSkorRiwayat.text = "Akurasi: $persen%"

            val format = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
            binding.tvWaktuRiwayat.text = format.format(Date(entitas.waktuScan))

            // Tentukan warna berdasarkan label hasil
            val isSehat = entitas.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
            val isBukan = entitas.hasilKlasifikasi.contains("Bukan", ignoreCase = true)
            
            val warnaId = when {
                isBukan -> R.color.teks_redup
                isSehat -> R.color.hijau_primer
                else -> R.color.merah_penyakit
            }
            val teksKategori = when {
                isBukan -> "INVALID"
                isSehat -> "SEHAT"
                else -> "PENYAKIT"
            }
            val warna = ContextCompat.getColor(binding.root.context, warnaId)

            // Strip kanan
            binding.stripKiri.setBackgroundColor(warna)

            // Badge kategori
            binding.tvKategoriRiwayat.text = teksKategori
            binding.tvKategoriRiwayat.setBackgroundColor(warna)

            // Muat gambar
            val fileGambar = File(entitas.lokasiGambar)
            if (fileGambar.exists()) {
                binding.imgRiwayat.load(fileGambar) { crossfade(true) }
            } else {
                binding.imgRiwayat.setImageResource(R.drawable.ic_daun_labu)
            }
            
            // Tombol Hapus — dengan dialog konfirmasi
            binding.btnHapus.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                    .setTitle("Hapus Riwayat")
                    .setMessage("Yakin ingin menghapus data analisis \"${entitas.hasilKlasifikasi}\"?\nTindakan ini tidak dapat dibatalkan.")
                    .setPositiveButton("Hapus") { _, _ ->
                        onHapus(entitas)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            
            // Tombol klik seluruh item
            binding.root.setOnClickListener {
                onKlik(entitas)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val binding = ItemRiwayatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RiwayatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        holder.ikat(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<EntitasRiwayat>() {
            override fun areItemsTheSame(oldItem: EntitasRiwayat, newItem: EntitasRiwayat) =
                oldItem.idRiwayat == newItem.idRiwayat

            override fun areContentsTheSame(oldItem: EntitasRiwayat, newItem: EntitasRiwayat) =
                oldItem == newItem
        }
    }
}
