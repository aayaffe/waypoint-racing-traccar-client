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

**Overall Release A progress: 5%**
