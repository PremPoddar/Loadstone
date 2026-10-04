# Loadstone

Drop a pin, follow it back. Loadstone is a simple offline navigation app — not a map.
Mark a spot (your car, a campsite, a trailhead), and later a single compass arrow points
you straight back to it.

## What makes it accurate

A phone's GPS doesn't report one true point — each fix comes with an accuracy radius that
shrinks as the receiver settles. Loadstone runs those fixes through a **recursive Bayesian
(Kalman) filter** in a local east/north frame: position is a mean and a variance, each fix is
weighted by its own accuracy, and the gap between fixes widens the variance by however far you
could have walked in it. Standing still converges; walking up to the spot decays the approach
out of the estimate on its own; a multipath spike fails a gate stated in standard deviations.

The uncertainty it reports is the point. Fixes that are all wrong by the same thirty metres
agree perfectly with each other, so an estimate built from scatter alone calls that tight — which
is how a pin saved at "±2 m" could put you on the wrong side of a car park. The filter refuses to
claim better than the part of GPS error that repeated measurements cannot remove.

Nothing you do while saving removes that shared error, but coming back does. Stand on the spot
with a good fix and Loadstone offers to **sharpen the pin**: hours or weeks later the satellites
and the atmosphere have moved on, so the second measurement is wrong in a genuinely independent
direction and the two together land closer than either.

The follow-back arrow is a true compass: it uses the rotation-vector sensor and corrects for
magnetic declination (via `GeomagneticField`), so it points at the real target, not magnetic
north — and it works while you're standing still. Its smoothing adapts rather than splitting the
difference: a dead band kills sub-degree sensor noise, and the gain opens up as you turn, so the
arrow is still when you are still and immediate when you move. Once you are closer to the pin
than the uncertainty, it stops pointing and says so — a five-metre error seen from four metres
away swings the bearing through tens of degrees.

## Features

- Onboarding that explains drop → name → follow in three screens
- Live-tightening accuracy while dropping a pin
- Named pins with an optional note and a color/icon category, each card wearing its own icon
- Photos on the pin card, with a full-screen swipeable gallery
- Compass arrow with distance, an "arrived" state and an honest search radius
- Sharpen a saved pin by standing on it again
- Swipe a card right to pin it to the top of the list, left to unpin
- Share a pin as seven funny words — "Grumpy walruses quietly juggle soggy waffles backwards."
  — which travels through any chat app, or down a phone line, with no link to open
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

- `core/geo` — pure, testable logic: `PositionEstimator` (the filter), `LocalFrame` (east/north
  tangent plane), `HeadingFilter` (adaptive compass smoothing), `PinRefinement` (merging a return
  visit), `GeoMath` (distance, bearing, arrow angle).
- `core/format` — distance and coordinate formatting.
- `data/pin` · `data/db` — Room-backed pin storage.
- `data/settings` — DataStore-backed preferences.
- `data/location` — `LocationRepository` (fused provider + live filtering) and
  `CompassRepository` (true-north heading).
- `share` — `WordCode` and its frozen vocabularies, plus `AppLinks` for incoming shared links.
- `ui/…` — one package per screen (onboarding, home, drop, navigate, settings) plus shared
  `components` (the `AccuracyDial` and `CompassArrow` are custom-drawn).
