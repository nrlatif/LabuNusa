package com.modul.LabuNusa

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.modul.LabuNusa.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainkan()
    }

    private fun mainkan() {
        val overshoot = OvershootInterpolator(1.5f)

        // 1. Grup logo (ikon + nama + tagline) muncul dengan efek "pop" dari bawah
        binding.groupLogo.apply {
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(750)
                .setStartDelay(150)
                .setInterpolator(overshoot)
                .start()
        }

        // 2. Ikon scaling animasi sendiri di atas grup
        binding.ikonDaun.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setStartDelay(150)
            .setInterpolator(overshoot)
            .start()

        // 3. Teks versi muncul perlahan di bawah
        binding.tvVersi.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(900)
            .start()

        // 4. Pindah ke MainActivity setelah 2.4 detik
        binding.root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2400)
    }
}
