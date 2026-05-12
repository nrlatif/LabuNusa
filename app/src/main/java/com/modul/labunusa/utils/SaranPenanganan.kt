package com.modul.LabuNusa.utils

object SaranPenanganan {
    fun ambilSaran(label: String): String {
        return when (label.lowercase()) {
            "bercak daun" ->
                    "Buang dan bakar daun yang terinfeksi agar tidak menyebar. Semprotkan fungisida berbahan aktif tembaga atau mankozeb pada pagi atau sore hari. Jaga jarak tanam agar sirkulasi udara baik."
            "embun tepung" ->
                    "Semprotkan fungisida berbasis belerang (sulfur) atau fungisida nabati (seperti ekstrak mimba). Kurangi kelembapan di sekitar tanaman dengan pemangkasan daun."
            "layu fusarium" ->
                    "Cabut dan musnahkan tanaman yang sudah layu parah agar tidak menular. Hindari penyiraman berlebihan (tanah terlalu basah). Gunakan agensia hayati seperti Trichoderma sp."
            "daun sehat" ->
                    "Tanaman Anda tampak sehat. Lanjutkan perawatan rutin dengan penyiraman secukupnya dan pemupukan berimbang untuk menjaga kesehatan tanaman."
            "tidak teridentifikasi" ->
                    "Gambar tidak dikenali sebagai daun labu.\n\n• Arahkan kamera langsung ke permukaan daun\n• Pastikan daun mengisi sebagian besar bingkai\n• Hindari latar belakang yang terlalu ramai\n• Pastikan pencahayaan cukup dan tidak buram"
            else ->
                    "Lakukan pemeriksaan lebih lanjut pada tanaman Anda dan konsultasikan dengan penyuluh pertanian terdekat."
        }
    }
}
