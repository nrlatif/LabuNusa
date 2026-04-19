package com.modul.labuku.utils

object HelperMitigasi {
    fun dapatkanMitigasi(label: String): String {
        return when (label.lowercase()) {
            "bercak daun" -> "Buang dan bakar daun yang terinfeksi agar tidak menyebar. Semprotkan fungisida berbahan aktif tembaga atau mankozeb pada pagi atau sore hari. Jaga jarak tanam agar sirkulasi udara baik."
            "embun tepung" -> "Semprotkan fungisida berbasis belerang (sulfur) atau fungisida nabati (seperti ekstrak mimba). Kurangi kelembapan di sekitar tanaman dengan pemangkasan daun."
            "layu fusarium" -> "Cabut dan musnahkan tanaman yang sudah layu parah agar tidak menular. Hindari penyiraman berlebihan (tanah terlalu basah). Gunakan agensia hayati seperti Trichoderma sp."
            "daun sehat" -> "Tanaman Anda tampak sehat. Lanjutkan perawatan rutin dengan penyiraman secukupnya dan pemupukan berimbang untuk menjaga kesehatan tanaman."
            "bukan daun" -> "Sistem tidak mendeteksi daun labu pada gambar ini. Pastikan Anda mengambil gambar daun dari jarak dekat dan fokus agar terbaca oleh sistem."
            else -> "Lakukan pemeriksaan lebih lanjut pada tanaman Anda dan konsultasikan dengan penyuluh pertanian terdekat."
        }
    }
}
