package com.rohsins.icetea.DataModel

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(Device::class), version = 2, exportSchema = false)
abstract class DeviceDatabase: RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    companion object {
        @Volatile private var INSTANCE: DeviceDatabase? = null

        fun getInstance(context: Context): DeviceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                DeviceDatabase::class.java, "Device.db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
    }
}