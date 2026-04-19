package com.modul.labuku

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.modul.labuku.databinding.ActivitySplashBinding

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

        // 1. Ikon daun muncul dengan efek "pop" dari bawah
        binding.ikonDaun.apply {
            translationY = 40f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(200)
                .setInterpolator(overshoot)
                .start()
        }

        // 2. Nama muncul
        binding.tvNama.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(700)
            .start()

        // 3. Tagline muncul
        binding.tvTagline.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(950)
            .start()

        // 4. Pindah ke MainActivity setelah 2.2 detik
        binding.root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200)
    }
}
