package com.modul.LabuNusa.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class SplashParticleView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Partikel>()
    private val paintDaun = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animRunning = true
    private var lastTime = System.currentTimeMillis()
    private val warnaPartikel =
            intArrayOf(
                    Color.parseColor("#6DBF47"),
                    Color.parseColor("#52A832"),
                    Color.parseColor("#81C99A"),
                    Color.parseColor("#4CAF7D"),
                    Color.parseColor("#A8D8A8"),
                    Color.parseColor("#3D9E6E")
            )

    data class Partikel(
            var x: Float,
            var y: Float,
            var ukuran: Float,
            var kecepatan: Float,
            var rotasi: Float,
            var kecepatanRotasi: Float,
            var alpha: Float,
            var warna: Int,
            var offsetSinus: Float,
            var faseSinus: Float,
            var amplitudoSinus: Float
    )
    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buatPartikel(w, h)
    }

    private fun buatPartikel(w: Int, h: Int) {
        particles.clear()
        val jumlah = 22
        repeat(jumlah) {
            val uk = Random.nextFloat() * 18f + 8f
            particles.add(
                    Partikel(
                            x = Random.nextFloat() * w,
                            y = Random.nextFloat() * h + h * 0.2f,
                            ukuran = uk,
                            kecepatan = Random.nextFloat() * 0.6f + 0.3f,
                            rotasi = Random.nextFloat() * 360f,
                            kecepatanRotasi = (Random.nextFloat() - 0.5f) * 1.2f,
                            alpha = Random.nextFloat() * 0.5f + 0.1f,
                            warna = warnaPartikel[Random.nextInt(warnaPartikel.size)],
                            offsetSinus = Random.nextFloat() * Math.PI.toFloat() * 2f,
                            faseSinus = 0f,
                            amplitudoSinus = Random.nextFloat() * 1.5f + 0.5f
                    )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val now = System.currentTimeMillis()
        val delta = (now - lastTime).coerceAtMost(33).toFloat()
        lastTime = now

        for (p in particles) {
            p.faseSinus += delta * 0.002f
            p.x += sin(p.faseSinus + p.offsetSinus) * p.amplitudoSinus
            p.y -= p.kecepatan * delta * 0.5f
            p.rotasi += p.kecepatanRotasi * delta * 0.05f
            if (p.y < -p.ukuran * 2) {
                p.y = height.toFloat() + p.ukuran
                p.x = Random.nextFloat() * width
                p.alpha = Random.nextFloat() * 0.4f + 0.1f
            }
            gambarDaun(canvas, p)
        }

        if (animRunning) postInvalidateOnAnimation()
    }

    private fun gambarDaun(canvas: Canvas, p: Partikel) {
        canvas.save()
        canvas.translate(p.x, p.y)
        canvas.rotate(p.rotasi)

        paintDaun.color = p.warna
        paintDaun.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
        paintDaun.style = Paint.Style.FILL

        val path = Path()
        val s = p.ukuran

        path.moveTo(0f, -s)
        path.cubicTo(s * 0.8f, -s * 0.5f, s * 0.8f, s * 0.5f, 0f, s)
        path.cubicTo(-s * 0.8f, s * 0.5f, -s * 0.8f, -s * 0.5f, 0f, -s)
        path.close()
        canvas.drawPath(path, paintDaun)

        paintDaun.color = Color.parseColor("#AAFFFFFF")
        paintDaun.alpha = (p.alpha * 180).toInt().coerceIn(0, 255)
        paintDaun.style = Paint.Style.STROKE
        paintDaun.strokeWidth = s * 0.08f
        val pathUrat = Path()
        pathUrat.moveTo(0f, -s * 0.8f)
        pathUrat.lineTo(0f, s * 0.8f)
        canvas.drawPath(pathUrat, paintDaun)

        canvas.restore()
    }

    fun mulai() {
        animRunning = true
        lastTime = System.currentTimeMillis()
        invalidate()
    }

    fun hentikan() {
        animRunning = false
    }
}
