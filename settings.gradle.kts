pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CromaScheduler"

// :app             — Compose UI, ViewModels, navigation. Depends on all three below.
// :core:engine     — pure Kotlin. Graph model + coloring algorithms + constraint validation.
//                    NO android.* imports, ever — this is what keeps the scheduling math
//                    testable on a plain JVM and reusable if the UI is ever rebuilt.
// :core:data       — Room entities/DAOs, repositories, CSV import/export.
// :core:designsystem — theme, colors, shapes, reusable rounded-card composables
//                    (the same visual language as MCQ Quick Check, rebuilt in Compose).
include(":app")
include(":core:engine")
include(":core:data")
include(":core:designsystem")
