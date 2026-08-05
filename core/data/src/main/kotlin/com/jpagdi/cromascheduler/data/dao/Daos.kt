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

    @Query("SELECT * FROM availability_blocks WHERE entityType = :type AND entityId = :entityId")
    suspend fun getBlocksFor(type: AvailabilityEntityType, entityId: String): List<AvailabilityBlockEntity>

    @Query("DELETE FROM availability_blocks")
    suspend fun clear()
}

@Dao
interface TimeslotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(timeslots: List<TimeslotEntity>)

    @Query("SELECT * FROM timeslots ORDER BY dayOfWeek, periodIndex")
    suspend fun getAll(): List<TimeslotEntity>

    @Query("DELETE FROM timeslots")
    suspend fun clear()
}

@Dao
interface ScheduleRunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(run: ScheduleRunEntity)

    @Query("SELECT * FROM schedule_runs ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<ScheduleRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<ScheduleAssignmentEntity>)

    @Query("SELECT * FROM schedule_assignments WHERE scheduleRunId = :runId")
    suspend fun getAssignmentsFor(runId: String): List<ScheduleAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConflicts(conflicts: List<ConflictRecordEntity>)

    @Query("SELECT * FROM conflict_records WHERE scheduleRunId = :runId")
    suspend fun getConflictsFor(runId: String): List<ConflictRecordEntity>
}
