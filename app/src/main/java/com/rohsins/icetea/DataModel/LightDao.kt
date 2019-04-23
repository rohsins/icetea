package com.rohsins.icetea.DataModel

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface LightDao {
    @Query("SELECT * from Light")
    fun getLightById(): List<Light>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLight(light: Light)

    @Query("DELETE From Light")
    fun deleteAllLights()
}