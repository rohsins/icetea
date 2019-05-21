package com.rohsins.icetea.DataModel

import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ColumnInfo

@Entity(tableName = "Light")
data class Light (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "udi")
    var udi: String,
    @ColumnInfo(name = "alias")
    var alias: String,
    @ColumnInfo(name = "isChecked")
    var isChecked: Boolean,
    @ColumnInfo(name = "intensity")
    var intensity: Int,
    @ColumnInfo(name = "color")
    var color: String
)