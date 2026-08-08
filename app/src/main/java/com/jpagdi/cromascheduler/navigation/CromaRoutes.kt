package com.jpagdi.cromascheduler.navigation

/**
 * One flat set of string routes for the whole app — replaced the earlier per-Activity navigation
 * (each screen used to be its own Android Activity, launched via Intent) now that everything is
 * Compose behind a single NavHost. Route builders live next to their route strings so a call site
 * never hand-assembles a path string itself.
 */
object CromaRoutes {
    const val HOME = "home"
    const val WORKSPACE = "workspace/{type}"
    const val IMPORT = "import/{type}"
    const val GENERATE = "generate/{type}"
    const val VALIDATE = "validate/{runId}"
    const val REPAIR = "repair/{runId}"
    const val REPAIR_UPLOAD = "repair_upload"
    const val OPTIMIZE = "optimize/{runId}"
    const val RESULTS = "results/{runId}"
    const val EXPORT = "export/{runId}/{runName}"
    const val TEACHERS = "teachers"
    const val TEACHER_AVAILABILITY = "teacher_availability/{teacherId}/{teacherName}"
    const val DEFINE_PERIODS = "define_periods"
    const val SETTINGS = "settings"

    fun workspace(type: String) = "workspace/$type"
    fun import(type: String) = "import/$type"
    fun generate(type: String) = "generate/$type"
    fun validate(runId: String) = "validate/$runId"
    fun repair(runId: String) = "repair/$runId"
    fun optimize(runId: String) = "optimize/$runId"
    fun results(runId: String) = "results/$runId"
    fun export(runId: String, runName: String) = "export/$runId/${java.net.URLEncoder.encode(runName, "UTF-8")}"
    fun teacherAvailability(teacherId: String, teacherName: String) =
        "teacher_availability/$teacherId/${java.net.URLEncoder.encode(teacherName, "UTF-8")}"
}
