package com.usmandiscountstore.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val fromDate: String,       // yyyy-MM-dd
    val toDate: String,         // yyyy-MM-dd
    val reason: String = "",
    val status: String = "PENDING",   // PENDING / APPROVED / REJECTED
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedBy: String = "",
    val reviewedAt: Long = 0L,
    val adminNote: String = ""
)
