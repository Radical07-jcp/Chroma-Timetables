# Mixed XML + Jetpack Compose — why, and where the line is

This app is intentionally **not** all-Compose and **not** all-Views. Per explicit request, it mirrors
the reference app's own architecture (plain Activities + XML layouts, DrawerLayout nav) for the
screens that are primarily *navigation and lists*, while keeping the screens that are primarily
*forms and data display* in Jetpack Compose, where they were already built out across earlier phases.

## The line

**XML / Views** (`home/` package):
- `HomeActivity` — launcher, drawer, timetable list.
- `TimetableDetailActivity` — one run's info + the migrated action buttons.
- `drawer_content.xml` — shared by both via `DrawerLayout`.

**Jetpack Compose** (`ui/` package, unchanged from earlier phases except a couple of added
`onBack` params): Import, Generate, Validate, Repair, Results, Export, Teachers, Teacher
Availability, Define Periods, Settings.

## The bridge

`host/ComposeHostActivity.kt` is the *only* file that knows both sides exist. It takes a
`HostScreen` sealed class (encoded into an `Intent`'s extras) and renders exactly one Compose
screen, wired so that screen's own `onBack`/callback lambdas either `finish()` this Activity or
`start()` the next `HostScreen`. Nothing about the Compose screens' own internals needed to change
to support this — they already took `onBack` and typed callbacks as constructor parameters, which
is what made them host-able one-per-Activity in the first place.

## Why not one or the other

- All-Compose would have meant throwing away the explicit request to mirror the reference app's
  drawer/list/detail structure.
- All-XML would have meant reimplementing five already-working, non-trivial screens (CSV import
  with validation-error lists, the constraint validator's violation list, the five-tab results
  view, three export formats) as Views, for no functional benefit.

If a future screen is added, the deciding question is the same one that put every existing screen
on its current side: is this screen mostly "which of these things do I tap into next" (→ XML,
`home/`), or mostly "here's a form / a table / a report" (→ Compose, hosted via
`ComposeHostActivity`)?
