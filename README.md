# CromaScheduler

Offline, on-device schedule generator using graph-coloring algorithms (Greedy,
Welsh-Powell, DSATUR). No AI/ML — all scheduling decisions are deterministic.

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
- [ ] Phase 4 — Graph coloring scheduling engine (Greedy, Welsh-Powell, DSATUR)
- [ ] Phase 5 — Constraint validation and conflict detection
- [ ] Phase 6 — Conflict repair and schedule optimization
- [ ] Phase 7 — Android UI (navigation, all destination screens, results screens)
- [ ] Phase 8 — Export (CSV/Excel/PDF/print) and testing

## Module boundaries (do not violate)

- `:core:engine` is a pure Kotlin JVM module — **no `android.*` imports, ever.**
  This is what keeps the scheduling math unit-testable without an emulator.
- Room assignment is a separate pass after graph coloring, not part of the
  conflict graph itself — see `ConflictGraph.kt` doc comment for why.
- `:core:data` entities are Room-specific and intentionally separate from
  `:core:engine`'s model classes (e.g. `SessionTypeEntity` vs `SessionType`) —
  mapping between them happens once, in the repository layer, so the engine
  never depends on Room/Android.
