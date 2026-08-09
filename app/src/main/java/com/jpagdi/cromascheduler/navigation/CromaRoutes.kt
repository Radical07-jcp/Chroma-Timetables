package com.jpagdi.cromascheduler.navigation

/**
 * One flat set of string routes for the whole app, behind a single NavHost. The New Timetable
 * wizard (CREATE_CHOOSE_TYPE -> CREATE_DEFINE_PERIODS -> CREATE_GENERATE) and the Repair-upload
 * wizard (REPAIR_CHOOSE_TYPE -> REPAIR_DEFINE_PERIODS -> REPAIR_UPLOAD) each share their own
 * CreateTimetableViewModel instance (created once in MainActivity, reset when the wizard finishes
 * or is backed out of) rather than passing type/period state through nav arguments — a List of
 * PeriodBlock doesn't fit cleanly into a nav route string.
 */
object CromaRoutes {
    const val HOME = "home"

    const val CREATE_CHOOSE_TYPE = "create/choose_type"
    const val CREATE_DEFINE_PERIODS = "create/define_periods"
    const val CREATE_GENERATE = "create/generate"

    const val REPAIR_CHOOSE_TYPE = "repair/choose_type"
    const val REPAIR_DEFINE_PERIODS = "repair/define_periods"
    const val REPAIR_UPLOAD = "repair/upload"

    const val IMPORT = "import/{type}"
    const val TIMETABLE_DETAIL = "timetable/{runId}"
    const val VALIDATE = "validate/{runId}"
    const val REPAIR = "repair/fix/{runId}"
    const val OPTIMIZE = "optimize/{runId}"
    const val RESULTS = "results/{runId}"
    const val EXPORT = "export/{runId}/{runName}"
    const val TEACHERS = "teachers"
    const val TEACHER_AVAILABILITY = "teacher_availability/{teacherId}/{teacherName}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun import(type: String) = "import/$type"
    fun timetableDetail(runId: String) = "timetable/$runId"
    fun validate(runId: String) = "validate/$runId"
    fun repair(runId: String) = "repair/fix/$runId"
    fun optimize(runId: String) = "optimize/$runId"
    fun results(runId: String) = "results/$runId"
    fun export(runId: String, runName: String) = "export/$runId/${java.net.URLEncoder.encode(runName, "UTF-8")}"
    fun teacherAvailability(teacherId: String, teacherName: String) =
        "teacher_availability/$teacherId/${java.net.URLEncoder.encode(teacherName, "UTF-8")}"
}
