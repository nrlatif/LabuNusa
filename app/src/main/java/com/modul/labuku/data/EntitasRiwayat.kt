package com.modul.labuku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabel_riwayat")
data class EntitasRiwayat(
    @PrimaryKey(autoGenerate = true)
    val idRiwayat: Int = 0,
    val waktuPotret: Long = System.currentTimeMillis(),
    val jalurGambarLokal: String,
    val hasilKlasifikasi: String,
    val skorAkurasi: Float
)
