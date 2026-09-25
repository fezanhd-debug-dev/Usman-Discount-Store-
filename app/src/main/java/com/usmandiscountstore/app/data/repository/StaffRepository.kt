package com.usmandiscountstore.app.data.repository

import com.usmandiscountstore.app.data.local.dao.StaffDao
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

class StaffRepository(private val dao: StaffDao) {
    fun getAllActive(): Flow<List<StaffEntity>> = dao.getAllActive()
    fun getAllStaff(): Flow<List<StaffEntity>> = dao.getAllStaff()
    suspend fun getById(id: Long): StaffEntity? = dao.getById(id)
    suspend fun getActiveCount(): Int = dao.getActiveCount()
    suspend fun getModeratorCount(): Int = dao.getModeratorCount()
    suspend fun add(staff: StaffEntity): Long = dao.insert(staff)
    suspend fun update(staff: StaffEntity) = dao.update(staff)
    suspend fun deactivate(id: Long) = dao.deactivate(id)
    suspend fun delete(id: Long) = dao.delete(id)
}
