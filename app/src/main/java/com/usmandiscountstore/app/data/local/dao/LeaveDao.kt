package com.usmandiscountstore.app.data.local.dao

import androidx.room.*
import com.usmandiscountstore.app.data.local.entity.LeaveRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_requests ORDER BY requestedAt DESC")
    fun getAll(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY requestedAt DESC")
    fun getPending(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT COUNT(*) FROM leave_requests WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM leave_requests WHERE staffId = :staffId ORDER BY requestedAt DESC")
    fun getByStaff(staffId: Long): Flow<List<LeaveRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: LeaveRequestEntity): Long

    @Update
    suspend fun update(request: LeaveRequestEntity)

    @Query("UPDATE leave_requests SET status = :status, reviewedBy = :by, reviewedAt = :at, adminNote = :note WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, by: String, at: Long, note: String)

    @Query("DELETE FROM leave_requests WHERE id = :id")
    suspend fun delete(id: Long)
}
