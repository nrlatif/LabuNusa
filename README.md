# 🎃 LabuNusa

[![Android Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![TensorFlow Lite](https://img.shields.io/badge/ML-TensorFlow%20Lite-orange.svg?style=flat-square&logo=tensorflow)](https://www.tensorflow.org/lite)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-blue.svg?style=flat-square)](https://developer.android.com/about/dashboards)

Aplikasi Android berbasis Machine Learning untuk melakukan klasifikasi dan deteksi dini penyakit pada daun tanaman labu. Aplikasi ini membantu petani atau pehobi tanaman untuk mengidentifikasi kesehatan daun labu secara cepat dan akurat menggunakan kamera *smartphone*.

---

## 📌 Ringkasan Project

**LabuNusa** adalah aplikasi mobile Android yang memanfaatkan model Machine Learning **MobileNetV2** yang dikonversi ke format **TensorFlow Lite (TFLite)** (`mobilenetv2_labuV2_float32_final.tflite`). Aplikasi ini dirancang untuk mendeteksi penyakit daun tanaman labu secara lokal (*on-device inference*) tanpa memerlukan koneksi internet aktif.

### Fitur Utama:
* **Deteksi Real-Time**: Menggunakan kamera perangkat (ditenagai oleh Android CameraX) untuk menangkap gambar daun dan mendeteksi kondisi kesehatan daun secara langsung.
* **Klasifikasi Penyakit**: Mampu mendeteksi 4 kondisi utama pada daun labu:
  * **Bercak Daun** (Leaf Spot)
  * **Daun Sehat** (Healthy Leaf)
  * **Embun Tepung** (Powdery Mildew)
  * **Layu Fusarium** (Fusarium Wilt)
* **Pre-Screening Cerdas**: Dilengkapi dengan validasi gambar sebelum klasifikasi dilakukan (untuk mencegah deteksi palsu pada objek selain daun):
  * **Rasio Hijau Dominan**: Memastikan objek yang difoto memiliki warna hijau daun yang cukup.
  * **Analisis Tekstur**: Memeriksa variansi tekstur objek untuk memastikan itu adalah daun sungguhan.
  * **Validasi Entropi & Skor**: Mencegah klasifikasi dengan tingkat kepercayaan rendah atau ambigu (*Tidak Teridentifikasi*).
* **Penyimpanan Riwayat**: Didukung oleh database lokal **Room** untuk menyimpan riwayat hasil klasifikasi yang telah dilakukan pengguna.

---

## 📥 Cara Unduh

Anda dapat mengunduh dan memasang aplikasi ini melalui beberapa cara berikut:

### 1. Unduh APK Langsung (Untuk Pengguna Umum)
Cara termudah untuk menggunakan aplikasi di HP Android Anda adalah dengan mengunduh berkas `.apk` siap pakai:

👉 [**Unduh LabuNusa APK Versi Terbaru**](https://github.com/nrlatif/Skripshit/releases/latest/download/app-release.apk)

> 💡 *Catatan: Jika Anda mengunduh APK di luar Google Play Store, pastikan Anda telah mengaktifkan opsi "Izinkan instalasi dari sumber tidak dikenal" (Allow installation from unknown sources) di pengaturan keamanan Android Anda.*

### 2. Unduh Source Code (.ZIP)
Bagi Anda yang ingin melihat berkas kode sumber tanpa menggunakan Git:

👉 [**Unduh Source Code (ZIP)**](https://github.com/nrlatif/Skripshit/archive/refs/heads/main.zip)

### 3. Menggunakan Git Clone (Untuk Developer)
Untuk meng-clone repositori ini ke komputer lokal Anda guna pengembangan lebih lanjut:

```bash
# Clone menggunakan remote origin utama
git clone https://github.com/nrlatif/LabuNusa.git
```

---

## ⚙️ Spesifikasi Minimum

Untuk menjalankan aplikasi ini dengan lancar, berikut adalah spesifikasi perangkat dan lingkungan pengembangan yang dibutuhkan:

### 📱 Spesifikasi Perangkat Pengguna (Android)
* **Sistem Operasi**: Android 8.0 Oreo (API Level 26) ke atas (Direkomendasikan Android 10+).
* **Memori (RAM)**: Minimal **2 GB** (Direkomendasikan **4 GB** agar proses komputasi Machine Learning/Inference berjalan mulus).
* **Kamera**: Kamera belakang yang berfungsi dengan baik (sangat disarankan memiliki fitur *autofocus*).
* **Penyimpanan Kosong**: Minimal **50 MB** ruang penyimpanan bebas.
* **Perizinan Aplikasi**: Izin akses Kamera (*Camera Permission*) diperlukan untuk proses klasifikasi langsung.

### 💻 Spesifikasi Lingkungan Pengembangan (Developer)
Jika Anda ingin membuka, memodifikasi, dan melakukan *build* ulang proyek ini:
* **IDE**: Android Studio Iguana (2023.2.1) atau versi yang lebih baru.
* **Java Development Kit (JDK)**: JDK 11 (Source & Target compatibility menggunakan Java 11).
* **Android SDK**: SDK Platform 35 (Android 15).
* **Build System**: Gradle dengan Kotlin DSL (`build.gradle.kts`).
* **Dependency Utama**:
  * TensorFlow Lite (`org.tensorflow:tensorflow-lite`)
  * Android Jetpack CameraX (`androidx.camera`)
  * Room Database (`androidx.room`)
  * Jetpack Navigation & ViewBinding
