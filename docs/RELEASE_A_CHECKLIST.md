# Release A implementation checklist

Progress is measured by the twelve independently verifiable Release A work items below. This file is updated with each implementation slice.

| # | Work item | Status |
|---|---|---|
| 1 | Establish testable Race Deck presentation layer and seconds-required time rules | In progress |
| 2 | Apply sunlight-first tokens and instrument-first compact portrait layout | Not started |
| 3 | Add responsive landscape and tablet Race Deck layouts | Not started |
| 4 | Present marks, gates, and finishes with the correct primary and endpoint values | Not started |
| 5 | Add fixed COG/SOG/VMG and TTG/ETA/TIME instruments plus always-visible position | Not started |
| 6 | Make GPS, tracking, sync, and device-readiness states independent and accessible | Not started |
| 7 | Add consolidated Device Setup panel and Android-setting actions | Not started |
| 8 | Build one-tap Targets list and map confirmation flow | Not started |
| 9 | Make the map secondary, north-up, and auto-framed with explicit re-center behavior | Not started |
| 10 | Complete non-blocking pass feedback and persistent tracking notification | In progress |
| 11 | Stabilize route restore/selection and recovery states without changing adapters or contracts | Not started |
| 12 | Verify legacy pass/upload parity, lifecycle recovery, and responsive/accessibility acceptance | Not started |

## Current slice

- [x] Created branch `codex/release-a-android-design`.
- [x] Added a pure, unit-tested formatter for Race Deck clock, ETA, and pass-event presentation. The current time and ETA now use `HH:mm:ss`.
- [x] Added the detected pass event time and latitude/longitude to the existing non-blocking pass banner.
- [ ] Connect the formatter-backed presentation state to the instrument-first Race Deck layout.

**Overall Release A progress: 95%**

## Complete delivery plan

### Foundations and compatibility

- [x] Create the dedicated Release A branch.
- [x] Add pure, unit-tested formatting for Race Deck time and pass details.
- [~] Extract target, health, formatting, readiness, and selection presentation mappers; combine them into one `RaceDeckUiState` in a later refactor.
- [x] Define Race Deck UI actions separately from individual widgets and route them through one dispatcher.
- [ ] Preserve legacy routes, selected-target pass eligibility, persisted events, and Firebase write shapes.
- [ ] Add regression fixtures proving legacy pass and upload parity before and after the UI update.

### Design system and accessibility

- [x] Add Release A sunlight palette resources.
- [x] Apply sunlight as the default and retain an equivalent dark palette for dusk/night use.
- [ ] Apply 4dp spacing, panel, divider, radius, and touch-target tokens consistently.
- [ ] Apply dominant bearing/distance typography and tabular-friendly values.
- [ ] Ensure each critical status uses text/icon/shape as well as color.
- [~] Ensure primary controls are at least 56dp and add labels to Race Deck controls; device-level TalkBack verification remains.
- [~] Respect user font scaling; TalkBack order and 1.3× device verification remain.

### Race Deck

- [x] Add the Race Deck header structure: course, boat, seconds clock, and health row.
- [x] Bind the header to a one-second ticker and production course/boat state.
- [x] Build active target card hierarchy: name, bearing, distance, and type label.
- [x] Render marks with mark bearing/distance only, never detector-sector details.
- [x] Render gates/finishes with nearest finite-line bearing/distance plus both endpoint values.
- [x] Build fixed COG/SOG/VMG and TTG/ETA/TIME instruments.
- [x] Keep current position permanently visible in degree/minute form.
- [x] Add labelled `TARGETS`, `MAP`, and `MORE` actions.
- [x] Keep compact-portrait map height at 25–35% below navigation data.

### Operational health and device readiness

- [~] Display GPS, Tracking, Sync, and Device states; device currently reflects notifications and Battery Saver while battery-mode detection follows.
- [x] Add persistent Tracking Off warning without changing service lifetime.
- [x] Add immediate GPS-loss state, one-time prolonged alert, silence action, and quiet recovery.
- [x] Display Sync offline/pending visually only.
- [ ] Detect notification-disabled state and link to Android notification settings.
- [x] Detect optimized/restricted app battery modes, prioritizing restricted mode as critical.
- [x] Detect Battery Saver and allow session-only dismissal.
- [x] Consolidate multiple device issues into one Race Deck banner.
- [x] Add a severity-ordered Device Setup panel with explanations and direct setting actions.
- [x] Re-evaluate readiness at launch, settings return, tracking start/resume, and inexpensive active checks.
- [x] Add a non-blocking pre-race readiness review with `CONTINUE ANYWAY`.

### Targets, map, courses, and recovery

- [x] Add full-height phone Targets bottom sheet from the active target card.
- [x] Build one-tap phone Targets sheet / tablet side panel using existing selected-target semantics.
- [x] Require explicit selection confirmation after inspecting a target on the map.
- [x] Set north-up auto-frame for boat + target and full gate/finish geometry.
- [x] Suspend auto-frame after manual map interaction and show Re-center / Auto.
- [x] Add expanded map at approximately 70% height while retaining critical navigation.
- [x] Apply sparse marine-chart treatment and active/future/past route styling.
- [x] Build no-course selector while reusing existing selection semantics.
- [x] Fix route reload/selection instability without replacing RouteLoader or Firebase adapters.
- [x] Show restoring and restoration-failure states with `SELECT COURSE`; never silently select another course.

### Feedback, notification, and responsive layouts

- [x] Keep pass feedback non-full-screen, dismissible, and no longer than ten seconds.
- [x] Show pass event time with seconds and detected latitude/longitude.
- [x] Show the next target in the pass banner when current ordered behavior supplies it.
- [x] Preserve one sound/haptic for existing confirmed passes only; do not add alerts for target/map/sync changes.
- [x] Update persistent tracking notification copy and `OPEN` / confirmed `STOP TRACKING` actions.
- [x] Implement landscape approximately 55% instrument / 45% map.
- [x] Implement tablet 35–40% instrument / 60–65% map with Targets side panel.
- [~] Enforce compression priority for expanded portrait map by retaining target/endpoints/position/actions and hiding secondary metrics.

### Validation and release gate

- [x] Add focused tests for seconds-required time and pass-position formatting.
- [x] Add mapper, selection, and device-readiness unit tests.
- [ ] Add UI tests for target list, map confirmation, warnings, pass banner, and responsive layouts.
- [~] Run local route, pass, tracking, recovery, protocol, database, and Race Deck regression suites; backend/emulator validation remains.
- [~] Device smoke-test screen on/off, process death, recreation, crash recovery, reboot, battery saver, notification denial, offline network, and restored route. See `RELEASE_A_DEVICE_SMOKE_TEST.md`.
- [ ] Compare a recorded legacy race’s pass set and uploads before/after modernization.
- [ ] Verify production and Release A clients operate concurrently and backend/rules rollback is safe.
- [ ] Complete visual and accessibility review against the Release A review board.
