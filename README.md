# Chroma Timetables

Offline, on-device schedule generator using graph-coloring algorithms (Greedy,
Welsh-Powell, DSATUR). No AI/ML, no cloud — every schedule is built entirely on
the device, deterministically. Same data + same algorithm always produces the
same result.

Kotlin, Jetpack Compose, Room, MVVM, multi-module.

## Phase status

- [x] **Phase 1 — Architecture, database schema, data models.** Module structure
      (`:app`, `:core:engine`, `:core:data`, `:core:designsystem`) in place.
      Room schema defined (Teacher/Subject/Room/Section/Session/Availability/
      Timeslot/ScheduleRun/ScheduleAssignment/ConflictRecord). Engine module has
      the vertex/edge model (`EngineSession`, `ConflictGraph`) and the
      `ColoringAlgorithm` strategy interface — implementations are Phase 4.
      Compose Home Dashboard scaffolded with the five entry points from the spec
      (Import/Generate/Validate/Repair/Export), no navigation wired yet.
- [x] **Phase 2 — CSV import, validation, local database storage.** Hand-rolled
      CSV parser (`CsvParser`, quoted-field support, no external CSV library
      dependency) plus one parser per file (teachers/subjects/rooms/sections/
      sessions/availability) that does row-level validation and reports errors
      per row instead of failing the whole import. `ZipCsvReader` supports the
      "import one zip with all six files" workflow. `CrossFileValidator` catches
      dangling references across files (e.g. a session pointing at a teacherId
      that doesn't exist). `CsvImportService` orchestrates parse → validate →
      persist in one Room transaction. **Assumed CSV column layouts are
      documented at the top of each parser function in `EntityCsvParsers.kt` /
      `SessionAvailabilityCsvParsers.kt` — flagged for your review since the
      spec didn't pin down exact headers.**
- [x] **Phase 3 — Graph construction module.** `GraphBuilder.buildConflictGraph()`
      groups sessions by teacherId/sectionId (not naive all-pairs comparison —
      matters at "hundreds to thousands of sessions" scale) and adds a conflict
      edge for every pair that shares either. Explicit user-defined conflict
      links are not yet wired to any data model field — `addLinkedConflicts()`
      is a documented extension seam for when that's needed.
- [x] **Phase 4 — Graph coloring scheduling engine.** `GreedyColoring`,
      `WelshPowellColoring`, `DsaturColoring` (default per spec) all implement the
      `ColoringAlgorithm` interface and are registered in
      `ColoringAlgorithmRegistry`. A "color" is a start `Timeslot`; multi-period
      sessions reserve a contiguous same-day run (`ColoringSupport.findValidStart`).
      `fixedAssignments` lets any algorithm pin some sessions in place — this is
      what Repair mode (Phase 6) reuses instead of a separate code path. Also added:
      `RoomAssigner` (the separate post-coloring resource-assignment pass decided
      in Phase 1) and `SchedulingEngine`, the facade Generate mode calls end to end.
- [x] **Phase 5 — Constraint validation and conflict detection.**
      `ConstraintValidator` checks every hard constraint from the spec (teacher/
      room/section double-booking, teacher/room availability, room capacity,
      duration-fits-in-day) against a fully-formed schedule and returns typed
      `ConstraintViolation`s. Soft constraints are NOT pass/fail here — they're
      scored separately in Phase 6.
- [x] **Phase 6 — Conflict repair and schedule optimization.** `RepairEngine`
      detects conflicts via ConstraintValidator, preserves every session not
      involved in one, and recolors only the rest (spec: "recalculate only the
      conflicting sessions"). `ScheduleQualityScorer` quantifies all five soft
      constraints (teacher idle time, room changes, section compactness, morning
      preference, room utilization) into one comparable score; `ScheduleOptimizer`
      is a bounded, deterministic local search that improves that score without
      ever violating a hard constraint or changing a session's room.
- [x] **Phase 7 — Android UI.** `ScheduleRepository` (:core:data) bridges Room
      entities to the engine and back — every mode (Generate/Generate Exam/
      Validate/Repair/Optimize) goes through it. Hand-rolled navigation (no
      Navigation-Compose dependency — see `Screen.kt`) between Home, Import,
      Generate, Validate, Repair, Results (Weekly/Daily/Teacher/Room/Section tabs
      + statistics), and Export. ViewModels (`ImportViewModel`, `ScheduleViewModel`,
      `ResultsViewModel`, `ExportViewModel`) wired through a manual `AppContainer`
      (no Hilt/Koin — see its doc comment).
- [x] **Phase 8 — Export and testing.** `CsvScheduleExporter` (plain text),
      `XlsxScheduleExporter` (hand-rolled minimal valid .xlsx — no POI dependency,
      see its doc comment), `PdfScheduleExporter` (Android's built-in
      `PdfDocument`, no dependency). Print reuses the same PDF via
      `PdfPrintDocumentAdapter` + `PrintManager`. Files share out through
      `FileProvider` (scoped to just the app's cache/exports folder).
      **Testing scope note:** engine unit tests (coloring, validation, repair) run
      on plain JVM and are included — I can't run them myself (no network/JVM
      execution in this sandbox) so they're unverified until you run them in
      Codespaces. Instrumented/UI tests aren't included — they'd need a real
      device or emulator, which isn't available here either; flag if you want a
      basic Compose UI test scaffold added later.

## Post-launch: rebrand + period configuration

Renamed to Chroma Timetables (app name, launcher icon from the supplied logo,
Home screen redesigned to a status-overview + workflow-cards layout, bottom
navigation with Home/Timetable/Teachers/Settings tabs). The "no AI" requirement
is stated explicitly in the Settings tab's About panel.

Also closed the timeslot gap flagged earlier: `PeriodConfigEntity` +
`TimeslotGenerator` (`:core:data/timeslot`) let each school set its own period
length (default 60 min, any custom value), periods per day, active days, and
an optional single daily break — Settings → Define Periods. Per-teacher
unavailability can now be set in-app too (Teachers tab → tap a teacher → tap
periods to toggle), not just via `availability.csv`.

## What genuinely needs your review
- **CSV column layouts are my best guess** (Phase 2) — see the callout at the top
  of `EntityCsvParsers.kt` / `SessionAvailabilityCsvParsers.kt`.
- **This has never been compiled.** No network access on my end means no Gradle
  dependency resolution, so I could not build or run this project even once.
  Everything above is written to compile based on the documented APIs of Room,
  Compose, and the Android SDK versions declared in the Gradle files, but the
  first real signal will be your build in Codespaces. If it fails, send me the
  error and I'll fix it directly — don't spend time debugging Gradle/AGP version
  mismatches yourself first.

## Module boundaries (do not violate)

- `:core:engine` is a pure Kotlin JVM module — **no `android.*` imports, ever.**
  This is what keeps the scheduling math unit-testable without an emulator.
- Room assignment is a separate pass after graph coloring, not part of the
  conflict graph itself — see `ConflictGraph.kt` doc comment for why.
- `:core:data` entities are Room-specific and intentionally separate from
  `:core:engine`'s model classes (e.g. `SessionTypeEntity` vs `SessionType`) —
  mapping between them happens once, in the repository layer, so the engine
  never depends on Room/Android.
