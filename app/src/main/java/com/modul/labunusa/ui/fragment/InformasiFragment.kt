package com.modul.LabuNusa.ui.fragment

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.modul.LabuNusa.R
import com.modul.LabuNusa.databinding.FragmentInformasiBinding

class InformasiFragment : Fragment() {

    private var _binding: FragmentInformasiBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInformasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? com.modul.LabuNusa.MainActivity)?.daftarkanHeader(
                2,
                binding.headerInformasi
        )

        val daftarInformasi = buatDaftarInformasi()
        val adapter = InformasiAdapter(daftarInformasi)

        binding.rvInformasi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInformasi.adapter = adapter

        // Custom outline provider: rounded rect hanya di sudut atas.
        // Outline diperluas ke bawah (+ cornerRadius) agar sudut bawah
        // tidak terpotong, sehingga hanya sudut atas yang meng-clip konten.
        val cornerRadiusPx = resources.getDimension(R.dimen.radius_konten_rounded)
        binding.contentContainer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0, 0,
                    view.width,
                    view.height + cornerRadiusPx.toInt(),
                    cornerRadiusPx
                )
            }
        }
        binding.contentContainer.clipToOutline = true
    }

    private fun buatDaftarInformasi(): List<ModelInformasi> =
            listOf(
                    ModelInformasi(
                            judul = "Daun Sehat",
                            subjudul = "Kondisi daun normal",
                            warnaPrimaryRes = R.color.hijau_primer,
                            warnaLatarRes = R.color.hijau_kartu,
                            warnaStrokeRes = R.color.hijau_terang,
                            seksiBagian =
                                    listOf(
                                            SeksiBagian(
                                                    judul = "Ciri-Ciri Daun Sehat",
                                                    isi =
                                                            "• Permukaan daun berwarna hijau merata\n" +
                                                                    "• Tidak ada bercak atau perubahan warna\n" +
                                                                    "• Tekstur daun kuat dan tidak layu\n" +
                                                                    "• Pertumbuhan normal dan segar"
                                            ),
                                            SeksiBagian(
                                                    judul = "Cara Perawatan",
                                                    isi =
                                                            "• Siram secara teratur di pagi hari\n" +
                                                                    "• Berikan pupuk sesuai kebutuhan\n" +
                                                                    "• Jaga kebersihan sekitar tanaman\n" +
                                                                    "• Pantau secara rutin setiap minggu"
                                            )
                                    )
                    ),
                    ModelInformasi(
                            judul = "Embun Tepung",
                            subjudul = "Powdery Mildew",
                            warnaPrimaryRes = R.color.merah_penyakit,
                            warnaLatarRes = R.color.merah_latar,
                            warnaStrokeRes = R.color.merah_penyakit,
                            seksiBagian =
                                    listOf(
                                            SeksiBagian(
                                                    judul = "Gejala",
                                                    isi =
                                                            "• Muncul serbuk putih seperti tepung di permukaan daun\n" +
                                                                    "• Daun menguning dan mengering dari tepi\n" +
                                                                    "• Daun menggulung ke atas\n" +
                                                                    "• Pertumbuhan tanaman terhambat"
                                            ),
                                            SeksiBagian(
                                                    judul = "Cara Penanganan & Mitigasi",
                                                    isi =
                                                            "• Semprot fungisida berbahan Sulfur atau Tembaga\n" +
                                                                    "• Pangkas daun yang sudah terinfeksi berat\n" +
                                                                    "• Pastikan sirkulasi udara cukup di antara tanaman\n" +
                                                                    "• Hindari menyiram di malam hari\n" +
                                                                    "• Kurangi kelembaban berlebihan di sekitar tanaman\n" +
                                                                    "• Rotasi tanaman pada musim berikutnya"
                                            ),
                                            SeksiBagian(
                                                    judul = "Pencegahan",
                                                    isi =
                                                            "• Pilih varietas tahan penyakit\n" +
                                                                    "• Jaga jarak tanam agar tidak terlalu rapat\n" +
                                                                    "• Semprotkan larutan baking soda (1 sdm/liter air) secara preventif"
                                            )
                                    )
                    ),
                    ModelInformasi(
                            judul = "Bercak Daun",
                            subjudul = "Leaf Spot Disease",
                            warnaPrimaryRes = R.color.kuning_penyakit,
                            warnaLatarRes = R.color.kuning_latar,
                            warnaStrokeRes = R.color.kuning_penyakit,
                            seksiBagian =
                                    listOf(
                                            SeksiBagian(
                                                    judul = "Gejala",
                                                    isi =
                                                            "• Muncul bercak coklat kekuningan pada daun\n" +
                                                                    "• Bercak dikelilingi pinggiran berwarna kuning\n" +
                                                                    "• Bercak melebar dan menyatu pada infeksi berat\n" +
                                                                    "• Daun menguning lalu rontok"
                                            ),
                                            SeksiBagian(
                                                    judul = "Cara Penanganan & Mitigasi",
                                                    isi =
                                                            "• Semprot fungisida berbahan Mankozeb atau Klorotalonil\n" +
                                                                    "• Buang dan musnahkan daun yang terinfeksi (jangan dikompos)\n" +
                                                                    "• Siram di pangkal tanaman, hindari membasahi daun\n" +
                                                                    "• Jaga kebersihan alat berkebun\n" +
                                                                    "• Kurangi kepadatan tanaman"
                                            ),
                                            SeksiBagian(
                                                    judul = "Pencegahan",
                                                    isi =
                                                            "• Gunakan benih bersertifikat bebas penyakit\n" +
                                                                    "• Semprot fungisida preventif saat musim hujan\n" +
                                                                    "• Hindari melukai tanaman saat pemeliharaan"
                                            )
                                    )
                    ),
                    ModelInformasi(
                            judul = "Layu Fusarium",
                            subjudul = "Fusarium Wilt",
                            warnaPrimaryRes = R.color.ungu_penyakit,
                            warnaLatarRes = R.color.ungu_latar,
                            warnaStrokeRes = R.color.ungu_penyakit,
                            seksiBagian =
                                    listOf(
                                            SeksiBagian(
                                                    judul = "Gejala",
                                                    isi =
                                                            "• Daun dan pucuk layu mendadak di siang hari\n" +
                                                                    "• Menguning dari daun bawah ke atas\n" +
                                                                    "• Batang bagian dalam berwarna coklat jika dipotong\n" +
                                                                    "• Tanaman mati secara bertahap"
                                            ),
                                            SeksiBagian(
                                                    judul = "Cara Penanganan & Mitigasi",
                                                    isi =
                                                            "• Cabut dan bakar tanaman yang terinfeksi segera\n" +
                                                                    "• Sterilkan tanah dengan kapur dolomit\n" +
                                                                    "• Aplikasikan fungisida sistemik berbahan Benomil\n" +
                                                                    "• Gunakan agen hayati Trichoderma sp. pada tanah\n" +
                                                                    "• JANGAN menanam labu di lokasi yang sama minimal 3 musim"
                                            ),
                                            SeksiBagian(
                                                    judul = "Pencegahan",
                                                    isi =
                                                            "• Gunakan benih yang sudah diperlakukan fungisida\n" +
                                                                    "• Perbaiki drainase tanah agar tidak tergenang\n" +
                                                                    "• Rendam benih dengan larutan Trichoderma sebelum tanam\n" +
                                                                    "• Rotasi tanaman wajib dilakukan"
                                            )
                                    )
                    )
            )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
