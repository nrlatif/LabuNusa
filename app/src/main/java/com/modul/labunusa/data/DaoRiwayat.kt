package com.modul.LabuNusa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoRiwayat {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpan(riwayat: EntitasRiwayat)

    @Query("SELECT * FROM tabel_riwayat ORDER BY waktuPotret DESC")
    fun ambilSemua(): Flow<List<EntitasRiwayat>>

    @androidx.room.Delete
    suspend fun hapus(riwayat: EntitasRiwayat)
}
