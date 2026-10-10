# Release A device smoke test

Run this checklist on a physical Android device before Release A is promoted.

## Race Deck and targets

- [ ] Verify Mark view shows only its name, bearing, and distance.
- [ ] Verify Gate and Finish show nearest-on-line values plus both endpoint values.
- [ ] Verify TIME and ETA include seconds.
- [ ] Verify current position remains visible in normal and expanded-map modes.
- [ ] Verify phone Targets opens full-height and selects with one tap.
- [ ] Verify tablet Targets opens as a right-side panel and selects with one tap.
- [ ] Verify map target selection needs explicit confirmation.

## Map and layouts

- [ ] Verify compact portrait keeps navigation above a 25–35% map.
- [ ] Verify expanded map can collapse back to normal.
- [ ] Verify manual map interaction exposes RE-CENTER and stops auto-framing.
- [ ] Verify RE-CENTER frames boat plus active target, including both gate endpoints.
- [ ] Verify landscape phone and tablet instrument/map splits are field-readable.

## Reliability and device readiness

- [ ] Deny notifications; verify DEVICE warning and direct Settings action.
- [ ] Enable Battery Saver; verify DEVICE warning, detail panel, and session-only Dismiss behavior.
- [ ] Set app battery mode to Optimized and Restricted; verify warning severity and Settings action.
- [ ] Turn GPS off; verify live last-valid-fix age, one prolonged alert, Silence action, and quiet recovery.
- [ ] Toggle connectivity; verify Sync changes between OFFLINE, PENDING, and healthy without sound.
- [ ] Dismiss the tracking notification while foregrounded, then background the app; verify it returns.
- [ ] Stop tracking from notification; verify confirmation before recording stops.

## Lifecycle and compatibility

- [ ] Screen off/on while tracking.
- [ ] Activity recreation and process death with a restorable route.
- [ ] Reboot behavior currently supported by the production service.
- [ ] Offline network and restored route.
- [ ] Run a recorded legacy race and compare its passes/uploads against the production client.
- [ ] Confirm production and Release A clients work against the same backend before any rollout.
