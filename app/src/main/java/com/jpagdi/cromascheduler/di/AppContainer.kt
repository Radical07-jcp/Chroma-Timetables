package com.jpagdi.cromascheduler.di

import android.content.Context
import androidx.room.Room
import com.jpagdi.cromascheduler.data.db.CromaDatabase
import com.jpagdi.cromascheduler.data.export.CsvScheduleExporter
import com.jpagdi.cromascheduler.data.export.PdfScheduleExporter
import com.jpagdi.cromascheduler.data.export.XlsxScheduleExporter
import com.jpagdi.cromascheduler.data.repository.CsvImportService
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository

/**
 * Manual dependency container — no Hilt/Koin. The app is small enough (one Room
 * database, a handful of repositories) that a DI framework would be a new
 * dependency bought for very little benefit. Everything here is a plain singleton
 * built once in Application.onCreate() and handed to composables via
 * CompositionLocal (see LocalAppContainer below).
 */
class AppContainer(context: Context) {
    val database: CromaDatabase = Room.databaseBuilder(
        context.applicationContext,
        CromaDatabase::class.java,
        CromaDatabase.DATABASE_NAME,
    ).build()

    val scheduleRepository = ScheduleRepository(database)
    val csvImportService = CsvImportService(database)
    val csvExporter = CsvScheduleExporter()
    val xlsxExporter = XlsxScheduleExporter()
    val pdfExporter = PdfScheduleExporter()
}
