package com.usmandiscountstore.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonus")
data class BonusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val monthYear: String,       // e.g. "2026-09"
    val amount: Double,
    val reason: String = "Overtime",
    val addedBy: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
