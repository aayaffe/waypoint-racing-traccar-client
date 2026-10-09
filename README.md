# Waypoint Racing ([based on Traccar](https://www.traccar.org/))

Available to selected tester only:

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="200" alt="Get it on Google Play"/>](https://play.google.com/store/apps/details?id=in.avimarine.waypointracing)


## Overview

Waypoint Racing is an application used in waypoint sailing events.
The application is based on the [Traccar Client for Android](https://github.com/traccar/traccar-client-android).
The application shows the next racing waypoint or gate and logs passage through those gates.
Tracking and race information is stored in the application's main Firebase backend.

See the [Privacy Policy](docs/index.md) for information about location tracking, race reports, and data deletion.

## Build and install

### Configure Mapbox for local development

The app needs a Mapbox **public** access token (one that starts with `pk.`) to display its map. Create a token with the public `STYLES:TILES`, `STYLES:READ`, and `FONTS:READ` scopes. Do not use a secret `sk.` token in the app.

The token configuration is deliberately local and is not committed to Git:

1. Copy [app/developer-config.xml.template](app/developer-config.xml.template) to `app/src/main/res/values/developer-config.xml`.
2. Replace `YOUR_PUBLIC_MAPBOX_ACCESS_TOKEN` in the copied file with your `pk.` token.

In PowerShell:

```powershell
Copy-Item app\developer-config.xml.template app\src\main\res\values\developer-config.xml
```

The template is stored in `app/`, not `app/src/main/res/values/`, because Android treats every file in the resources directory as a build input. The real `developer-config.xml` belongs in `app/src/main/res/values/`, where the Mapbox Android SDK finds the `mapbox_access_token` resource. That file is ignored by Git, so it remains local to each developer machine.

### Build and install a debug APK

Connect an Android device with USB debugging enabled, then run:

```powershell
.\gradlew.bat installRegularDebug
```

This builds the `regularDebug` variant and installs it on the connected device. To create the APK without installing it, run:

```powershell
.\gradlew.bat assembleRegularDebug
```

You can also open the project in Android Studio, create `developer-config.xml` as above, select the `regularDebug` build variant, and press **Run**.


## License

    Apache License, Version 2.0

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

This application was developed using free software from Jetbrains for OpenSource projects.

[<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_square.png" alt="JetBrains Black Box Logo logo." width=100>](https://www.jetbrains.com)
