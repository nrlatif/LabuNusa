package com.modul.LabuNusa.ui.fragment

import android.view.LayoutInflater
import android.view.View
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
    private val onKlik: (EntitasRiwayat) -> Unit,
    private val onSeleksiChange: (jumlah: Int) -> Unit,
    private val onModeSeleksiChange: (aktif: Boolean) -> Unit
) : ListAdapter<EntitasRiwayat, RiwayatAdapter.RiwayatViewHolder>(DiffCallback) {

    private val itemTerpilih = mutableSetOf<Int>()
    var modeSeleksi = false
        private set

    fun masukModeSeleksi() {
        modeSeleksi = true
        onModeSeleksiChange(true)
        notifyDataSetChanged()
    }

    fun keluarModeSeleksi() {
        modeSeleksi = false
        itemTerpilih.clear()
        onModeSeleksiChange(false)
        onSeleksiChange(0)
        notifyDataSetChanged()
    }

    fun pilihSemua() {
        currentList.forEach { itemTerpilih.add(it.idRiwayat) }
        onSeleksiChange(itemTerpilih.size)
        notifyDataSetChanged()
    }

    fun ambilItemTerpilih(): List<EntitasRiwayat> =
        currentList.filter { it.idRiwayat in itemTerpilih }

    inner class RiwayatViewHolder(private val binding: ItemRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun ikat(entitas: EntitasRiwayat) {
            val ctx = binding.root.context
            val isSehat = entitas.hasilKlasifikasi.contains("Sehat", ignoreCase = true)
            val isBukan = entitas.hasilKlasifikasi.contains("Bukan", ignoreCase = true)

            binding.tvHasilRiwayat.text = entitas.hasilKlasifikasi
            binding.tvSkorRiwayat.text = "Kepercayaan: ${(entitas.skorKepercayaan * 100).toInt()}%"
            val format = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
            binding.tvWaktuRiwayat.text = format.format(Date(entitas.waktuScan))

            val warnaId = when {
                isBukan -> R.color.teks_redup
                isSehat -> R.color.hijau_primer
                else -> R.color.merah_penyakit
            }
            val warna = ContextCompat.getColor(ctx, warnaId)
            binding.stripKiri.setBackgroundColor(warna)
            binding.tvKategoriRiwayat.text = when {
                isBukan -> "INVALID"
                isSehat -> "SEHAT"
                else -> "PENYAKIT"
            }
            binding.tvKategoriRiwayat.setBackgroundColor(warna)
            val fileAnotasi = entitas.lokasiGambarAnotasi?.let { File(it) }
            val fileGambar = File(entitas.lokasiGambar)
            when {
                fileAnotasi != null && fileAnotasi.exists() ->
                    binding.imgRiwayat.load(fileAnotasi) { crossfade(true) }
                fileGambar.exists() ->
                    binding.imgRiwayat.load(fileGambar) { crossfade(true) }
                else ->
                    binding.imgRiwayat.setImageResource(R.drawable.ic_daun_labu)
            }
            val terpilih = entitas.idRiwayat in itemTerpilih
            if (modeSeleksi) {
                binding.btnHapus.visibility = View.GONE
                binding.overlaySeleksi.visibility = if (terpilih) View.VISIBLE else View.GONE
                binding.checkboxSeleksi.visibility = View.VISIBLE
                binding.checkboxSeleksi.isChecked = terpilih
                binding.cardRiwayat.strokeWidth =
                    if (terpilih) ctx.resources.getDimensionPixelSize(R.dimen.stroke_selected)
                    else ctx.resources.getDimensionPixelSize(R.dimen.stroke_normal)
                binding.cardRiwayat.strokeColor =
                    if (terpilih) ContextCompat.getColor(ctx, R.color.hijau_primer)
                    else ContextCompat.getColor(ctx, R.color.abu_garis)
            } else {
                binding.btnHapus.visibility = View.VISIBLE
                binding.overlaySeleksi.visibility = View.GONE
                binding.checkboxSeleksi.visibility = View.GONE
                binding.cardRiwayat.strokeWidth =
                    ctx.resources.getDimensionPixelSize(R.dimen.stroke_normal)
                binding.cardRiwayat.strokeColor =
                    ContextCompat.getColor(ctx, R.color.abu_garis)
            }

            binding.root.setOnLongClickListener {
                if (!modeSeleksi) {
                    masukModeSeleksi()
                    toggleSeleksi(entitas)
                }
                true
            }

            binding.root.setOnClickListener {
                if (modeSeleksi) toggleSeleksi(entitas)
                else onKlik(entitas)
            }

            binding.btnHapus.setOnClickListener {
                tampilkanDialogHapus(
                    ctx = ctx,
                    judul = "Hapus Riwayat",
                    pesan = "Yakin ingin menghapus \"${entitas.hasilKlasifikasi}\"?\nTindakan ini tidak dapat dibatalkan.",
                    onKonfirmasi = { onHapus(entitas) }
                )
            }
        }

        private fun toggleSeleksi(entitas: EntitasRiwayat) {
            if (entitas.idRiwayat in itemTerpilih) itemTerpilih.remove(entitas.idRiwayat)
            else itemTerpilih.add(entitas.idRiwayat)
            onSeleksiChange(itemTerpilih.size)
            notifyItemChanged(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val binding = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RiwayatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        holder.ikat(getItem(position))
    }

    companion object {
        fun tampilkanDialogHapus(
            ctx: android.content.Context,
            judul: String,
            pesan: String,
            onKonfirmasi: () -> Unit
        ) {
            val view = android.view.LayoutInflater.from(ctx)
                .inflate(com.modul.LabuNusa.R.layout.dialog_konfirmasi_hapus, null)
            view.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvJudulDialog).text = judul
            view.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.tvPesanDialog).text = pesan

            val dialog = android.app.Dialog(ctx)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(view)
            dialog.window?.apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setLayout(
                    (ctx.resources.displayMetrics.widthPixels * 0.88).toInt(),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
                )
                view.background = androidx.core.content.ContextCompat.getDrawable(
                    ctx, com.modul.LabuNusa.R.drawable.bg_dialog_rounded
                )
            }

            view.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.btnBatalDialog)
                .setOnClickListener { dialog.dismiss() }
            view.findViewById<android.widget.TextView>(com.modul.LabuNusa.R.id.btnHapusDialog)
                .setOnClickListener { dialog.dismiss(); onKonfirmasi() }

            dialog.show()
        }

        private val DiffCallback = object : DiffUtil.ItemCallback<EntitasRiwayat>() {
            override fun areItemsTheSame(oldItem: EntitasRiwayat, newItem: EntitasRiwayat) =
                oldItem.idRiwayat == newItem.idRiwayat
            override fun areContentsTheSame(oldItem: EntitasRiwayat, newItem: EntitasRiwayat) =
                oldItem == newItem
        }
    }
}
