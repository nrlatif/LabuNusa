package com.modul.LabuNusa

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.modul.LabuNusa.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private val infiniteAnimators = mutableListOf<ObjectAnimator>()

    private val taglineFull = "Klasifikasi Penyakit Daun Labu"
    private var typewriterIndex = 0
    private val typewriterRunnable =
            object : Runnable {
                override fun run() {
                    if (typewriterIndex <= taglineFull.length) {
                        binding.tvTagline.text = taglineFull.substring(0, typewriterIndex)
                        typewriterIndex++
                        handler.postDelayed(this, 45)
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = 0xFF1A4A2E.toInt()
        window.navigationBarColor = 0xFF1A4A2E.toInt()

        mulaiSplash()
    }

    private fun mulaiSplash() {
        animasiBackground()

        handler.postDelayed({ binding.particleView.mulai() }, 300)
        handler.postDelayed({ animasiRipple() }, 400)
        handler.postDelayed({ animasiLogo() }, 700)
        handler.postDelayed({ animasiNama() }, 1100)
        handler.postDelayed({ mulaiTypewriter() }, 1450)
        handler.postDelayed({ animasiDekorasi() }, 1600)
        handler.postDelayed({ animasiRipplePulse() }, 2000)
        handler.postDelayed({ animasiLoading() }, 1900)
        handler.postDelayed({ animasiKeluar() }, 3600)
    }

    private fun animasiBackground() {
        binding.gradientOverlay
                .animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(DecelerateInterpolator())
                .start()
        binding.particleView
                .animate()
                .alpha(1f)
                .setDuration(1200)
                .setStartDelay(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
    }

    private fun animasiRipple() {
        val decEase = DecelerateInterpolator(2.5f)

        binding.rippleRing1
                .animate()
                .alpha(0.7f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(decEase)
                .start()

        binding.rippleRing2
                .animate()
                .alpha(0.45f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(750)
                .setStartDelay(120)
                .setInterpolator(decEase)
                .start()

        binding.rippleRing3
                .animate()
                .alpha(0.25f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setStartDelay(240)
                .setInterpolator(decEase)
                .start()
    }

    private fun animasiLogo() {
        val overshoot = OvershootInterpolator(2f)

        binding.groupLogo
                .animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(650)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()

        binding.ikonDaun
                .animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(750)
                .setInterpolator(overshoot)
                .start()

        binding.ikonDaun.rotation = -15f
        binding.ikonDaun
                .animate()
                .rotation(0f)
                .setDuration(750)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .start()
    }

    private fun animasiNama() {
        binding.tvNama.translationY = 20f
        binding.tvNama
                .animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
    }

    private fun mulaiTypewriter() {
        typewriterIndex = 0
        binding.tvTagline.animate().alpha(1f).setDuration(300).start()
        handler.post(typewriterRunnable)
    }

    private fun animasiDekorasi() {
        binding.lineAccent
                .animate()
                .alpha(1f)
                .scaleX(1f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        binding.tvVersi.animate().alpha(1f).setDuration(500).setStartDelay(200).start()
    }

    private fun animasiRipplePulse() {
        val pulseRing1 =
                ObjectAnimator.ofFloat(binding.rippleRing1, "alpha", 0.7f, 0.3f, 0.7f).apply {
                    duration = 1200
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
        val pulseRing2 =
                ObjectAnimator.ofFloat(binding.rippleRing2, "alpha", 0.45f, 0.15f, 0.45f).apply {
                    duration = 1400
                    startDelay = 200
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
        val scaleUpX =
                ObjectAnimator.ofFloat(binding.ikonDaun, "scaleX", 1f, 1.04f, 1f).apply {
                    duration = 1800
                    startDelay = 100
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
        val scaleUpY =
                ObjectAnimator.ofFloat(binding.ikonDaun, "scaleY", 1f, 1.04f, 1f).apply {
                    duration = 1800
                    startDelay = 100
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
        infiniteAnimators.addAll(listOf(pulseRing1, pulseRing2, scaleUpX, scaleUpY))
        infiniteAnimators.forEach { it.start() }
    }

    private fun animasiLoading() {
        binding.loadingContainer
                .animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()

        val fase1 =
                ObjectAnimator.ofFloat(binding.loadingFill, "scaleX", 0f, 0.75f).apply {
                    duration = 900
                    startDelay = 200
                    interpolator = DecelerateInterpolator(2f)
                }

        val fase2 =
                ObjectAnimator.ofFloat(binding.loadingFill, "scaleX", 0.75f, 0.95f).apply {
                    duration = 800
                    interpolator = DecelerateInterpolator(3f)
                }

        val set = AnimatorSet()
        set.playSequentially(fase1, fase2)
        set.start()
    }

    private fun selesaikanLoading() {
        ObjectAnimator.ofFloat(binding.loadingFill, "scaleX", binding.loadingFill.scaleX, 1f)
                .apply {
                    duration = 220
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
    }

    private fun animasiKeluar() {
        handler.removeCallbacks(typewriterRunnable)

        infiniteAnimators.forEach { it.cancel() }
        infiniteAnimators.clear()

        selesaikanLoading()

        val options =
                ActivityOptions.makeCustomAnimation(
                        this,
                        R.anim.splash_to_main_enter,
                        R.anim.splash_to_main_exit
                )
        startActivity(Intent(this@SplashActivity, MainActivity::class.java), options.toBundle())
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        infiniteAnimators.forEach { it.cancel() }
        infiniteAnimators.clear()
        binding.particleView.hentikan()
    }
}
