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
        ScheduleRunEntity::class,
        ScheduleAssignmentEntity::class,
        ConflictRecordEntity::class,
    ],
    version = 9, // schedule_assignments now supports per-run teacher/subject/class identity overrides
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
 * v3 -> v4: adds period_config.isUserConfigured — the flag GenerateScreen uses to require a real
 * Define Periods save before Generate is allowed. Existing rows default to false, which correctly
 * treats any period_config saved before this column existed as "still the auto-seeded default,
 * never actually confirmed by a person" — the safer assumption for a gate like this.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE period_config ADD COLUMN isUserConfigured INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/**
 * v4 -> v5: SessionTypeEntity dropped MEETING and SEMINAR — Faculty Meeting and Seminar schedules
 * are no longer distinct types the app supports. Room stores enums as their name() string, so this
 * isn't a column-type change, but any existing row with type='MEETING' or 'SEMINAR' would fail to
 * deserialize once the Kotlin enum no longer has that constant — deleting those rows (and anything
 * that points at them) is the only safe option, not silently coercing them to some other type.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM schedule_assignments WHERE sessionId IN (SELECT id FROM sessions WHERE type IN ('MEETING','SEMINAR'))")
        db.execSQL("DELETE FROM conflict_records WHERE sessionAId IN (SELECT id FROM sessions WHERE type IN ('MEETING','SEMINAR')) OR sessionBId IN (SELECT id FROM sessions WHERE type IN ('MEETING','SEMINAR'))")
        db.execSQL("DELETE FROM sessions WHERE type IN ('MEETING','SEMINAR')")
        db.execSQL("DELETE FROM schedule_runs WHERE sessionType IN ('MEETING','SEMINAR')")
    }
}

/**
 * v5 -> v6: periods stopped being one global, shared setting and became something each
 * schedule run defines for itself (see ScheduleRunEntity.periodBlocksEncoded/activeDaysEncoded).
 * The old `period_config` and `timeslots` tables are dropped outright rather than migrated —
 * there's nothing meaningful to carry forward (a single global config doesn't map onto "each run
 * has its own"), and existing runs simply fall back to a generic default the first time they're
 * validated/repaired/optimized/exported after this update (see ScheduleRepository.timeslotsFor()).
 * This is also the fix for the "choosing more than one day generates a blank timetable" bug: the
 * old flow was save-period-config -> separately regenerate a GLOBAL timeslot table -> Generate
 * reads that global table back — three separate steps sharing mutable state. The new flow computes
 * timeslots in memory, once, directly from the exact blocks/days just chosen, in the same call that
 * uses them — there's no longer a save-then-reload step where staleness could creep in.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS period_config")
        db.execSQL("DROP TABLE IF EXISTS timeslots")
        db.execSQL("ALTER TABLE schedule_runs ADD COLUMN periodBlocksEncoded TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE schedule_runs ADD COLUMN activeDaysEncoded TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v6 -> v7: schedule_runs gained rootRunId. Existing rows all default to NULL, i.e. every
 * pre-existing run is treated as its own root — correct, since before this there was no lineage
 * concept at all and every run (including old repair/optimize output) was already its own flat
 * Home entry. Only runs created by repair()/optimize() AFTER this migration get a non-null
 * rootRunId pointing at their lineage's original Generate run.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE schedule_runs ADD COLUMN rootRunId TEXT DEFAULT NULL")
    }
}


/** v7 -> v8: stores the source-data snapshot used to build each timetable. The snapshot lets
 * users import a different ZIP for a new timetable without invalidating older saved schedules.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE schedule_runs ADD COLUMN sourceSnapshotEncoded TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v8 -> v9: adds schedule_assignments.overrideTeacherId/overrideSubjectId/overrideSectionId —
 * the guided Repair workflow's multi-select "Adjust by" needs a way to swap ONE identity field
 * (say, just Class) between two sessions for a single run while leaving day/period/room and the
 * shared roster-level SessionEntity untouched. All three default NULL, meaning every existing
 * assignment keeps using its session's own roster identity exactly as before this migration.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE schedule_assignments ADD COLUMN overrideTeacherId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE schedule_assignments ADD COLUMN overrideSubjectId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE schedule_assignments ADD COLUMN overrideSectionId TEXT DEFAULT NULL")
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
    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
    .build()
