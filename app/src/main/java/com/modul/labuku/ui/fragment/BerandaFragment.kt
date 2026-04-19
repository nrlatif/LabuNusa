package com.modul.labuku.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.modul.labuku.R
import com.modul.labuku.databinding.FragmentBerandaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tampilkanTanggal()

        binding.cardMulaiScan.setOnClickListener {
            navigateTo(R.id.nav_scan)
        }

        binding.cardRiwayat.setOnClickListener {
            navigateTo(R.id.nav_riwayat)
        }

        binding.cardInformasi.setOnClickListener {
            navigateTo(R.id.nav_informasi)
        }

        binding.cardTentang.setOnClickListener {
            navigateTo(R.id.nav_tentang)
        }
    }

    private fun navigateTo(destinationId: Int) {
        try {
            val mainActivity = requireActivity() as com.modul.labuku.MainActivity
            val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)
            when (destinationId) {
                R.id.nav_scan -> {
                    // BerandaFragment membuka scanner
                    mainActivity.findViewById<android.view.View>(R.id.fab_scan).performClick()
                }
                R.id.nav_informasi -> {
                    viewPager.currentItem = 1
                }
                R.id.nav_riwayat -> {
                    viewPager.currentItem = 2
                }
                R.id.nav_tentang -> {
                    viewPager.currentItem = 3
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BerandaFragment", "Navigasi gagal ke $destinationId", e)
        }
    }

    private fun tampilkanTanggal() {
        try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID"))
            binding.tvTanggal.text = format.format(Date()).uppercase()
        } catch (e: Exception) {
            binding.tvTanggal.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
