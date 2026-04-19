package com.modul.labuku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EntitasRiwayat::class], version = 2, exportSchema = false)
abstract class BasisDataAplikasi : RoomDatabase() {

    abstract fun daoRiwayat(): DaoRiwayat

    companion object {
        @Volatile
        private var INSTANCE: BasisDataAplikasi? = null

        fun dapatkanBasisData(konteks: Context): BasisDataAplikasi {
            return INSTANCE ?: synchronized(this) {
                val instansi = Room.databaseBuilder(
                    konteks.applicationContext,
                    BasisDataAplikasi::class.java,
                    "basis_data_labuku"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instansi
                instansi
            }
        }
    }
}
