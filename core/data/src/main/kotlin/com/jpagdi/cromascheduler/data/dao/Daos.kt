package com.jpagdi.cromascheduler.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jpagdi.cromascheduler.data.entity.*

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(teachers: List<TeacherEntity>)

    @Query("SELECT * FROM teachers")
    suspend fun getAll(): List<TeacherEntity>

    @Query("DELETE FROM teachers")
    suspend fun clear()
}

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<SubjectEntity>

    @Query("DELETE FROM subjects")
    suspend fun clear()
}

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rooms: List<RoomEntity>)

    @Query("SELECT * FROM rooms")
    suspend fun getAll(): List<RoomEntity>

    @Query("DELETE FROM rooms")
    suspend fun clear()
}

@Dao
interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sections: List<SectionEntity>)

    @Query("SELECT * FROM sections")
    suspend fun getAll(): List<SectionEntity>

    @Query("DELETE FROM sections")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<SessionEntity>

    @Query("DELETE FROM sessions")
    suspend fun clear()
}

@Dao
interface AvailabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(blocks: List<AvailabilityBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(block: AvailabilityBlockEntity)

    @Query("SELECT * FROM availability_blocks WHERE entityType = :type AND entityId = :entityId")
    suspend fun getBlocksFor(type: AvailabilityEntityType, entityId: String): List<AvailabilityBlockEntity>

    @Query("DELETE FROM availability_blocks WHERE entityType = :type AND entityId = :entityId AND dayOfWeek = :day AND periodIndex = :period")
    suspend fun deleteBlock(type: AvailabilityEntityType, entityId: String, day: Int, period: Int)

    @Query("DELETE FROM availability_blocks")
    suspend fun clear()
}

@Dao
interface ScheduleRunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(run: ScheduleRunEntity)

    @Query("SELECT * FROM schedule_runs ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<ScheduleRunEntity>

    /** Home's list — one row per lineage (roots only). rootRunId IS NULL means "this run IS the root". */
    @Query("SELECT * FROM schedule_runs WHERE rootRunId IS NULL ORDER BY createdAtEpochMillis DESC")
    suspend fun getAllRoots(): List<ScheduleRunEntity>

    /** Everything under one lineage — the root itself plus every repair/optimize built from it, oldest first (a readable history order). */
    @Query("SELECT * FROM schedule_runs WHERE id = :rootRunId OR rootRunId = :rootRunId ORDER BY createdAtEpochMillis ASC")
    suspend fun getLineage(rootRunId: String): List<ScheduleRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<ScheduleAssignmentEntity>)

    @Query("SELECT * FROM schedule_assignments WHERE scheduleRunId = :runId")
    suspend fun getAssignmentsFor(runId: String): List<ScheduleAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConflicts(conflicts: List<ConflictRecordEntity>)

    @Query("SELECT * FROM conflict_records WHERE scheduleRunId = :runId")
    suspend fun getConflictsFor(runId: String): List<ConflictRecordEntity>

    @Query("SELECT * FROM schedule_runs WHERE id = :runId LIMIT 1")
    suspend fun getById(runId: String): ScheduleRunEntity?

    @Query("SELECT * FROM schedule_runs WHERE sessionType = :type ORDER BY createdAtEpochMillis DESC")
    suspend fun getAllByType(type: SessionTypeEntity): List<ScheduleRunEntity>

    @Query("DELETE FROM schedule_runs WHERE id = :runId")
    suspend fun deleteRun(runId: String)

    @Query("DELETE FROM schedule_assignments WHERE scheduleRunId = :runId")
    suspend fun deleteAssignmentsFor(runId: String)

    @Query("DELETE FROM conflict_records WHERE scheduleRunId = :runId")
    suspend fun deleteConflictsFor(runId: String)

    /** One row per run that has at least one conflict — runs with zero conflicts simply don't appear, which the caller reads as "clean". Powers the Home list's status pill without an N+1 query per run. */
    @Query("SELECT scheduleRunId, COUNT(*) as conflictCount FROM conflict_records GROUP BY scheduleRunId")
    suspend fun getConflictCountsByRun(): List<RunConflictCount>
}

data class RunConflictCount(val scheduleRunId: String, val conflictCount: Int)
