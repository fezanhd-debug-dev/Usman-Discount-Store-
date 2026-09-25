package com.usmandiscountstore.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "Usman Discount Store",
    val storeAddress: String = "Vehari Road, Old Hasilpur",
    val latitude: Double = 29.6974,
    val longitude: Double = 72.5518,
    val geofenceRadiusMeters: Float = 30f,
    val adminWhatsapp: String = "",
    val alertEnabled: Boolean = true
)
