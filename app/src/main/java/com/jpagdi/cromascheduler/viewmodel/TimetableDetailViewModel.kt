package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

data class LineageEntry(val run: ScheduleRunEntity, val conflictCount: Int)

/**
 * One TIMETABLE's whole history — [rootRunId] is the original Generate run; [entries] is that
 * run plus every Validate/Repair/Optimize built from it, oldest first, so the detail screen can
 * render it as one scrollable timeline instead of Validate/Optimize each spawning a new Home card.
 * runId always refers to the root — Validate/Repair/Optimize act on [latest] (the most recent
 * entry), since that's the timetable a person actually means by "this timetable" once history exists.
 */
class TimetableDetailViewModel(private val repository: ScheduleRepository, private val rootRunId: String) : ViewModel() {
    var entries by mutableStateOf<List<LineageEntry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var deleted by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set

    val root: ScheduleRunEntity? get() = entries.firstOrNull()?.run
    val latest: LineageEntry? get() = entries.lastOrNull()

    fun load() {
        viewModelScope.launch {
            val lineage = repository.getLineage(rootRunId)
            entries = lineage.map { LineageEntry(it, repository.getConflicts(it.id).size) }
            loaded = true
        }
    }

    /** Validate the latest entry — read-only, so it never creates a new row; just reports the count. */
    fun validateLatest() {
        val target = latest?.run ?: return
        viewModelScope.launch {
            busy = true
            repository.validate(target.id)
            load()
            busy = false
        }
    }

    /** Optimize the latest entry — creates one new lineage entry (rootRunId stamped by the repository), reloads the whole timeline in place. */
    fun optimizeLatest() {
        val target = latest?.run ?: return
        viewModelScope.launch {
            busy = true
            repository.optimize(target.id)
            load()
            busy = false
        }
    }

    /** Repair the latest entry — same in-place-append behavior as optimize. */
    fun repairLatest() {
        val target = latest?.run ?: return
        viewModelScope.launch {
            busy = true
            repository.repair(target.id)
            load()
            busy = false
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteRun(rootRunId)
            deleted = true
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository, rootRunId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TimetableDetailViewModel(repository, rootRunId) as T
        }
    }
}
