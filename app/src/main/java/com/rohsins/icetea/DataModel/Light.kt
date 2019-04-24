package com.rohsins.icetea.DataModel

import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ColumnInfo

@Entity(tableName = "Light")
data class Light (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    var id: String,
    @ColumnInfo(name = "lightAlias")
    var alias: String,
    @ColumnInfo(name = "switchValue")
    var isChecked: Boolean,
    @ColumnInfo(name = "lightIntensity")
    var intensity: Int,
    @ColumnInfo(name = "lightColor")
    var color: String
)