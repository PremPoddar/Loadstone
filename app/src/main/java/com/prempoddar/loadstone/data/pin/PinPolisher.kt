package com.prempoddar.loadstone.data.pin

import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.core.geo.PinPolish
import com.prempoddar.loadstone.data.location.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs [PinPolish] after a save, outside any screen, so leaving the drop screen straight away
 * doesn't cut the sharpening short.
 */
@Singleton
class PinPolisher @Inject constructor(
    private val locations: LocationRepository,
    private val pins: PinRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun polish(pinId: Long, saved: PinFix) {
        scope.launch {
            var best = saved
            withTimeoutOrNull(WINDOW_MS) {
                locations.estimatedFixes()
                    .catch { }
                    .takeWhile { next ->
                        when (val s = PinPolish.step(saved, best, next)) {
                            PinPolish.Step.Stop -> false
                            PinPolish.Step.Keep -> true
                            is PinPolish.Step.Improve -> {
                                best = s.fix
                                // Re-read the pin: the person may have renamed it or added photos meanwhile.
                                pins.get(pinId)?.let { pin ->
                                    pins.upsert(
                                        pin.copy(
                                            latitude = s.fix.latitude, longitude = s.fix.longitude,
                                            accuracyMeters = s.fix.estAccuracyMeters,
                                            elevationMeters = s.fix.altitudeMeters ?: pin.elevationMeters,
                                        ),
                                    )
                                }
                                true
                            }
                        }
                    }
                    .collect { }
            }
        }
    }

    companion object {
        const val WINDOW_MS = 45_000L
    }
}
