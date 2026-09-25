package com.usmandiscountstore.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.usmandiscountstore.app.data.local.dao.AdvanceDao
import com.usmandiscountstore.app.data.local.dao.AttendanceDao
import com.usmandiscountstore.app.data.local.dao.StaffDao
import com.usmandiscountstore.app.data.local.dao.StoreSettingsDao
import com.usmandiscountstore.app.data.local.entity.AdvanceEntity
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.data.local.entity.StoreSettingsEntity

@Database(
    entities = [
        StaffEntity::class,
        AttendanceEntity::class,
        StoreSettingsEntity::class,
        AdvanceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun advanceDao(): AdvanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uds_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
