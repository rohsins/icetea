package com.rohsins.icetea.DataModel

import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ColumnInfo

@Entity(tableName = "Light")
data class Light (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    var uid: Int,
    @ColumnInfo(name = "lightAlias")
    var lightAlias: String,
    @ColumnInfo(name = "switchValue")
    var switchValue: Int,
    @ColumnInfo(name = "lightIntensity")
    var lightIntensity: Int
)