package com.modul.LabuNusa.ml

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoundingBoxProcessorTest {

    @Test
    fun testBuatAnotasi_BitmapRecycled_ReturnsNull() {
        // Arrange: Skenario Bitmap yang sudah di-recycle
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle() // Simulate recycled state

        // Act: Panggil method buatAnotasi
        val result = BoundingBoxProcessor.buatAnotasi(bitmap, "Bercak Daun")

        // Assert: Harus mengembalikan null agar tidak crash (Null Safety)
        assertNull("Bitmap yang sudah di-recycle harus mengembalikan null", result)
    }

    @Test
    fun testBuatAnotasi_LabelTidakDikenal_ReturnsNull() {
        // Arrange: Skenario Label yang tidak diprogram dalam StrategiWarna
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Act: Panggil dengan label asing
        val result = BoundingBoxProcessor.buatAnotasi(bitmap, "Penyakit Asing XYZ")

        // Assert: Harus mengembalikan null karena strategi tidak ditemukan
        assertNull("Label tidak dikenal harus mengembalikan null", result)
    }

    @Test
    fun testBuatAnotasi_ParameterValid_BercakDaun() {
        // Arrange: Buat simulasi gambar daun (hijau) dengan area penyakit "Bercak Daun" (cokelat/merah gelap)
        val pW = 100
        val pH = 100
        val bitmap = Bitmap.createBitmap(pW, pH, Bitmap.Config.ARGB_8888)
        
        // Fill dengan warna daun sehat
        bitmap.eraseColor(Color.rgb(50, 200, 50))
        
        // Buat blob / bercak penyakit di tengah (warna cokelat gelap)
        // Menurut StrategiBercakDaun: (h <= 60f || h >= 300f) || (v < 0.35f) || (h in 20f..70f && s > 0.15f)
        val cPenyakit = Color.rgb(150, 50, 0)
        for (y in 40..60) {
            for (x in 40..60) {
                bitmap.setPixel(x, y, cPenyakit)
            }
        }

        // Act: Eksekusi fungsi anotasi
        val result = BoundingBoxProcessor.buatAnotasi(bitmap, "Bercak Daun")

        // Assert: Fungsi memproses blob dan mengembalikan Bitmap baru yang sudah ada gambaran bounding box
        assertNotNull("Jika parameter valid dan ada pola penyakit, fungsi harus mengembalikan Bitmap baru", result)
        // Pastikan bukan referensi yang sama (karena mengembalikan copy)
        assertNotSame("Harus mengembalikan instance Bitmap baru", bitmap, result)
        // Ukuran harus tetap sama dengan asli
        assertEquals("Lebar Bitmap hasil harus sama dengan asli", bitmap.width, result?.width)
        assertEquals("Tinggi Bitmap hasil harus sama dengan asli", bitmap.height, result?.height)
    }
}
