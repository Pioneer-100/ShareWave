package com.swiftshare.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshare.data.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    fun nextPage() {
        _currentPage.value = (_currentPage.value + 1).coerceAtMost(2)
    }

    fun previousPage() {
        _currentPage.value = (_currentPage.value - 1).coerceAtLeast(0)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsDataStore.setOnboardingCompleted()
        }
    }
}
