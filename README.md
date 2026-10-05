<div align="center">

<img src=".github/assets/icon.png" width="112" alt="Loadstone">

# Loadstone

**Drop a pin. Follow it back.**

An offline compass that points at the spot you saved — not a map.

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

</div>

---

Mark where you left the car on level 3, where the trailhead was, where the tent is. Hours or
weeks later, open the pin and walk: one arrow, one distance, no map tiles, no signal needed.

Loadstone does one thing, and the whole app is built around doing it honestly — telling you not
just where to walk but how sure it is, because a confident arrow pointing at the wrong bench is
worse than no arrow at all.

## What it does

| | |
|---|---|
| 🧭 **Follow-back arrow** | True north, corrected for magnetic declination. Works standing still. |
| 📍 **Honest accuracy** | Every pin carries the radius you may have to search, and the app refuses to overstate it. |
| ✨ **Sharpen a pin** | Stand on the spot again and fold that visit in — the only thing that fixes a pin saved badly. |
| 📷 **Photos** | Snap the parking level or the trail marker. They sit on the card; tap for a swipeable gallery. |
| 💬 **Share as words** | Seven funny words instead of a link. Travels through any chat app, or down a phone line. |
| 📌 **Pin to top** | Swipe a card right to hold it above the distance sort, left to let it go. |
| 🏷️ **Categories** | Car, home, camp, trail, star — and every card draws its own icon from the family. |
| 🌗 **Material You** | Dynamic color, full light and dark themes, metric or imperial. |

Location is used **only while the app is open**. Nothing is tracked in the background, and
nothing leaves the phone unless you share a pin yourself.

## Sharing a place as a sentence

A shared link is only as good as whatever opens it. A sentence needs nothing:

```
Grumpy walruses quietly juggle soggy waffles backwards.
```

That is a position, to within about three metres, anywhere on Earth. Seven words drawn from four
frozen 128-word vocabularies in a fixed grammar — *adjective, noun, adverb, verb, adjective,
noun, adverb* — carrying 49 bits: 23 each for latitude and longitude, plus a checksum so a
sentence misremembered by one word is refused rather than quietly pointing at a different street.

The grammar does two jobs. It makes the result funny every time rather than now and then, because
any adjective in the list sits happily on any noun in the list. And it makes a word in the wrong
position wrong by construction, before any arithmetic runs.

Read one back with the **abc** button in the toolbar. It accepts the whole forwarded message, not
just the seven words, and tells you which word it doesn't recognise as you type.

## How it stays accurate

A phone's GPS doesn't report one true point — each fix comes with an accuracy radius that shrinks
as the receiver settles. Loadstone runs those fixes through a **recursive Bayesian (Kalman)
filter** in a local east/north frame: position is a mean and a variance, each fix is weighted by
its own accuracy, and the gap between fixes widens the variance by however far you could have
walked in it.

Standing still converges. Walking up to the spot decays the approach out of the estimate on its
own. A multipath spike fails a gate stated in standard deviations rather than in hand-tuned
metres.

<details>
<summary><strong>Why the uncertainty number is the whole point</strong></summary>

<br>

Fixes that are all wrong by the same thirty metres — bounced off a wall, taken under bad
satellite geometry — agree perfectly with each other. An estimate built from scatter alone calls
that tight, which is how a pin saved at "±2 m" could put you on the wrong side of a car park. The
filter propagates variance explicitly and then refuses to claim better than the part of GPS error
that repeated measurements cannot remove at all.

Nothing you do while saving removes that shared error. Coming back does: hours or weeks later the
satellites and the atmosphere have moved on, so a second measurement is wrong in a genuinely
independent direction, and the two together land closer than either. That is what **sharpen this
pin** does, and it is the only way to correct a pin that was already saved badly.

</details>

<details>
<summary><strong>Why the arrow is steady</strong></summary>

<br>

One fixed smoothing strength cannot be both still at rest and quick into a turn, and splitting
the difference gives you a shiver *and* a lag. A dead band kills sub-degree sensor noise, and the
gain opens up as you turn, so the arrow is still when you are still and immediate when you move.

Navigation also runs on the filtered estimate rather than raw fixes, which hop several metres a
second — at close range that hop is most of the bearing. And once you are nearer the pin than the
combined uncertainty, the arrow stops pointing and says so: a five-metre error seen from four
metres away swings the direction through tens of degrees, and pointing anyway is a confident
guess.

</details>

## Build & run

Requires the Android SDK and JDK 17.

| Command | What it does |
|---|---|
| `./gradlew :app:installDebug` | Build and install on a connected device or emulator |
| `./gradlew :app:testDebugUnitTest` | Unit tests — the filter, the word codec, geo maths, formatting |
| `./gradlew :app:connectedDebugAndroidTest` | Room persistence tests (needs a device) |
| `./gradlew bundleRelease` | Signed release bundle for Play (see `keystore.properties.sample`) |

`minSdk` 26 (Android 8.0) · `targetSdk` 36 · package `com.prempoddar.loadstone`

## Architecture

Single-activity Jetpack Compose, MVVM with `StateFlow`, Hilt for DI.

```
core/geo       PositionEstimator  the filter every pin comes from
               LocalFrame         east/north tangent plane
               HeadingFilter      adaptive compass smoothing
               PinRefinement      merging a return visit into a stored pin
               GeoMath            distance, bearing, arrow angle
core/format    distance and coordinate formatting
data/location  LocationRepository (fused provider) · CompassRepository (true north)
data/pin       pin model and repository
data/db        Room entities, DAO and migrations
data/settings  DataStore-backed preferences
share          WordCode and its frozen vocabularies · AppLinks for incoming links
ui/            one package per screen, plus components —
               AccuracyDial and CompassArrow are custom-drawn
```

Everything in `core/` is free of Android dependencies, so the parts worth being sure about are
covered by plain JVM tests.

## License

[Apache 2.0](LICENSE)
