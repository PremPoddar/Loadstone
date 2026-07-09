package com.prempoddar.loadstone.data.settings

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private fun repo() =
        SettingsRepository(ApplicationProvider.getApplicationContext())

    @Test fun `defaults then persisted values round trip`() = runTest {
        val repo = repo()

        val defaults = repo.settings.first()
        assertThat(defaults.onboardingComplete).isFalse()
        assertThat(defaults.themeMode).isEqualTo(ThemeMode.SYSTEM)

        repo.setUnitSystem(UnitSystem.IMPERIAL)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setOnboardingComplete(true)

        val updated = repo.settings.first()
        assertThat(updated.unitSystem).isEqualTo(UnitSystem.IMPERIAL)
        assertThat(updated.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(updated.onboardingComplete).isTrue()
    }
}
