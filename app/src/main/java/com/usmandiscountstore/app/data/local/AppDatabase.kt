package com.usmandiscountstore.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.usmandiscountstore.app.data.local.dao.*
import com.usmandiscountstore.app.data.local.entity.*

@Database(
    entities = [
        StaffEntity::class,
        AttendanceEntity::class,
        StoreSettingsEntity::class,
        AdvanceEntity::class,
        BonusEntity::class,
        LeaveRequestEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun bonusDao(): BonusDao
    abstract fun leaveDao(): LeaveDao

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
