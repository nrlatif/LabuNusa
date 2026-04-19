package com.modul.labuku

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.modul.labuku.databinding.ActivityMainBinding
import com.modul.labuku.ui.adapter.MainPagerAdapter
import com.modul.labuku.ui.fragment.ScanFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set adapter ke ViewPager2
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Animasi kemunculan Navbar pertama kali aplikasi dibuka
        binding.root.postDelayed({
            showNavContainer()
        }, 150)
        
        // Disable bottom nav placeholder click (tombol kosong di tengah untuk kamera)
        binding.bottomNavView.menu.findItem(R.id.nav_scan_placeholder)?.isEnabled = false

        // Sinkronisasi ViewPager usapan layar bergeser -> merubah status Bottom Nav
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menu = binding.bottomNavView.menu
                when (position) {
                    0 -> menu.findItem(R.id.nav_beranda).isChecked = true
                    1 -> menu.findItem(R.id.nav_informasi).isChecked = true
                    2 -> menu.findItem(R.id.nav_riwayat).isChecked = true
                    3 -> menu.findItem(R.id.nav_tentang).isChecked = true
                }
            }
        })

        // Sinkronisasi klik Bottom Nav -> menggeser ViewPager
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_informasi -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_riwayat -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                R.id.nav_tentang -> {
                    binding.viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }

        // Action FAB Camera
        binding.fabScan.setOnClickListener {
            // Animasi Bounce untuk tombol itu sendiri
            val bounce = AnimationUtils.loadAnimation(this, R.anim.fab_bounce)
            binding.fabScan.startAnimation(bounce)

            // Membuka ScanFragment di layar atas (overlay) 
            bukaScannerKhusus()
        }
    }

    private fun bukaScannerKhusus() {
        // Hilangkan bottom navbar dengan animasi slide down agar kamera fullscreen
        hideNavContainer()
        
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.slide_up_kamera, R.anim.slide_down_kamera, R.anim.slide_up_kamera, R.anim.slide_down_kamera)
        transaction.replace(R.id.scan_container, ScanFragment())
        transaction.addToBackStack("SCAN_FRAG")
        transaction.commit()
    }
    
    // Dipanggil dari dalam ScanFragment menggunakan (requireActivity() as MainActivity).tutupScanner()
    fun tutupScanner() {
        showNavContainer()
        supportFragmentManager.popBackStack()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            // Jika kita berada di halaman kamera, kembali akan menutup kamera dan memunculkan navbar kembali
            showNavContainer()
            supportFragmentManager.popBackStack()
        } else if (binding.viewPager.currentItem != 0) {
            // Jika di-back dari halaman riwayat dll, balik ke beranda dulu
            binding.viewPager.currentItem = 0
        } else {
            super.onBackPressed()
        }
    }

    private fun hideNavContainer() {
        if (binding.navContainer.visibility == View.VISIBLE) {
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_hide)
            binding.navContainer.startAnimation(slideDown)
            binding.navContainer.visibility = View.GONE
        }
    }

    private fun showNavContainer() {
        if (binding.navContainer.visibility == View.GONE) {
            binding.navContainer.visibility = View.VISIBLE
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_show)
            binding.navContainer.startAnimation(slideUp)
        }
    }
}