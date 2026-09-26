package com.usmandiscountstore.app.data.local.dao

import androidx.room.*
import com.usmandiscountstore.app.data.local.entity.BonusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BonusDao {
    @Query("SELECT * FROM bonus ORDER BY addedAt DESC")
    fun getAll(): Flow<List<BonusEntity>>

    @Query("SELECT * FROM bonus WHERE staffId = :staffId AND monthYear = :monthYear")
    suspend fun getByStaffMonth(staffId: Long, monthYear: String): List<BonusEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM bonus WHERE staffId = :staffId AND monthYear = :monthYear")
    suspend fun totalForStaffMonth(staffId: Long, monthYear: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bonus: BonusEntity): Long

    @Query("DELETE FROM bonus WHERE id = :id")
    suspend fun delete(id: Long)
}
