package com.modul.LabuNusa

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.modul.LabuNusa.databinding.ActivityMainBinding
import com.modul.LabuNusa.ui.adapter.MainPagerAdapter
import com.modul.LabuNusa.ui.fragment.IdentifikasiFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val warnaTerang = 0xFF52B885.toInt()
    private val warnaGelap = 0xFF1C5A3B.toInt()
    private val headerViews = arrayOfNulls<View>(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force Edge-to-Edge: Gambar activity menembus ke bawah status bar
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hindari Bottom Navigation tertutup oleh tombol navigasi HP bawaan
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Hanya pad bagian bawah (navigation bar), bagian atas (status bar) biarkan 0 agar gradient merambat ke atas!
            view.setPadding(0, 0, 0, insets.bottom)
            // KONSUMSI insets agar BottomNavigationView di dalamnya tidak ikut-ikutan menambah padding secara otomatis!
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        binding.root.postDelayed({ binding.viewPager.offscreenPageLimit = 3 }, 800)

        binding.root.post { showNavContainer() }
        binding.bottomNavView.menu.findItem(R.id.nav_scan_placeholder)?.isEnabled = false

        binding.viewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        val menu = binding.bottomNavView.menu
                        when (position) {
                            0 -> menu.findItem(R.id.nav_beranda).isChecked = true
                            1 -> menu.findItem(R.id.nav_riwayat).isChecked = true
                            2 -> menu.findItem(R.id.nav_informasi).isChecked = true
                            3 -> menu.findItem(R.id.nav_tentang).isChecked = true
                        }
                        // Status bar dibiarkan transparan (diatur di themes.xml) agar gradient header terlihat menyatu.
                    }
                }
        )

        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_riwayat -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_informasi -> {
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

        binding.fabScan.setOnClickListener {
            val bounce = AnimationUtils.loadAnimation(this, R.anim.fab_bounce)
            binding.fabScan.startAnimation(bounce)
            animasiFabTekan()
            bukaScannerKhusus()
        }
    }

    fun daftarkanHeader(position: Int, headerView: View) {

        val orientation =
                if (position % 2 == 0) GradientDrawable.Orientation.LEFT_RIGHT
                else GradientDrawable.Orientation.RIGHT_LEFT

        val gd = GradientDrawable(orientation, intArrayOf(warnaTerang, warnaGelap))
        headerView.background = gd
        headerViews[position] = headerView
    }

    private fun animasiFabTekan() {
        val sx1 =
                ObjectAnimator.ofFloat(binding.fabScan, "scaleX", 1f, 0.87f).apply { duration = 70 }
        val sy1 =
                ObjectAnimator.ofFloat(binding.fabScan, "scaleY", 1f, 0.87f).apply { duration = 70 }
        val sx2 =
                ObjectAnimator.ofFloat(binding.fabScan, "scaleX", 0.87f, 1.05f, 1f).apply {
                    duration = 180
                    startDelay = 70
                }
        val sy2 =
                ObjectAnimator.ofFloat(binding.fabScan, "scaleY", 0.87f, 1.05f, 1f).apply {
                    duration = 180
                    startDelay = 70
                }
        AnimatorSet().apply {
            playTogether(sx1, sy1, sx2, sy2)
            start()
        }
    }

    private fun bukaScannerKhusus() {
        hideNavContainer()
        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_up_kamera,
                        R.anim.slide_down_kamera,
                        R.anim.slide_up_kamera,
                        R.anim.slide_down_kamera
                )
                .replace(R.id.scan_container, IdentifikasiFragment())
                .addToBackStack("SCAN_FRAG")
                .commit()
    }

    fun tutupScanner() {
        showNavContainer()
        supportFragmentManager.popBackStack()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            showNavContainer()
            supportFragmentManager.popBackStack()
        } else if (binding.viewPager.currentItem != 0) {
            binding.viewPager.currentItem = 0
        } else {
            super.onBackPressed()
        }
    }

    private fun hideNavContainer() {
        if (binding.navContainer.visibility == View.VISIBLE) {
            binding.navContainer.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_down_hide)
            )
            binding.navContainer.visibility = View.GONE
        }
    }

    private fun showNavContainer() {
        if (binding.navContainer.visibility == View.GONE) {
            binding.navContainer.visibility = View.VISIBLE
            binding.navContainer.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_up_show)
            )
        }
    }
}
