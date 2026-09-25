package com.usmandiscountstore.app.data.local.dao

import androidx.room.*
import com.usmandiscountstore.app.data.local.entity.AdvanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advance ORDER BY addedAt DESC")
    fun getAll(): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advance WHERE staffId = :staffId ORDER BY addedAt DESC")
    fun getByStaff(staffId: Long): Flow<List<AdvanceEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance WHERE staffId = :staffId")
    suspend fun totalForStaff(staffId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(advance: AdvanceEntity): Long

    @Query("DELETE FROM advance WHERE id = :id")
    suspend fun delete(id: Long)
}
