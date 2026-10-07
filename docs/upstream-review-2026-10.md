# Upstream tracking review (7 October 2026)

This repository descends from `traccar/traccar-client-android`. Its last common
commit with that repository is `b17a81b`; the old Android branch has 35 later
commits. Traccar has stopped development there and moved its active client to
`traccar/traccar-client`, a Flutter application backed by a separate tracking
SDK. The two clients cannot be merged as source trees.

## Adopted

| Source | Adaptation for Waypoint Racing |
| --- | --- |
| Old Android client status widget (`2667f61` through `6481787`) | A small home screen widget shows whether tracking was started or stopped and opens the racing app. It listens to the existing package-scoped service broadcasts and reads the saved tracking state when first added. It does not start or stop tracking itself. |
| Active client log export (`ead1dab`) | The Status screen shares its current messages as a UTF-8 text file using the existing `FileProvider`. The export is initiated by the user and grants the recipient read access to that one file. |

## Already covered in this client

- The old client's foreground-service location permission, explicit service
  broadcasts, and handling of runtime startup failures are already present.
- More recent local work also checks notification visibility, recovers stalled
  location requests, and handles screen-off GPS limits. Replacing those paths
  with the old upstream service would remove racing-specific safeguards.

## Not ported

- The active client's heartbeat-location and SOS features use Traccar's
  separate SDK and server protocol. Waypoint Racing records Firebase event
  sessions, gate passages, and position uploads with different semantics. These
  features need a separate product and backend design before integration.
- MDM-managed settings target a different deployment model and would require
  a policy for which racing settings an administrator may override.
- Upstream dependency, SDK, and review-library bumps do not map cleanly to the
  current Android project, which already targets Android API 36 and uses its
  own Gradle and Firebase configuration.

## Fork relationship

The old upstream is archived in practice, while the active client has a
different language, architecture, and backend. Treat upstream as a reference
for targeted ideas and consider detaching this repository after any open pull
requests are merged. GitHub's *Leave fork network* action is permanent and can
discard repository metadata including pull requests and issues; check its
eligibility and effects in GitHub's current documentation before using it.
Keep attribution to Traccar and the Apache 2.0 license after detaching.
