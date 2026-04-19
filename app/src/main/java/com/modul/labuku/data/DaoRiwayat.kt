package com.modul.labuku.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoRiwayat {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sisipkanRiwayat(riwayat: EntitasRiwayat)

    @Query("SELECT * FROM tabel_riwayat ORDER BY waktuPotret DESC")
    fun ambilSemuaRiwayat(): Flow<List<EntitasRiwayat>>

    @androidx.room.Delete
    suspend fun hapusRiwayat(riwayat: EntitasRiwayat)
}
