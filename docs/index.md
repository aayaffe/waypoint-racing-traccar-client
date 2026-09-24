---
title: Waypoint Racing Privacy Policy
---

# Waypoint Racing Privacy Policy

Last updated: September 24, 2026

Waypoint Racing is an Android app for waypoint sailing events. It displays routes and the next waypoint or gate, records passage through race marks, and stores tracking information in the app's main Firebase backend. In this policy, "we" means the operator of Waypoint Racing, reachable at the contact address below. This policy explains the data handled by the app and the choices available to you.

## Data the app handles

- **Location and device status.** While tracking is active, the app obtains location updates, including coordinates, time, speed, course, altitude, and accuracy. It also records a device identifier chosen or generated in the app, boat name, battery level, charging status, and whether a location was marked as simulated. Tracking runs through an Android foreground service and can continue when the app is not on screen. A persistent notification indicates that the service is running.
- **Race information.** The app uses route, event, waypoint, and gate details to determine when a waypoint or gate has been passed. A passing report can contain the event and route identifiers, gate name and identifier, passing time and position, boat name, device identifier, signed-in user identifier, battery level, simulated-location status, and app version. Users can also submit a manual passing report.
- **Account and boat information.** Google sign-in is used for race reporting. Firebase Authentication processes account information associated with sign-in, such as your Google account email address and Firebase user identifier. The app stores boat details you provide, which can include a boat name, sail number, and skipper name, in Firebase Cloud Firestore.
- **App activity and diagnostics.** The app uses Google Analytics for Firebase, Firebase Crashlytics, and Firebase Remote Config. These services may process app usage events, crash and diagnostic information, device and app information, and app or installation identifiers. The app specifically logs gate-passing events and records tracking start, tracking stop, and route reset events associated with a signed-in user identifier.
- **Map data.** When you open the map, the app uses Mapbox to load map content and display your position and route. Mapbox may receive technical information needed to deliver the map, such as network and device information. See [Mapbox's privacy policy](https://www.mapbox.com/legal/privacy).

## How data is used and shared

The app uses location to show your position, guide you along a route, detect and record waypoint or gate passings, and provide live tracking during an event. It uses account and boat information to associate reports with a participant and make race reports available to the event's organizers or race committee.

The app stores passing reports, boat details, and certain app events in its main Firebase backend for race operations. Tracking information is stored in that backend. A remotely controlled setting governs whether periodic position records are uploaded to Firebase Cloud Firestore while location updates are being received; that setting is off by default in the app. The app also keeps passing history on the device. Google processes data for authentication, storage, analytics, crash reporting, and configuration under its applicable terms and [Firebase privacy information](https://firebase.google.com/support/privacy).

We do not use your location to serve advertising.

## Your choices

You can stop tracking in the app to stop its tracking service and location updates. You can deny or revoke location permission in Android settings, but location-based features will then be unavailable. You can sign out of your Google account in the app.

## Storage and deletion

The app stores settings, route information, and passing history on your device. Local passing history can be reset in the app; uninstalling the app removes its local app data. Firebase records may remain after local data is cleared or the app is uninstalled.

To request deletion of your Waypoint Racing account and associated app data, email [avimarineinnovations@gmail.com](mailto:avimarineinnovations@gmail.com) with the subject **Account Deletion Request** and include the email address used to sign in. We will send a verification email to that address and delete the account and associated app data within 30 days of receiving your request, after verification, as described in the [account deletion instructions](account-deletion.html). Deletion is irreversible.

We have not specified a fixed retention period for Firebase records. Contact us for information about records associated with your account and deletion of those records.

## Security

The app disables Android backup of its app data. Firebase connections use HTTPS. No transmission or storage method can be guaranteed completely secure.

## Changes and contact

We may update this policy when the app or its data practices change. The date above identifies the latest version. For privacy questions or requests, contact [avimarineinnovations@gmail.com](mailto:avimarineinnovations@gmail.com).
