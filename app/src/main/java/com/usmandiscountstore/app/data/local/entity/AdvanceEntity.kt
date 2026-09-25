package com.usmandiscountstore.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advance")
data class AdvanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val amount: Double,
    val date: String,
    val reason: String = "",
    val mode: String = "CASH",
    val addedBy: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
