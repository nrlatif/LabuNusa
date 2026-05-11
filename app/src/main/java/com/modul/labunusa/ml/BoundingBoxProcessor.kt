package com.modul.LabuNusa.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object BoundingBoxProcessor {

    private const val PROC_MAX = 320

    fun buatAnotasi(bitmap: Bitmap, label: String): Bitmap? {
        if (bitmap.isRecycled) return null
        val strategi = tentukanStrategi(label) ?: return null
        return proses(bitmap, strategi)
    }

    private fun proses(bitmap: Bitmap, strategi: StrategiWarna): Bitmap? {
        val origW = bitmap.width
        val origH = bitmap.height

        val scale = minOf(1f, PROC_MAX.toFloat() / maxOf(origW, origH))
        val pW = (origW * scale).toInt().coerceAtLeast(1)
        val pH = (origH * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, pW, pH, true)
        val pixels = IntArray(pW * pH)
        scaled.getPixels(pixels, 0, pW, 0, 0, pW, pH)
        scaled.recycle()

        val hsv = FloatArray(3)
        val isDaun     = BooleanArray(pW * pH)
        val isPenyakit = BooleanArray(pW * pH)

        for (i in pixels.indices) {
            val px = pixels[i]
            Color.RGBToHSV(Color.red(px), Color.green(px), Color.blue(px), hsv)
            val h = hsv[0]; val s = hsv[1]; val v = hsv[2]

            val isBackground = (v > 0.93f && s < 0.07f) || (v < 0.07f)
            if (!isBackground) isDaun[i] = true

            val isHijauSehat = h in 75f..160f && s > 0.22f && v > 0.15f
            if (!isBackground && !isHijauSehat) {
                isPenyakit[i] = strategi.validasiPiksel(h, s, v)
            }
        }

        var leafL = pW; var leafR = 0; var leafT = pH; var leafB = 0
        for (y in 0 until pH) for (x in 0 until pW) {
            if (isDaun[y * pW + x]) {
                if (x < leafL) leafL = x; if (x > leafR) leafR = x
                if (y < leafT) leafT = y; if (y > leafB) leafB = y
            }
        }
        if (leafR - leafL < pW * 0.05f || leafB - leafT < pH * 0.05f) {
            leafL = 0; leafR = pW; leafT = 0; leafB = pH
        }

        val visited = BooleanArray(pW * pH)
        val blobs = mutableListOf<IntArray>()
        for (y in leafT..leafB) {
            for (x in leafL..leafR) {
                val idx = y * pW + x
                if (isPenyakit[idx] && !visited[idx]) {
                    val blob = bfs(isPenyakit, visited, x, y, pW, pH)
                    if (blob[4] > 1) blobs.add(blob)
                }
            }
        }
        if (blobs.isEmpty()) return null

        val minBlobAbs = maxOf(4f, (pW * pH) * 0.0005f).toInt()
        val signifikan = blobs.filter { it[4] >= minBlobAbs }.sortedByDescending { it[4] }
        val isScattered = blobs.size >= 6 || (signifikan.isEmpty() && blobs.size >= 2)

        val finalBlobs: List<IntArray> = when {
            isScattered || strategi.drawUnion -> {
                val topBlobs = (if (signifikan.isNotEmpty()) signifikan else blobs).take(30)
                listOf(intArrayOf(
                    topBlobs.minOf { it[0] }, topBlobs.minOf { it[1] },
                    topBlobs.maxOf { it[2] }, topBlobs.maxOf { it[3] },
                    topBlobs.sumOf { it[4] }
                ))
            }
            signifikan.isNotEmpty() -> signifikan.take(3)
            else -> listOf(blobs.maxByOrNull { it[4] } ?: return null)
        }

        return gambarAnotasi(bitmap, finalBlobs, scale, origW, origH, strategi)
    }

    private val DX = intArrayOf(-1, 1, 0, 0, -1, 1, -1, 1)
    private val DY = intArrayOf(0, 0, -1, 1, -1, -1, 1, 1)

    private fun bfs(
        mask: BooleanArray, visited: BooleanArray,
        startX: Int, startY: Int, w: Int, h: Int
    ): IntArray {
        var minX = startX; var maxX = startX
        var minY = startY; var maxY = startY
        var count = 0
        val queue = ArrayDeque<Int>()
        val s = startY * w + startX
        queue.add(s); visited[s] = true
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val x = idx % w; val y = idx / w
            count++
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            for (d in 0..7) {
                val nx = x + DX[d]; val ny = y + DY[d]
                if (nx in 0 until w && ny in 0 until h) {
                    val ni = ny * w + nx
                    if (mask[ni] && !visited[ni]) { visited[ni] = true; queue.add(ni) }
                }
            }
        }
        return intArrayOf(minX, minY, maxX, maxY, count)
    }

    private fun gambarAnotasi(
        original: Bitmap, blobs: List<IntArray>,
        scale: Float, origW: Int, origH: Int, strategi: StrategiWarna
    ): Bitmap? {
        if (original.isRecycled) return null
        val hasil = original.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        val canvas = Canvas(hasil)

        val strokeW = (minOf(origW, origH) * 0.018f).coerceIn(4f, 14f)
        val pad = strokeW.toInt()
        val r = Color.red(strategi.warnaBox)
        val g = Color.green(strategi.warnaBox)
        val b = Color.blue(strategi.warnaBox)

        val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = strategi.warnaBox; strokeWidth = strokeW
        }
        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.argb(45, r, g, b)
        }
        val paintLabelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.argb(210, r, g, b)
        }
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (minOf(origW, origH) * 0.052f).coerceIn(18f, 46f)
            isFakeBoldText = true
        }

        for (blob in blobs) {
            val left   = (blob[0] / scale - pad).coerceAtLeast(0f)
            val top    = (blob[1] / scale - pad).coerceAtLeast(0f)
            val right  = (blob[2] / scale + pad).coerceAtMost(origW.toFloat())
            val bottom = (blob[3] / scale + pad).coerceAtMost(origH.toFloat())
            val rect = RectF(left, top, right, bottom)

            canvas.drawRect(rect, paintFill)
            canvas.drawRect(rect, paintBorder)

            val teks = strategi.labelSingkat
            val txtW = paintLabel.measureText(teks)
            val txtH = paintLabel.textSize
            val bgRect = RectF(
                rect.left + strokeW, rect.top + strokeW,
                rect.left + strokeW + txtW + 16f, rect.top + strokeW + txtH + 8f
            )
            canvas.drawRoundRect(bgRect, 6f, 6f, paintLabelBg)
            canvas.drawText(teks, bgRect.left + 8f, bgRect.bottom - 6f, paintLabel)
        }
        return hasil
    }

    private fun tentukanStrategi(label: String): StrategiWarna? = when {
        label.contains("Embun Tepung", ignoreCase = true) -> StrategiEmbunTepung
        label.contains("Bercak",       ignoreCase = true) -> StrategiBercakDaun
        label.contains("Layu",         ignoreCase = true) -> StrategiLayuFusarium
        else -> null
    }

    private interface StrategiWarna {
        val warnaBox: Int
        val labelSingkat: String
        val drawUnion: Boolean get() = false
        fun validasiPiksel(h: Float, s: Float, v: Float): Boolean
    }

    private object StrategiEmbunTepung : StrategiWarna {
        override val warnaBox     = Color.rgb(210, 210, 210)
        override val labelSingkat = "Embun Tepung"
        override fun validasiPiksel(h: Float, s: Float, v: Float) =
            (v > 0.55f) || (s < 0.35f && v > 0.30f)
    }

    private object StrategiBercakDaun : StrategiWarna {
        override val warnaBox     = Color.rgb(200, 70, 20)
        override val labelSingkat = "Bercak Daun"
        override val drawUnion    = true
        override fun validasiPiksel(h: Float, s: Float, v: Float) =
            (h <= 60f || h >= 300f) || (v < 0.35f) || (h in 20f..70f && s > 0.15f)
    }

    private object StrategiLayuFusarium : StrategiWarna {
        override val warnaBox     = Color.rgb(220, 150, 10)
        override val labelSingkat = "Layu Fusarium"
        override fun validasiPiksel(h: Float, s: Float, v: Float) =
            (h in 30f..80f) || (s < 0.40f && v > 0.35f) || (h in 160f..300f && v > 0.40f)
    }
}
