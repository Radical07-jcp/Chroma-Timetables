package com.jpagdi.cromascheduler.data.repository

import com.jpagdi.cromascheduler.data.entity.SessionEntity
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.SessionType

/**
 * The single conversion point between the Room-facing SessionTypeEntity/SessionEntity
 * and the engine-facing SessionType/EngineSession. Kept as a small standalone mapper
 * (not a method on either class) so neither :core:data's entities nor :core:engine's
 * models need to know the other package exists — :core:data is allowed to depend on
 * :core:engine (declared in build.gradle.kts), but the reverse must never happen.
 */
fun SessionEntity.toEngineSession(): EngineSession = EngineSession(
    id = id,
    type = when (type) {
        SessionTypeEntity.CLASS -> SessionType.CLASS
        SessionTypeEntity.EXAM -> SessionType.EXAM
        SessionTypeEntity.LAB -> SessionType.LAB
        SessionTypeEntity.MEETING -> SessionType.MEETING
        SessionTypeEntity.SEMINAR -> SessionType.SEMINAR
    },
    subjectId = subjectId,
    teacherId = teacherId,
    sectionId = sectionId,
    roomTypeRequired = roomTypeRequired,
    durationPeriods = durationPeriods,
)

fun List<SessionEntity>.toEngineSessions(): List<EngineSession> = map { it.toEngineSession() }
