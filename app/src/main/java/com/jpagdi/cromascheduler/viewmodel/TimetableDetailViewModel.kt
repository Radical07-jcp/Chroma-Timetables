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
class TimetableDetailViewModel(private val repository: ScheduleRepository, initialRootRunId: String) : ViewModel() {
    // Mutable because deleting the root version while other versions still exist promotes a new
    // root under the same lineage — this keeps pointing reload()/actions at whichever run is
    // currently the root, instead of the id this screen happened to be opened with.
    private var currentRootId: String = initialRootRunId

    var entries by mutableStateOf<List<LineageEntry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var deleted by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var repairConflictDetails by mutableStateOf<List<ScheduleRepository.ConflictDetail>>(emptyList())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Bumped after every action that changes this lineage (optimize/repair/validate/delete-
     * promotion). The screen observes this and rebuilds this ViewModel from scratch with a fresh
     * `viewModel(key=...)` call when it changes — i.e. it always re-derives state exactly the way
     * "go back to the timetable list, then open this timetable again" does, since that path has
     * been confirmed to reliably show the latest version and in-place state patching has not.
     * This sidesteps needing to pin down exactly why the in-place path lagged; it just never
     * relies on it being correct.
     */
    var refreshToken by mutableStateOf(0)
        private set

    val root: ScheduleRunEntity? get() = entries.firstOrNull()?.run
    val latest: LineageEntry? get() = entries.lastOrNull()

    fun clearError() {
        errorMessage = null
    }

    fun load() {
        viewModelScope.launch {
            try {
                reload()
            } catch (t: Throwable) {
                errorMessage = "Couldn't load this timetable: ${t.message ?: t::class.simpleName}"
                loaded = true
            }
        }
    }

    private suspend fun reload() {
        // The nav route this screen is opened with may be a lineage's true root id, OR one of
        // its child versions' own id — Optimize/Repair both navigate here with the run they
        // just created, which is a child, not the root. Resolve to the true root first so the
        // full lineage always loads (siblings included), instead of silently showing only the
        // one run that happens to match whatever id we were handed.
        val given = repository.getRun(currentRootId)
        val rootRunId = given?.rootRunId
        if (rootRunId != null) currentRootId = rootRunId
        val lineage = repository.getLineage(currentRootId)
        entries = lineage.map { LineageEntry(it, repository.getConflicts(it.id).size) }
        loaded = true
    }

    /** Validate the latest entry — read-only, so it never creates a new row; just reports the count. */
    fun validateLatest() {
        val target = latest?.run ?: return
        viewModelScope.launch {
            busy = true
            try {
                repository.validate(target.id)
                refreshToken += 1
            } catch (t: Throwable) {
                errorMessage = "Validate failed: ${t.message ?: t::class.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    /** Optimize the latest entry — creates one new lineage entry (rootRunId stamped by the repository). */
    fun optimizeLatest() {
        val target = latest?.run ?: return
        viewModelScope.launch {
            busy = true
            try {
                repository.optimize(target.id)
                refreshToken += 1
            } catch (t: Throwable) {
                errorMessage = "Optimize failed: ${t.message ?: t::class.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    /** Repair the latest entry — same in-place-append behavior as optimize. */
    fun loadRepairConflicts() {
        val target = latest?.run ?: return
        viewModelScope.launch { repairConflictDetails = repository.getConflictDetails(target.id) }
    }

    fun repairLatest(selectedSessionIds: Set<String>) {
        val target = latest?.run ?: return
        if (selectedSessionIds.isEmpty()) return
        viewModelScope.launch {
            busy = true
            try {
                repository.repair(target.id, selectedSessionIds = selectedSessionIds)
                refreshToken += 1
            } catch (t: Throwable) {
                errorMessage = "Repair failed: ${t.message ?: t::class.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun renameVersion(runId: String, name: String) {
        viewModelScope.launch {
            repository.renameRun(runId, name)
            refreshToken += 1
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteRun(currentRootId)
            deleted = true
        }
    }

    /**
     * Delete one saved version. If it's a derived (non-root) version, only that entry goes away
     * and the rest of the history is untouched. If it's the root and other versions still exist,
     * the lineage survives on the next-oldest version — [currentRootId] follows that promotion, and
     * [refreshToken] tells the screen to rebuild fresh from it rather than trust the in-place state.
     * [deleted] (and [onDeleted]) only fire when this was truly the last version left.
     */
    fun deleteVersion(runId: String, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newRootId = repository.deleteVersion(runId)
            if (newRootId == null) {
                deleted = true
                onDeleted?.invoke()
            } else {
                currentRootId = newRootId
                refreshToken += 1
                onDeleted?.invoke()
            }
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository, rootRunId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TimetableDetailViewModel(repository, rootRunId) as T
        }
    }
}
