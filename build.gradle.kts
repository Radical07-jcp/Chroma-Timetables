// Top-level build file. Plugin versions are declared here (apply false) so every
// module below references the same version without repeating it — bump once here
// when upgrading Android Gradle Plugin / Kotlin / Compose compiler.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
