package com.jpagdi.cromascheduler.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jpagdi.cromascheduler.di.AppContainer

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        ImportViewModel::class.java -> ImportViewModel(container.csvImportService) as T
        ScheduleViewModel::class.java -> ScheduleViewModel(container.scheduleRepository) as T
        ResultsViewModel::class.java -> ResultsViewModel(container.scheduleRepository) as T
        ExportViewModel::class.java -> ExportViewModel(
            container.scheduleRepository, container.csvExporter, container.xlsxExporter, container.pdfExporter,
        ) as T
        PeriodConfigViewModel::class.java -> PeriodConfigViewModel(container.scheduleRepository) as T
        TeachersViewModel::class.java -> TeachersViewModel(container.scheduleRepository) as T
        TeacherAvailabilityViewModel::class.java -> TeacherAvailabilityViewModel(container.scheduleRepository) as T
        HomeViewModel::class.java -> HomeViewModel(container.scheduleRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
