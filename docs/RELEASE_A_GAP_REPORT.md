# Release A gap report

**Audit date:** 2026-10-11  
**Status:** Not ready for release. Core Race Deck implementation is present, but required parity, accessibility, lifecycle, and device acceptance evidence is incomplete.

This report separates missing implementation/configuration from missing verification. An unchecked verification item is not, by itself, evidence that the feature is absent.

## Scope and current evidence

- The latest installed and tested build is the `.dev` regular debug variant. The latest user instruction limits testing to this variant; production backend testing was not performed.
- The SM-G998B was the primary device; after the reboot check it disconnected from ADB. The latest debug build was also installed/launched on an SM-G930F running Android 8.0/API 26, which is currently the connected device.
- `:app:testRegularDebugUnitTest` passed **74 tests, 0 failures, 1 skipped**. `:app:assembleRegularDebug` succeeded.
- A follow-up compact-portrait check found metric-row clipping below 800dp. The `.dev` layout now hides the duplicate target selector, compacts the header and vertical gaps, and preserves all six metrics; phone verification passed at 1.3× font scale using a temporary 720×1100 display override at density 300. Original display/font/rotation settings were restored.
- On the SM-G998B, Race Deck target presentation, Targets selection, positive map confirmation, expanded-map position visibility/collapse, landscape layout, dark-mode Device Setup, and RE-CENTER framing were exercised. Finish is restored as the active target; Location is enabled and Battery Saver is off.
- Two pass events (Haifa West Point and Polynom Finish Line) appeared and uploaded during `.dev` device testing. They remain in dev test data; no cleanup was performed.
- The latest worktree has not been committed. See [`RELEASE_A_CHECKLIST.md`](RELEASE_A_CHECKLIST.md) and [`RELEASE_A_DEVICE_SMOKE_TEST.md`](RELEASE_A_DEVICE_SMOKE_TEST.md) for the detailed evidence ledger.

## Missing implementation or release configuration

1. **No known user-visible feature remains unimplemented in the code-level scope.** Target, health, time/pass, readiness, map-selection, and responsive-layout mapping/policies now feed the single `RaceDeckUiState`. Android resource lookup and existing navigation calculations remain in their intended Activity/domain paths; pass detection and persisted event contracts are unchanged.
2. **A production-registered Release A client configuration is absent.** `app/google-services.json` registers `in.avimarine.waypointracing`; the `.dev` debug client is `in.avimarine.waypointracing.dev` and targets a different Firebase project. There is no separate Release A production Firebase client registration/configuration in this checkout for side-by-side coexistence.

## Missing tests and release validation

### Race/pass compatibility

- A recorded legacy race file and its expected pass/upload set are not present in the repository. Local persistence tests cover serialization and upload-status transitions, but do not prove detector output or exact before/after pass parity.
- Same-backend production/Release A coexistence and backend/rules rollback have not been tested. Per the latest instruction, production testing is outside the current test scope.
- Pending-upload/offline replay parity has not been exercised; the offline UI check did not induce a pending upload.

### Physical-device, accessibility, and visual acceptance

- The tablet disconnected before the latest build was installed. Verify the current build’s tablet split and one-tap Targets side panel.
- Physical TalkBack traversal/read order has not been verified. Font-scale layout has been visually checked on the phone, but TalkBack and its spoken priority remain open.
- Dark-mode Race Deck, Targets, map-selection dialog, and Device Setup warning/panel were checked. Dark pass-banner and remaining popup surfaces remain unchecked.
- Finish the full review-board comparison and record and resolve any remaining visual deviations. The MAP action is intentionally neutral per the user’s correction, even though the original board highlights it.

### Pass feedback and device behavior

- Runtime pass feedback showed event time, coordinates, and uploaded state; the Finish event also showed saved-on-device state. Exact 10-second dismissal, immediate dismissal, next-target content for a Mark/Gate, and pass sound/haptic remain unverified. The phone was in silent mode during these checks.
- GPS-loss timing and the Silence control were exercised: the warning age exceeded two minutes, Silence hid its control through the later check, and Location was restored. Audible/haptic firing was not confirmed because the device was silent.
- Dismissing the tracking notification while foregrounded and verifying its return after backgrounding remains unchecked.
- Screen off/on and update/restart route restoration were exercised. After reboot, manually launching `.dev` restored Haifa Exit Test and Finish; the UI showed TRACKING active and TrackingService was running after launch, but GPS had no fresh fix. Automatic service/notification restoration before opening the app remains unconfirmed. `am kill` did not terminate the `.dev` process while its foreground tracking service was active, so process-death recovery remains unchecked; it was not force-stopped. Activity recreation/failure flows, crash recovery, and route restoration while offline also remain unchecked.
- The current phone covered normal portrait, compact portrait below 800dp at 1.3×, expanded portrait, and landscape. Complete remaining responsive/tablet variants and repeat the full matrix on the latest build where the existing evidence predates it.

## Release decision

Keep Release A in **not-ready** status until the missing recorded-race parity evidence and required device/accessibility/lifecycle checks are complete. The production coexistence/rollback gate remains explicitly unverified under the current dev/debug-only testing instruction.
