package com.usmandiscountstore.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val designation: String = "",
    val role: String = "STAFF",
    val dailyWage: Double = 0.0,
    val joinDate: String = "",
    val photoPath: String = "",
    val isActive: Boolean = true
)
