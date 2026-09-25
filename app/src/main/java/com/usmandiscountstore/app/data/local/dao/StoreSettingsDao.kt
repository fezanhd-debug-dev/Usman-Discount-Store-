package com.usmandiscountstore.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.usmandiscountstore.app.data.local.entity.StoreSettingsEntity

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1")
    suspend fun get(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: StoreSettingsEntity)
}
