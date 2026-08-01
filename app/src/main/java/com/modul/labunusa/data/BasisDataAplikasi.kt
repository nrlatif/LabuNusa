package com.modul.LabuNusa.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EntitasRiwayat::class], version = 5, exportSchema = false)
abstract class BasisDataAplikasi : RoomDatabase() {

    abstract fun aksesRiwayat(): DaoRiwayat

    companion object {
        @Volatile
        private var INSTANCE: BasisDataAplikasi? = null

        fun bukaDatabase(konteks: Context): BasisDataAplikasi {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    konteks.applicationContext,
                    BasisDataAplikasi::class.java,
                    "basis_data_LabuNusa"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
