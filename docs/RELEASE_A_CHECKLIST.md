# Release A implementation checklist

Progress is measured by the twelve independently verifiable Release A work items below. This file is updated with each implementation slice. The compact table is historical; use **Handoff: remaining Release A work** as the authoritative list of unfinished items.

| # | Work item | Status |
|---|---|---|
| 1 | Establish testable Race Deck presentation layer and seconds-required time rules | Complete |
| 2 | Apply sunlight-first tokens and instrument-first compact portrait layout | Complete; final accessibility review pending |
| 3 | Add responsive landscape and tablet Race Deck layouts | Complete in code; device review pending |
| 4 | Present marks, gates, and finishes with the correct primary and endpoint values | Complete |
| 5 | Add fixed COG/SOG/VMG and TTG/ETA/TIME instruments plus always-visible position | Complete |
| 6 | Make GPS, tracking, sync, and device-readiness states independent and accessible | Complete in code; TalkBack/device review pending |
| 7 | Add consolidated Device Setup panel and Android-setting actions | Complete |
| 8 | Build one-tap Targets list and map confirmation flow | Complete in code; interaction smoke review pending |
| 9 | Make the map secondary, north-up, and auto-framed with explicit re-center behavior | Complete in code; device review pending |
| 10 | Complete non-blocking pass feedback and persistent tracking notification | Complete in code; lifecycle smoke review pending |
| 11 | Stabilize route restore/selection and recovery states without changing adapters or contracts | Complete in code; recovery smoke review pending |
| 12 | Verify legacy pass/upload parity, lifecycle recovery, and responsive/accessibility acceptance | In progress; device/backend evidence outstanding |

## Current slice

- [x] Created branch `codex/release-a-android-design`.
- [x] Added a pure, unit-tested formatter for Race Deck clock, ETA, and pass-event presentation. The current time and ETA now use `HH:mm:ss`.
- [x] Added the detected pass event time and latitude/longitude to the existing non-blocking pass banner.
- [x] Connect navigation, endpoint, position, warning, and pass presentation to the immutable Race Deck UI state and bind the primary layout to it.
- [x] Preserve primary target, endpoint, position, and actions while expanding the portrait map; the map takes the remaining measured space.
- [x] Apply day/night semantic colors to warning and pass surfaces and replace pixel-based dialog spacing with density-aware tokens.
- [x] Require an explicit, target-type-specific `SELECT MARK` / `SELECT GATE` / `SELECT FINISH` confirmation on map selection.
- [x] Replace long-press-only map selection with point/line tap selection, temporary target highlighting, and confirmation-only selection commits.
- [x] Recompute active-target navigation values from the latest location immediately when a target changes, without refreshing GPS freshness or pass detection.
- [x] Re-apply the selected target when a route finishes loading, even when the saved index is unchanged and emits no preference event.
- [x] Keep `Restoring navigation…` visible during asynchronous URL route loads, then dismiss it on success or failure.
- [x] Render all pass-banner content from state, including finish/mark title and icon, next target, exact event position/time, and upload status.
- [x] Make the pass-banner dismiss target at least 56dp.
- [x] Drive Device Setup, Tracking Off, and GPS warning visibility/content from `RaceDeckUiState`.
- [x] Bind localized GPS, Tracking, Sync, and Device status labels from `RaceDeckUiState`.
- [x] Bind active-target name/type labels and TalkBack description from the target presentation state.
- [x] Add Robolectric layout binding for navigation and pass content, light/dark palette checks, 1.3× font-scale bounds, and expanded/responsive-map policy coverage.
- [x] Keep the tablet Gate/Finish endpoint card content-sized so position and all six navigation metrics remain visible; the 1280×800 dp, 1.3× font-scale regression test passes.
- [x] Characterize local pass persistence from the legacy `test_wptracing.json` course fixture, including telemetry, route/session fields, duplicate replacement, and pending-to-uploaded status.
- [x] Give the active target's primary bearing and distance rows intrinsic height; the measurement regression now asserts both values are visible at 1.3× font scale, and the phone build matches the review-board hierarchy.
- [x] Render unavailable ETA as the specified em dash while retaining `HH:mm:ss` for available arrival times; verified on the phone and covered by formatter tests.
- [x] Keep MAP visually neutral by default; it remains an action in the always-visible split layout, not a selected tab.
- [x] Fit the full landscape Race Deck at 1.3× font scale by moving the course/boat/clock into one header row and compressing secondary content while preserving 36sp primary target values; verified on SM-G998B.
- [x] Reposition the normal portrait map below measured navigation content when a Device/GPS warning expands the header stack; the connected phone retained the full TTG/ETA/TIME row under the notification-off warning.
- [x] Apply a distinct critical Device treatment and `DEVICE ✕` label when background tracking is Restricted; verified against both Optimized and Restricted Android settings on SM-G998B.
- [x] Place the map after the navigation pane and before bottom actions in the accessibility traversal order; a layout test guards these boundaries. Physical TalkBack reading order is still pending.

**Release A status: implementation complete; acceptance incomplete.** Code-level client work is complete. Physical-device, recorded-race parity, accessibility, and shared-backend release gates remain open as itemized below.

See [`RELEASE_A_GAP_REPORT.md`](RELEASE_A_GAP_REPORT.md) for the current separation of implementation/configuration gaps from missing acceptance evidence.

## Complete delivery plan

### Foundations and compatibility

- [x] Create the dedicated Release A branch.
- [x] Add pure, unit-tested formatting for Race Deck time and pass details.
- [x] Extract target, health, time/pass formatting, readiness, map-selection, and responsive-layout presentation mappers/policies; bind their results through the single `RaceDeckUiState`. Android resource lookup and legacy navigation calculations remain in the Activity/domain layer.
- [x] Define Race Deck UI actions separately from individual widgets and route them through one dispatcher.
- [x] Preserve legacy course format, selected-target pass eligibility, persisted events, and Firebase write paths; `TrackingController`, `FirestoreDatabase`, `GatePassing`, `GatePassings`, `Route`, and `Position` are byte-for-byte unchanged from `origin/master`.
- [~] Add regression fixtures proving legacy pass and upload parity before and after the UI update. A deterministic local pass-persistence fixture now protects the legacy record shape/status; a recorded track with expected pass set/uploads is still needed for end-to-end parity.

### Design system and accessibility

- [x] Add Release A sunlight palette resources.
- [x] Apply sunlight as the default and retain an equivalent dark palette for dusk/night use.
- [x] Apply 4dp spacing, panel, divider, radius, typography, and touch-target tokens across Race Deck, Targets, Device Setup, and pass feedback.
- [x] Apply dominant bearing/distance typography and tabular-friendly values; critical numeric instruments request tabular figures. Physical visual review remains.
- [x] Ensure each critical status uses text/icon/shape as well as color.
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

- [x] Display GPS, Tracking, Sync, and Device states independently, including notification and app/system battery modes.
- [x] Add persistent Tracking Off warning without changing service lifetime.
- [x] Add immediate GPS-loss state, one-time prolonged alert, silence action, and quiet recovery.
- [x] Display Sync offline/pending visually only.
- [x] Detect notification-disabled state and link to Android notification settings.
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
- [~] Add UI tests for state-to-layout binding, warning visibility/content, pass title/next/upload state, 56dp dismiss and target-row sizing, map confirmation labels, responsive thresholds, expanded-map sizing, dark palette resources, and 1.3× text-line bounds. Warning/pass-banner interaction and live phone/tablet flows still need runtime coverage.
- [x] Run local route, pass, tracking, recovery, protocol, database, and Race Deck regression suites; shared-backend validation remains below.
- [~] Device smoke-test screen on/off, process death, recreation, crash recovery, reboot, battery saver, notification denial, offline network, and restored route. See `RELEASE_A_DEVICE_SMOKE_TEST.md`.
- [ ] Compare a recorded legacy race’s pass set and uploads before/after modernization.
- [ ] Verify production and Release A clients operate concurrently and backend/rules rollback is safe.
- [ ] Complete visual and accessibility review against the Release A review board.

## Handoff: remaining Release A work

This section is the authoritative handoff backlog. Do not mark Release A complete until every item below has direct evidence.

### A. Finish / verify code-level presentation work

- [x] **Complete `RaceDeckUiState` migration.** The primary Race Deck view now binds course/boat header, target name/type/accessibility text, navigation values, endpoints, position, localized GPS/Tracking/Sync/Device labels, device/tracking/GPS warnings, and full pass-banner content (type, title, location/time, next target, and upload status) from the immutable UI state. Existing location calculations and detector behavior remain in the legacy presentation/domain layer.
- [x] **Apply common layout tokens consistently.** Shared spacing, panel, typography, and 56dp control tokens cover Race Deck, Targets, Device Setup, and pass feedback; secondary legacy screens are outside the Release A cockpit scope.
- [x] **Finish compact-portrait compression behavior in code.** The map is hidden below 800dp. Expanded mode measures critical target/endpoints/position content and allocates remaining height to the map. Physical size/font-scale review remains below.
- [~] **Validate dark mode end-to-end.** Physical phone checks cover Race Deck, Targets, map confirmation dialog, and Device Setup warning/panel; Robolectric verifies dark pass/target palette resources. Pass banner and remaining popup surfaces still need runtime verification.
- [ ] **Accessibility polish.** Code labels and touch targets are present, including target-row descriptions; TalkBack order and 1.3× font-scale clipping still need device verification.
- [~] **Add/finish UI tests.** Added Robolectric layout-binding/touch-size tests, including tablet endpoint sizing and measured active-target values at 1.3× font scale, and pure expanded-map sizing tests. Interaction tests for warnings, re-center, dismissal, and some responsive variants remain.

### B. Functional regression verification

- [x] **Run full local Android unit suite.** Latest `:app:testRegularDebugUnitTest` passed: 74 tests, 0 failures, 1 skipped. Gradle needed the approved cache/process permission retry after the sandbox denied the existing distribution lock.
- [~] **Legacy parity fixture/test.** Local GatePass serialization and upload-status persistence are characterized against the legacy course fixture. A recorded legacy track and its expected pass/upload set are still needed to prove end-to-end parity. Do not modify detector eligibility or persisted event shapes.
- [~] **Route recovery tests.** Existing route-selection and location-recovery unit suites pass; full Activity restore/failure/update flows still need runtime smoke verification.
- [~] **Tracking/notification tests.** Existing service/receiver/readiness unit suites pass; notification permission and screen-off lifecycle scenarios still need device verification.

### C. Physical-device and backend release gate

- [ ] Run every scenario in [`RELEASE_A_DEVICE_SMOKE_TEST.md`](RELEASE_A_DEVICE_SMOKE_TEST.md) on a physical device, including screen off/on, process death, recreation, reboot behavior, Battery Saver, notification denial, offline operation, GPS outage, small portrait, landscape, and tablet.
- [ ] Record results against both Light and Dark system mode, including 1.3× font scale and TalkBack.
- [ ] Compare a recorded legacy race between production and Release A builds for exact pass set and upload parity.
- [ ] Verify production and Release A clients concurrently against the same backend/rules; verify rollback does not break either client.
- [ ] Perform final visual review against `waypoint-racing-shared/docs/Release A/Waypoint Racing Android App Review Board.png` and resolve deviations.

### Current build-state note

- The worktree has uncommitted Release A follow-up changes after commit `b698202` on `codex/release-a-android-design`.
- The primary changes are in `MainActivity`, `MapFragment`, Race Deck UI state/layout policy, `activity_main.xml`, target-sheet resources, semantic dimensions/strings, and unit/layout tests.
- Latest full local unit run completed successfully: 74 tests, 0 failures, 1 skipped. Robolectric checks map/target values at 1.3× font scale, phone/tablet breakpoints, neutral bottom-action styling, critical restricted-device styling, navigation/map accessibility traversal constraints, night palette resources, full pass-banner state binding, the 10-second dismissal limit, compact tablet endpoint sizing, nonzero measured target-bearing/distance rows, a visible position placeholder before the first fix, and local pass persistence/status transitions.
- `TrackingController`, `FirestoreDatabase`, `GatePassing`, `GatePassings`, `Route`, and `Position` match `origin/master`. A recorded-race pass/upload parity fixture is still not present.
- `:app:assembleRegularDebug` also completed successfully; the regular debug APK packaged and signed.
- `:app:assembleRegularDebuggableRelease` also completed successfully using `app/google-services.json` (production Firebase configuration). This artifact was not installed or run against production; the `.dev` configuration targets a different Firebase project and the repository has no separate Release A client registration for safe coexistence testing.
- Deployed the latest `.dev` regular debug build to the SM-G998B and SM-G930F. The SM-G930F (Android 8.0/API 26) currently remains connected; the SM-G998B disconnected from ADB after reboot. The tablet’s earlier build verified endpoint sizing, but it disconnected before the latest build was installed. Phone checks cover Finish and Mark presentation, Targets/map selection and restoration, compact portrait including 1.3×, expanded-map position visibility and collapse, landscape, dark Device Setup/target surfaces, and neutral MAP styling. Two passes appeared and uploaded in dev test data. After reboot, manual app launch restored Haifa Exit Test and Finish, but GPS had no fresh fix; automatic service/notification restart before app launch remains unconfirmed. Recorded-race parity remains open. Per the latest user instruction, testing is limited to `.dev` debug; production coexistence/rollback is unverified. No emulator command is installed.
- Firebase configuration inspection confirms `app/google-services.json` and `app/src/debug/google-services.json` target different Firebase projects. The installed `.dev` client therefore cannot provide same-backend production/Release A coexistence evidence; a production-configured Release A Firebase client and an approved test environment are not present in this worktree.
