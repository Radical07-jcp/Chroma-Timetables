# CHROMA_ABSORB_NOTES — MCQ Quick Check → Chroma Timetables

What changed in this pass, mapped to each requested item. Everything below is real code against the
actual entities/repository/ViewModels from the prior phases — not a re-skin of placeholder screens.

## 1. Home screen: saved-test list → Timetables list with status
`home/HomeActivity.kt` + `res/layout/activity_home.xml` + `res/layout/row_timetable_card.xml`.
Lists every `ScheduleRunEntity` as a card (name, type • algorithm • date, and a CLEAN / N CONFLICTS
status pill computed from `ScheduleRepository.getConflictCountsByRun()`, a new bulk query added for
this). Refreshes in `onResume()`, same pattern as the reference app's own list activity.

## 2. Test Details → Specific Timetable, with buttons migrated from Home
`home/TimetableDetailActivity.kt` + `res/layout/activity_timetable_detail.xml`. The old Home
Dashboard's Validate / Repair / Export buttons (plus Optimize and View, which used to live behind
"pick a run" screens) now live here, already scoped to one `runId` — no more intermediate run-picker.
Generate New Schedule and Import Data are the only two actions that stayed on Home, since they don't
belong to any one existing timetable.

## 3. CHROMA / TIMETABLES header
`activity_home.xml` and `drawer_content.xml` both use the same two-TextView, two-size pattern
(18sp bold / 9sp letter-spaced) as the reference app's "MCQ" / "QUICK CHECK" header.

## 4. Sidebar + buttons
`res/layout/drawer_content.xml`, wired by `home/DrawerHelper.kt`: SCHEDULING (Import Data, Generate
New Schedule, Define Periods), DATA (Teachers), PREFERENCES (Settings), SUPPORT (About / no-AI
note). Opens via the header hamburger (`ic_menu.xml`).

## 5. App icon
Re-cropped from your uploaded artwork (auto-trimmed the white margin, then re-centered at ~60% of
canvas for the adaptive foreground so nothing gets clipped by launcher masks) and regenerated at all
five densities as both adaptive (foreground+background) and legacy icons. The second upload replaced
the source used for this.

## 6. CSV import / generate must not mix schedule types
- `data/entity/ScheduleEntities.kt`: `ScheduleRunEntity` gained `sessionType`, with a real Room
  `Migration(2, 3)` (not a destructive fallback).
- `data/repository/CsvImportService.kt`: `importFromFiles`/`importFromZip` now take an
  `expectedSessionType`; any `sessions.csv` row whose own `type` doesn't match is rejected as a
  normal row-level `CsvValidationError` (skipped, not a hard failure) instead of silently imported.
- `ui/ImportScreen.kt`: a mandatory type-picker dialog (`ScheduleTypePromptDialog`, shared with
  Generate) gates both the zip and multi-file pickers — you cannot import without first confirming
  which schedule type the data is for.
- `data/repository/ScheduleRepository.kt`: `generate()` now takes a mandatory `sessionType` and ANDs
  it into session selection, so a run's colored sessions can never mix types either.
- `ui/GenerateScreen.kt`: replaced the old class/exam-only toggle with the same 5-type prompt.

## 7. Maintained mixed XML + Compose
See `MIXED_UI.md` at the project root for the full rationale and the exact line between the two.
Short version: Home/TimetableDetail/drawer are XML Views; every screen they hand off to (Import,
Generate, Validate, Repair, Results, Export, Teachers, Teacher Availability, Define Periods,
Settings) is the existing Compose screen from earlier phases, hosted one-per-Activity via the new
`host/ComposeHostActivity.kt`.

## Also fixed along the way
- `AppContainer.kt` called `Room.databaseBuilder(...)` directly but `:app` never declared a Room
  dependency — moved database construction into `:core:data`'s own `buildCromaDatabase()` (already
  had the dependency) and had `AppContainer` call that instead.
- Old bottom-tab Compose nav (`MainActivity.kt`, `navigation/Screen.kt`, `HomeTabScreen.kt`,
  `TimetableTabScreen.kt`, `RunsListScreen.kt`) removed — fully superseded by
  HomeActivity/TimetableDetailActivity/ComposeHostActivity.

## Known limitations, same caveat as the first absorb
This has still never been through an actual Gradle/Android build (no network access to Google's
Maven repo in this environment) — I traced every signature and import by hand against the real
source, but a real build is the only way to be certain. Worth a first `./gradlew assembleDebug`
before relying on it.
