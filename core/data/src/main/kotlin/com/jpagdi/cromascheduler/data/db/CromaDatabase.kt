package com.jpagdi.cromascheduler.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jpagdi.cromascheduler.data.dao.*
import com.jpagdi.cromascheduler.data.entity.*

/**
 * version = 1 for Phase 1. Bump on every schema change from here on and add a
 * Migration — CromaScheduler stores real school data across terms, so unlike a
 * scratch/rebuildable cache, destructive fallback migration is not acceptable
 * once this ships. (fallbackToDestructiveMigration is intentionally NOT set below.)
 */
@Database(
    entities = [
        TeacherEntity::class,
        SubjectEntity::class,
        RoomEntity::class,
        SectionEntity::class,
        SessionEntity::class,
        AvailabilityBlockEntity::class,
        TimeslotEntity::class,
        ScheduleRunEntity::class,
        ScheduleAssignmentEntity::class,
        ConflictRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CromaDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun subjectDao(): SubjectDao
    abstract fun roomDao(): RoomDao
    abstract fun sectionDao(): SectionDao
    abstract fun sessionDao(): SessionDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun timeslotDao(): TimeslotDao
    abstract fun scheduleRunDao(): ScheduleRunDao

    companion object {
        const val DATABASE_NAME = "croma_scheduler.db"
    }
}
