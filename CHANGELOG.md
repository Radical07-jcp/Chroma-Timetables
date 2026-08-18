## v1.0.2 — Repair & Conflict Resolution Patch

### Fixed
- Added Subject double-booking validation.
- Added conflict categorization for Teacher, Time Period, Room, Class, and Subject conflicts.
- Added targeted conflict repair from the timetable detail screen.
- Repair now allows selecting specific reported conflicts instead of repairing the entire timetable.
- Scoped repair preserves all unselected sessions and their existing assignments.
- Scoped room repair preserves rooms assigned to unaffected sessions.
- Optimizer focus is restricted to the affected sessions during targeted optimization.
- Optimizer swaps are restricted to sessions inside the selected repair scope.
- Existing conflict records are cleared before fresh validation results are persisted, preventing stale conflicts from remaining attached to a schedule version.
- Added conflict loading and selection UI for timetable repair.
- Preserved existing rename/version behavior while integrating the repair workflow.

### Safety
- Targeted repair does not unlock unrelated sessions.
- Targeted optimizer swaps cannot move an unrelated session as the partner of a selected session.
- Unresolved constraints remain reported after repair rather than being hidden.

# Chroma Engine Changelog

## [1.0.1] — Full UI/Workflow Revamp

### Typography & Branding
- Wired the UI typography to **Space Grotesk** through the Jetpack Compose Google Fonts provider, with the bundled Montserrat face retained only as an offline fallback; the UI no longer intentionally uses the Android system font.
- Preserved **Press Start 2P** for the CHROMA / TIMETABLES brand wordmark.
- Updated **CHROMA** to the gold brand color and **TIMETABLES** to a contrasting mint color.
- Adjusted the TIMETABLES size/letter spacing so its visual width better matches CHROMA.
- Added the same branded wordmark treatment to the shared screen chrome.

### In-App Logo
- Replaced the in-app display logo with the newly supplied artwork.
- Removed the remaining black background/fill from the two calendar squares and graph areas so those regions are transparent.
- Added a theme-aware contrasting shadow/halo around the transparent in-app logo.
- **Launcher icon assets were not changed by this pass**; the gold launcher identity remains separate from the transparent in-app logo.

### Material 3 Visual System
- Extended the reference Import-success visual language across Home, Import, Generate, Timetable Detail, and shared screen chrome.
- Added distinct semantic **PLAN / VALIDATE / OPTIMIZE** tags with separate colors.
- Increased visual hierarchy, spacing, surface treatment, and action clarity without replacing the existing scheduling architecture.
- Added formal justified alignment for longer explanatory/body copy where appropriate.
- Preserved Light, Dark, Black, and independent accent-color preferences.

### Timetable Creation / Import
- Changed the New Timetable flow so **every new timetable starts with a fresh import step** before generation.
- ZIP import now **replaces the current working dataset** instead of silently reusing a previous ZIP's data.
- Added **Clear current imported data** for explicitly removing the working dataset.
- Existing saved timetables remain intact when the working dataset is replaced or cleared.
- Retained automatic validation as part of generation.
- Preserved the post-import continuation into Generate.

### Saved Timetable Data Isolation
- Added an immutable source-data snapshot to each timetable version.
- Older timetable versions can therefore still be displayed, validated, repaired, optimized, and exported after a different ZIP is imported for a newer timetable.
- Kept legacy runs without snapshots compatible through the existing global-data fallback.

### Repair / Optimization
- Teacher availability changes now propagate into saved timetable source snapshots.
- Repair and optimization read the timetable's own source snapshot instead of accidentally switching to whichever dataset was most recently imported.
- Optimization retains the minimal-change strategy, including local moves and direct teacher/session swaps.
- Validation remains part of the optimization result path.

### Timetable Version Management
- Added delete controls to **every saved timetable version** in the timetable detail history.
- Deleting the root timetable removes its complete lineage.
- Deleting a derived Repair/Optimize version removes only that version, leaving the rest of the timetable history intact.
- Preserved the existing Home-level timetable lineage model.

### Release Metadata
- Settings/About version display updated to **Chroma Engine v1.0.1**.

This changelog records the changes made from the supplied **base ZIP (treated as Chroma Engine v0.0.0)** to the current **Chroma Engine v1.0.0** working baseline.

## [1.0.0] — Initial Stable Chroma Engine Release

### Versioning
- Established the supplied base ZIP as **v0.0.0**.
- Established this working baseline as **Chroma Engine v1.0.0**.
- Android app version name changed from `0.1.0` to `1.0.0`.
- Version information is surfaced in the drawer, Settings, and About.

### UI / Design System — Full Revamp
- Reworked shared Compose app chrome toward a modern Material 3 presentation.
- Reworked top app bars and home header.
- Reduced the visual weight of the old full-width accent header treatment and moved toward cleaner Material 3 surfaces.
- Refined typography hierarchy, spacing, cards, status presentation, and touch targets.
- Refined dashboard cards and timetable list presentation.
- Updated the design-system palette and neutral surfaces.
- Preserved user-selectable Light, Dark, and Black themes.
- Preserved the two independent accent-color preferences.
- Improved theme-aware Material 3 component usage.

### Navigation / Drawer
- Revamped the navigation drawer layout and grouping.
- Added a direct **New timetable** action to the drawer.
- Added explicit Light / Dark / Black theme selection in the drawer.
- Reorganized drawer content into scheduling, appearance, app, and support areas.
- Fixed a drawer navigation race: navigation now waits for the drawer close animation to complete before changing destinations, addressing the observed blank/frozen-screen behavior.

### Timetable Creation / Import Workflow
- Closed the import workflow dead end where imported data could leave the user without an obvious next action.
- Added an explicit post-import continuation into generation.
- Wired import completion back into the generation flow.
- Kept validation inside the generation pipeline: generated schedules are validated before being persisted.
- Kept imported data persisted locally rather than introducing an unnecessary second save step.
- Added clearer access to timetable creation from the drawer.

### Timetable Workspace
- Added a dedicated **Views** entry to the timetable workspace so generated results are directly reachable.
- Preserved access to teacher-focused timetable management.
- Moved secondary actions under a **More** action surface instead of overcrowding the bottom navigation.
- Kept export, repair, and timetable deletion accessible from the timetable workspace.
- Preserved timetable lineage/version behavior.

### Optimization / Minimal-Change Repair
- Reworked the optimization contract so the source timetable is never mutated.
- Optimization now produces a new version in the same timetable lineage.
- Added a configurable **maximum change budget**.
- Added explicit optimization outcome metrics:
  - assignments changed
  - swaps performed
  - violations before optimization
  - violations after optimization
  - quality score before
  - quality score after
- Changed optimization to prioritize hard-constraint repair before soft-quality improvement.
- Added **single-session local moves** as the first repair strategy.
- Added **direct two-teacher/two-session swaps** as a second strategy for cases where one teacher's acceptable slot is occupied by another teacher who can legally take the first teacher's slot.
- Candidate moves are checked against availability, duration, room occupancy, and complete schedule validation.
- Existing assignments are protected unless a change actually improves hard validity or, for an already-valid schedule, improves quality.
- Deterministic ordering is retained so the same input produces repeatable optimization behavior.

### Data / Engine Integration
- Extended `ScheduleRepository.optimize()` to return a structured optimization outcome instead of only a run ID.
- Passed the optimization change budget from the repository into `ScheduleOptimizer`.
- Kept the existing graph-coloring generation engine and validation engine intact as the functional foundation.
- Preserved the existing Room-backed schedule persistence and lineage model.

### Branding
- Reframed the application identity as **Chroma Timetables / Chroma Engine v1.0.0**.
- Replaced the old transparent logo treatment with the supplied **gold-background Chroma logo**.
- The gold background is intentionally preserved as part of the brand identity.
- Added generous logo safe space so the artwork is not oversized or pressed against the image edge.
- Applied the gold brand treatment to launcher assets while retaining adaptive-icon behavior.
- Added **Developed by Sir_JPagdi** to the app identity surfaces.

### About / Settings
- Added the Chroma logo to the About screen with intentional safe spacing.
- Added engine version and developer credit to About.
- Added a Chroma Engine v1.0.0 release summary to Settings.
- Kept existing algorithm and accent-color settings intact.

## Base v0.0.0 → v1.0.0 scope note

The changes above are a **presentation, workflow, navigation, branding, and optimization-layer evolution** of the supplied project. The existing core scheduling architecture remains the foundation: graph-coloring generation, deterministic hard-constraint validation, Room persistence, repair, export, and the existing multi-module Compose structure were retained rather than replaced wholesale.
