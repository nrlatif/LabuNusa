package com.modul.labuku.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.modul.labuku.ui.fragment.BerandaFragment
import com.modul.labuku.ui.fragment.InformasiFragment
import com.modul.labuku.ui.fragment.RiwayatFragment
import com.modul.labuku.ui.fragment.TentangFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BerandaFragment()
            1 -> InformasiFragment()
            2 -> RiwayatFragment()
            3 -> TentangFragment()
            else -> throw IllegalArgumentException("Posisi tidak valid")
        }
    }
}
