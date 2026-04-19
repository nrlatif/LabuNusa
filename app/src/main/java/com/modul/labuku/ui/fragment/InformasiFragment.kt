package com.modul.labuku.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import com.modul.labuku.databinding.FragmentInformasiBinding

class InformasiFragment : Fragment() {

    private var _binding: FragmentInformasiBinding? = null
    private val binding get() = _binding!!

    // Status expand/collapse tiap kartu
    private var sehatDibuka = false
    private var embunDibuka = false
    private var bercakDibuka = false
    private var layuDibuka = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInformasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aturAkordion(
            header = binding.headerSehat,
            konten = binding.kontenSehat,
            ikon = binding.ikonSehat,
            isOpen = { sehatDibuka },
            setOpen = { sehatDibuka = it }
        )

        aturAkordion(
            header = binding.headerEmbun,
            konten = binding.kontenEmbun,
            ikon = binding.ikonEmbun,
            isOpen = { embunDibuka },
            setOpen = { embunDibuka = it }
        )

        aturAkordion(
            header = binding.headerBercak,
            konten = binding.kontenBercak,
            ikon = binding.ikonBercak,
            isOpen = { bercakDibuka },
            setOpen = { bercakDibuka = it }
        )

        aturAkordion(
            header = binding.headerLayu,
            konten = binding.kontenLayu,
            ikon = binding.ikonLayu,
            isOpen = { layuDibuka },
            setOpen = { layuDibuka = it }
        )
    }

    private fun aturAkordion(
        header: View,
        konten: View,
        ikon: android.widget.ImageView,
        isOpen: () -> Boolean,
        setOpen: (Boolean) -> Unit
    ) {
        header.setOnClickListener {
            if (isOpen()) {
                // Tutup
                konten.animate()
                    .alpha(0f)
                    .scaleY(0.95f)
                    .setDuration(200)
                    .withEndAction { konten.visibility = View.GONE }
                    .start()
                ikon.animate().rotation(0f).setDuration(200).start()
                setOpen(false)
            } else {
                // Buka
                konten.alpha = 0f
                konten.scaleY = 0.95f
                konten.visibility = View.VISIBLE
                konten.animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .start()
                ikon.animate().rotation(180f).setDuration(200).start()
                setOpen(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
