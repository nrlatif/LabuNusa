package com.modul.LabuNusa.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabel_riwayat")
data class EntitasRiwayat(
    @PrimaryKey(autoGenerate = true)
    val idRiwayat: Int = 0,
    @ColumnInfo(name = "waktuPotret")
    val waktuScan: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "jalurGambarLokal")
    val lokasiGambar: String,
    val hasilKlasifikasi: String,
    val skorKepercayaan: Float,
    @ColumnInfo(name = "jalurGambarAnotasi", defaultValue = "")
    val lokasiGambarAnotasi: String? = null
)
