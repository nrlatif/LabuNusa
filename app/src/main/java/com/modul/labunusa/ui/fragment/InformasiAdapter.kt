package com.modul.LabuNusa.ui.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.modul.LabuNusa.databinding.ItemInformasiBinding

class InformasiAdapter(
    private val daftar: List<ModelInformasi>
) : RecyclerView.Adapter<InformasiAdapter.InformasiViewHolder>() {

    inner class InformasiViewHolder(val binding: ItemInformasiBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InformasiViewHolder {
        val binding = ItemInformasiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InformasiViewHolder(binding)
    }

    override fun getItemCount() = daftar.size

    override fun onBindViewHolder(holder: InformasiViewHolder, position: Int) {
        val item = daftar[position]
        val ctx = holder.binding.root.context
        val warnaPrimary = ContextCompat.getColor(ctx, item.warnaPrimaryRes)
        val warnaLatar   = ContextCompat.getColor(ctx, item.warnaLatarRes)
        val warnaStroke  = ContextCompat.getColor(ctx, item.warnaStrokeRes)

        with(holder.binding) {
            cardInformasi.strokeColor = warnaStroke
            headerKartu.setBackgroundColor(warnaLatar)
            stripKiri.setBackgroundColor(warnaPrimary)

            tvJudul.text = item.judul
            tvSubjudul.text = item.subjudul
            tvSubjudul.setTextColor(warnaPrimary)
            ikonPanah.setColorFilter(warnaPrimary)

            val seksi = item.seksiBagian
            if (seksi.isNotEmpty()) {
                tvJudulSeksi1.text = seksi[0].judul
                tvJudulSeksi1.setTextColor(warnaPrimary)
                tvIsiSeksi1.text = seksi[0].isi
            }
            if (seksi.size >= 2) {
                tvJudulSeksi2.text = seksi[1].judul
                tvJudulSeksi2.setTextColor(warnaPrimary)
                tvIsiSeksi2.text = seksi[1].isi
            }
            if (seksi.size >= 3) {
                divider2.visibility = View.VISIBLE
                tvJudulSeksi3.visibility = View.VISIBLE
                tvIsiSeksi3.visibility = View.VISIBLE
                tvJudulSeksi3.text = seksi[2].judul
                tvJudulSeksi3.setTextColor(warnaPrimary)
                tvIsiSeksi3.text = seksi[2].isi
            } else {
                divider2.visibility = View.GONE
                tvJudulSeksi3.visibility = View.GONE
                tvIsiSeksi3.visibility = View.GONE
            }

            kontenKartu.visibility = if (item.sedangDibuka) View.VISIBLE else View.GONE
            ikonPanah.rotation = if (item.sedangDibuka) 180f else 0f

            headerKartu.setOnClickListener {
                item.sedangDibuka = !item.sedangDibuka
                if (item.sedangDibuka) {
                    kontenKartu.alpha = 0f
                    kontenKartu.scaleY = 0.95f
                    kontenKartu.visibility = View.VISIBLE
                    kontenKartu.animate().alpha(1f).scaleY(1f).setDuration(250).start()
                    ikonPanah.animate().rotation(180f).setDuration(200).start()
                } else {
                    kontenKartu.animate()
                        .alpha(0f).scaleY(0.95f).setDuration(200)
                        .withEndAction { kontenKartu.visibility = View.GONE }
                        .start()
                    ikonPanah.animate().rotation(0f).setDuration(200).start()
                }
            }
        }
    }
}
