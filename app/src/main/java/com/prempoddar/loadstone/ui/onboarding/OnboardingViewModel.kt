package com.prempoddar.loadstone.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(true)
            onDone()
        }
    }
}
