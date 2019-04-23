package com.rohsins.icetea.DataModel

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(Light::class), version = 1, exportSchema = false)
abstract class LightDatabase: RoomDatabase() {
    abstract fun lightDao(): LightDao
    companion object {
        @Volatile private var INSTANCE: LightDatabase? = null

        fun getInstance(context: Context): LightDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                LightDatabase::class.java, "Light.db")
                .allowMainThreadQueries()
                .build()
    }
}