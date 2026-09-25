package com.usmandiscountstore.app.data.local.dao

import androidx.room.*
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY markedAt DESC")
    fun getByDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE staffId = :staffId ORDER BY date DESC LIMIT 60")
    fun getByStaff(staffId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE staffId = :staffId AND date = :date LIMIT 1")
    suspend fun getByStaffAndDate(staffId: Long, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE staffId = :staffId AND date LIKE :monthLike ORDER BY date ASC")
    suspend fun getStaffMonthly(staffId: Long, monthLike: String): List<AttendanceEntity>

    @Query("SELECT COUNT(*) FROM attendance WHERE staffId = :staffId AND status = 'PRESENT'")
    suspend fun countPresent(staffId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity): Long

    @Update
    suspend fun update(attendance: AttendanceEntity)

    @Delete
    suspend fun delete(attendance: AttendanceEntity)
}
