package com.usmandiscountstore.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["staffId", "date"], unique = true)]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val date: String,
    val status: String = "PRESENT",
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val selfiePath: String = "",
    val note: String = "",
    val markedBy: String = "",
    val markedAt: Long = System.currentTimeMillis()
)
