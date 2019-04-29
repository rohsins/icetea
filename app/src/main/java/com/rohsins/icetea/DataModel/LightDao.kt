package com.rohsins.icetea.DataModel

import android.arch.persistence.room.*

@Dao
interface LightDao {
    @Query("SELECT * from Light")
    fun getAllLight(): List<Light>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLight(light: Light)

    @Update
    fun updateLight(light: Light)

    @Query("DELETE From Light WHERE id = :udi")
    fun deleteLight(udi: String)

    @Query("DELETE From Light")
    fun deleteAllLights()
}