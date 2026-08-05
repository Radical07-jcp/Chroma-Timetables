// Deliberately a Kotlin JVM module, not an Android library module — this module must
// never gain an android.* import. That constraint is what makes the scheduling engine
// unit-testable on plain JVM (no emulator needed) and portable if the UI layer is ever
// swapped out entirely.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
