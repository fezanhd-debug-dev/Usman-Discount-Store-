package com.usmandiscountstore.app.data.local.dao

import androidx.room.*
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE isActive = 1 ORDER BY id DESC")
    fun getAllActive(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff ORDER BY id DESC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getById(id: Long): StaffEntity?

    @Query("SELECT COUNT(*) FROM staff WHERE isActive = 1")
    suspend fun getActiveCount(): Int

    @Query("SELECT COUNT(*) FROM staff WHERE isActive = 1 AND role = 'MODERATOR'")
    suspend fun getModeratorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(staff: StaffEntity): Long

    @Update
    suspend fun update(staff: StaffEntity)

    @Query("UPDATE staff SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun delete(id: Long)
}
