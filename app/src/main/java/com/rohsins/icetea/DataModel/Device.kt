package com.rohsins.icetea.DataModel

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "Device")
data class Device (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int,
    @ColumnInfo(name = "udi")
    var udi: String,
    @ColumnInfo(name = "alias")
    var alias: String,
    @ColumnInfo(name = "type")
    var type: String
)