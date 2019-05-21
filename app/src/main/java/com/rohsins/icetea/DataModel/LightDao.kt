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

    @Query("UPDATE Light SET intensity = :intensity WHERE udi =:udi")
    fun updateLightIntensity(udi: String, intensity: Int)

    @Query("UPDATE Light SET color = :color WHERE udi =:udi")
    fun updateLightColor(udi: String, color: String)

    @Query("UPDATE Light SET isChecked = :isChecked WHERE udi =:udi")
    fun updateLightIsChecked(udi: String, isChecked: Boolean)

    @Query("UPDATE Light SET isChecked = :isChecked, intensity = :intensity, color = :color WHERE udi =:udi")
    fun updateLightIsCheckedIntensityColor(udi: String, isChecked: Boolean, intensity: Int, color: String)

    @Query("DELETE From Light WHERE udi = :udi")
    fun deleteLight(udi: String)

    @Query("DELETE From Light")
    fun deleteAllLights()
}