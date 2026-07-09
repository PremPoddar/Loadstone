# Loadstone

Drop a pin, follow it back. Loadstone is a simple offline navigation app — not a map.
Mark a spot (your car, a campsite, a trailhead), and later a single compass arrow points
you straight back to it.

## What makes it accurate

A phone's GPS doesn't report one true point — each fix comes with an accuracy radius that
shrinks as the receiver settles. While you're on the **Drop a pin** screen, Loadstone keeps
collecting fixes and converges on the real position using an **inverse-variance weighted
average** with outlier rejection: tighter fixes count more, wild ones are discarded, and the
estimate visibly sharpens the longer you hold still. Stand there a few seconds and watch the
±meters reading fall before you save.

The follow-back arrow is a true compass: it uses the rotation-vector sensor and corrects for
magnetic declination (via `GeomagneticField`), so it points at the real target, not magnetic
north — and it works while you're standing still.

## Features

- Onboarding that explains drop → name → follow in three screens
- Live-tightening accuracy while dropping a pin
- Named pins with an optional note and a color/icon category
- Compass arrow with distance and an "arrived" state
- Share a pin as coordinates, or open a shared `geo:` link to save one
- Material 3 with Material You dynamic color, full light and dark themes
- Metric/imperial units (auto from locale, override in Settings)
- Location is used **only while the app is open** — no background tracking

## Build & run

Requires the Android SDK and JDK 17+.

```bash
./gradlew :app:installDebug   # build and install on a connected device/emulator
./gradlew :app:testDebugUnitTest          # unit tests (accuracy engine, geo math, formatting)
./gradlew :app:connectedDebugAndroidTest  # Room persistence tests (needs a device/emulator)
```

- `minSdk` 26 (Android 8.0) · `targetSdk` 36
- Package: `com.prempoddar.loadstone`

## Architecture

Single-activity Jetpack Compose, MVVM with `StateFlow`, Hilt for DI.

- `core/geo` — pure, testable logic: `LocationAveraging` (accuracy engine), `GeoMath` (distance,
  bearing, arrow angle).
- `core/format` — distance and coordinate formatting.
- `data/pin` · `data/db` — Room-backed pin storage.
- `data/settings` — DataStore-backed preferences.
- `data/location` — `LocationRepository` (fused provider + live averaging) and
  `CompassRepository` (true-north heading).
- `ui/…` — one package per screen (onboarding, home, drop, navigate, settings) plus shared
  `components` (the `AccuracyDial` and `CompassArrow` are custom-drawn).
