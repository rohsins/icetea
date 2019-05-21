package com.rohsins.icetea.DataModel

import android.arch.persistence.room.*

@Dao
interface DeviceDao {
    @Query("SELECT * from Device")
    fun getAllDevice(): List<Device>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDevice(device: Device)

    @Update
    fun updateDevice(device: Device)

    @Query("DELETE From Device WHERE udi = :udi")
    fun deleteDevice(udi: String)

    @Query("SELECT * From Device WHERE udi = :udi")
    fun getDevice(udi: String): Device

    @Query("SELECT alias From Device WHERE udi = :udi")
    fun getDeviceAlias(udi: String): String

    @Query("DELETE From Device")
    fun deleteAllDevices()
}