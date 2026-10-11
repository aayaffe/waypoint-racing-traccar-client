# Release A device smoke test

Run this checklist on a physical Android device before Release A is promoted.

## Race Deck and targets

- [x] On the connected SM-G998B phone, select Haifa West Point (Mark) and verify the active-target card shows its name, bearing, and distance without gate endpoint rows; restored Polynom Finish Line afterward (2026-10-11).
- [x] On the connected SM-G998B phone, verify Finish shows nearest-on-line bearing/distance plus both endpoint values (Release A debug build, 2026-10-11).
- [x] On the connected SM-G998B phone, verify TIME includes seconds and unavailable ETA displays `—`; formatter tests cover seconds on available ETA (Release A debug build, 2026-10-11).
- [x] On the connected SM-G998B phone, verify the current position row (`POS —` before a fresh fix) remains visible above the expanded map; normal position values were visible earlier on the same device (2026-10-11).
- [x] On the connected SM-G998B phone, verify Targets opens full-height; one tap on Shavit Gate changes the active target and recalculates nearest-on-gate bearing/distance and both endpoint values from the live fix. Restored Polynom Finish Line afterward (2026-10-11).
- [x] On the connected SM-G998B phone, verify Targets opens with the current finish selected and one tap on that row closes while preserving it (Release A debug build, 2026-10-11).
- [ ] Verify tablet Targets opens as a right-side panel and selects with one tap.
- [x] On the connected SM-G998B phone, tap the Finish endpoint and verify the preview dialog offers `SELECT FINISH`; Cancel leaves the active target unchanged (Release A debug build, 2026-10-11).
- [x] On the connected SM-G998B `.dev` build, tap the Haifa West Point map marker, verify the preview asks to select it as the active target, confirm `SELECT MARK`, and verify the Mark becomes active. Restored Polynom Finish Line from Targets afterward (2026-10-11).
- [~] Observed Haifa West Point and Polynom Finish Line pass feedback on the connected SM-G998B `.dev` build. Both showed event time, coordinates, and uploaded status in the banner/Targets; Finish also showed Saved on device and Uploaded to server. The banners were gone by later screenshots, but their exact ten-second timing and immediate dismiss were not measured. The device was silent, so sound/haptic and Mark next-target presentation remain unverified. Both events remain in dev test data (2026-10-11).

## Map and layouts

- [x] On the connected SM-G998B phone, verify compact portrait keeps endpoints, position, six metrics, and actions visible above the 25–35% map (Release A debug build, 2026-10-11).
- [x] On the connected SM-G998B phone at a temporary 720×1100 display override with density 300 (below 800dp), verify the map and duplicate target selector are hidden while target values, endpoints, position, all six metrics, and bottom actions remain visible at 1.3× font scale. Restore the prior display/font/rotation settings (2026-10-11).
- [x] On the connected SM-G930F running Android 8.0/API 26, launch the latest `.dev` build and verify compact portrait hides the map while showing target, endpoints, position, all six metrics, and actions (2026-10-11).
- [x] On the connected SM-G998B phone, verify MAP expands to a larger map while retaining the active target, endpoints, position, and bottom actions; tapping COLLAPSE restores the normal map and six metrics (2026-10-11).
- [x] On the connected SM-G998B phone, verify a map tap exposes RE-CENTER; tapping RE-CENTER hides the button and returns to the target frame (Release A debug build, 2026-10-11).
- [x] On the connected SM-G998B phone in landscape, pan the map, tap RE-CENTER, and verify the boat plus both Finish endpoints are visible in the target frame (2026-10-11).
- [x] On the connected SM-G998B phone, verify landscape target/endpoints/position/six metrics remain visible above neutral bottom actions, with MAP unselected (Release A debug build, 2026-10-11).
- [x] Repeat the landscape visibility check at 1.3× font scale; primary values, endpoints, position, both metrics rows, and actions remain on screen (SM-G998B, 2026-10-11).
- [x] At 1.3× in portrait, verify target, endpoints, position, all six metrics, and bottom actions remain visible; the map yields height to the larger navigation stack (SM-G998B, 2026-10-11).
- [x] On the connected SM-X730 tablet, verify Gate endpoints stay compact and leave position plus COG/SOG/VMG and TTG/ETA/TIME visible (Release A debug build, 2026-10-11).
- [ ] Verify TalkBack order follows visual priority.
- [x] On the connected SM-G998B phone, verify Race Deck target, endpoints, position, metrics, and neutral actions remain readable in Dark mode (Release A debug build, 2026-10-11).
- [x] In Dark mode on the SM-G998B, verify Targets sheet and the `SELECT FINISH` map confirmation dialog remain readable; cancel restores the same active target (2026-10-11).
- [x] In Dark mode on the SM-G998B, verify the consolidated Battery Saver warning and Device Setup panel text/actions remain readable; restore Battery Saver off and Light mode afterward (2026-10-11).
- [ ] Verify pass-banner and remaining popup surfaces in Dark mode.

## Reliability and device readiness

- [x] On the connected SM-G998B, revoke notifications; verify the DEVICE warning, Device Setup details, and ENABLE NOTIFICATIONS opens Android App notifications settings. Grant permission again and verify DEVICE returns healthy (2026-10-11).
- [x] With the notification warning shown, verify the map yields space to keep target, endpoints, position, and TTG/ETA/TIME visible (SM-G998B, 2026-10-11).
- [x] On the connected SM-G998B, enable Battery Saver; verify DEVICE warning, Battery Saver details, and session-only Dismiss hides the banner while DEVICE remains degraded. Battery Saver was turned back off (2026-10-11).
- [x] On the connected SM-G998B, set app battery mode to Optimized; verify the `Battery optimization may interrupt tracking` warning and FIX opens App info. Restore Unrestricted afterward (2026-10-11).
- [x] On the connected SM-G998B, set app battery mode to Restricted; verify `DEVICE ✕` and the critical red `BACKGROUND TRACKING RESTRICTED` banner. Restore Unrestricted afterward (2026-10-11).
- [x] On the connected SM-G998B, disabling Location showed GPS LOST, a live last-valid-fix age, and the Silence GPS Alerts action; enabling Location restored GPS quietly (2026-10-11).
- [~] On the connected SM-G998B phone, keep GPS unavailable for over two minutes; the warning age reached 132 seconds. SILENCE removed its control, and it remained hidden at 256 seconds. The phone was in silent mode, so the audible/haptic alert firing could not be confirmed; Location was restored afterward (2026-10-11).
- [x] On the connected SM-G998B, disable Wi-Fi/mobile data and verify amber `SYNC ○ OFFLINE`; restore connectivity and verify green `SYNC ●` returns without sound. No pending-upload state was induced (2026-10-11).
- [ ] Dismiss the tracking notification while foregrounded, then background the app; verify it returns.
- [x] On the connected SM-G998B, invoke the notification's `CONFIRM_STOP_TRACKING` action and verify the Stop tracking confirmation appears; Cancel leaves TRACKING active (2026-10-11).

## Lifecycle and compatibility

- [x] Screen off/on while tracking on the connected SM-G998B; the Race Deck returned with the selected route and navigation values intact (Release A debug build, 2026-10-11).
- [x] Update/restart with `adb install -r` and relaunch; Haifa Exit Test restored and TRACKING remained active (SM-G998B, 2026-10-11).
- [ ] Process-death recovery remains unverified. `am kill` left the `.dev` process alive while its foreground tracking service was active; the app was brought back to the foreground with Haifa Exit Test and Finish intact. The service was not force-stopped (2026-10-11).
- [~] Rebooted the SM-G998B with the `.dev` build installed. After boot, manually launching the app restored Haifa Exit Test and Finish; the UI showed TRACKING active and the TrackingService was running after launch. GPS was LOST with no fresh fix, and automatic foreground-service/notification recovery before manually opening the app was not confirmed. The SM-G998B later disconnected from ADB (2026-10-11).
- [ ] Offline network and restored route.
- [ ] Run a recorded legacy race and compare its passes/uploads against the production client.
- [ ] Confirm production and Release A clients work against the same backend before any rollout.
