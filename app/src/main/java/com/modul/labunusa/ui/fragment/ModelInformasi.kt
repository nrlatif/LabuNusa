package com.modul.LabuNusa.ui.fragment

data class ModelInformasi(
    val judul: String,
    val subjudul: String,
    val warnaPrimaryRes: Int,
    val warnaLatarRes: Int,
    val warnaStrokeRes: Int,
    val seksiBagian: List<SeksiBagian>,
    var sedangDibuka: Boolean = false
)

data class SeksiBagian(
    val judul: String,
    val isi: String
)
