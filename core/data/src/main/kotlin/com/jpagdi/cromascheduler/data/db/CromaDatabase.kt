package com.jpagdi.cromascheduler.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jpagdi.cromascheduler.data.dao.*
import com.jpagdi.cromascheduler.data.entity.*

/**
 * version = 1 for Phase 1. Bump on every schema change from here on and add a
 * Migration — CromaScheduler stores real school data across terms, so unlike a
 * scratch/rebuildable cache, destructive fallback migration is not acceptable
 * once this ships. (fallbackToDestructiveMigration is intentionally NOT set below.)
 *
 * exportSchema is false for now — enabling it requires wiring a room.schemaLocation
 * KSP argument (Gradle config, not just this annotation) to give the schema JSON
 * somewhere to land. Not worth doing before the schema has settled; flip this to
 * true and add that KSP arg once you're getting close to a real release, so
 * migrations from that point on have a schema history to diff against.
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
        PeriodConfigEntity::class,
        ScheduleRunEntity::class,
        ScheduleAssignmentEntity::class,
        ConflictRecordEntity::class,
    ],
    version = 3, // bumped: ScheduleRunEntity gained sessionType (see MIGRATION_2_3 below)
    exportSchema = false,
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
    abstract fun periodConfigDao(): PeriodConfigDao
    abstract fun scheduleRunDao(): ScheduleRunDao

    companion object {
        const val DATABASE_NAME = "croma_scheduler.db"
    }
}

/**
 * v2 -> v3: adds schedule_runs.sessionType (the "which schedule type is this run"
 * column that the Home/Timetable-detail rebuild and the type-specific
 * Generate/Import flow depend on). Existing rows default to CLASS — a real
 * migration, not fallbackToDestructiveMigration, per this file's own doc
 * comment above about not throwing away real school data across a schema bump.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE schedule_runs ADD COLUMN sessionType TEXT NOT NULL DEFAULT 'CLASS'",
        )
    }
}

/**
 * The only place Room.databaseBuilder() gets called — kept inside :core:data so
 * Room stays this module's implementation detail. :app's AppContainer calls this
 * function instead of touching Room directly, which is also what was actually
 * broken before: :app never declared a Room dependency (only :core:data did), so
 * `Room.databaseBuilder(...)` in AppContainer.kt was an unresolved reference.
 */
fun buildCromaDatabase(context: Context): CromaDatabase = Room.databaseBuilder(
    context.applicationContext,
    CromaDatabase::class.java,
    CromaDatabase.DATABASE_NAME,
)
    .addMigrations(MIGRATION_2_3)
    .build()
