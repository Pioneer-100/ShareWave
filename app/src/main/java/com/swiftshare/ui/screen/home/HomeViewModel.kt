package com.swiftshare.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshare.data.datastore.UserPreferences
import com.swiftshare.data.datastore.UserPreferencesDataStore
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.domain.model.TransportChannel
import com.swiftshare.domain.repository.DeviceRepository
import com.swiftshare.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentTransfers: List<TransferRecord> = emptyList(),
    val totalBytesSent: Long = 0L,
    val totalBytesSaved: Long = 0L,
    val activeChannel: TransportChannel = TransportChannel.UNKNOWN,
    val deviceName: String = android.os.Build.MODEL,
    val aiCompressionEnabled: Boolean = true,
    val aiDeduplicationEnabled: Boolean = true,
    val onboardingCompleted: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val deviceRepository: DeviceRepository,
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        transferRepository.observeAll(),
        transferRepository.observeTotalBytesSent(),
        transferRepository.observeTotalBytesSaved(),
        deviceRepository.activeChannel,
        prefsDataStore.preferences,
    ) { transfers, bytesSent, bytesSaved, channel, prefs ->
        HomeUiState(
            recentTransfers = transfers.take(10),
            totalBytesSent = bytesSent,
            totalBytesSaved = bytesSaved,
            activeChannel = channel,
            deviceName = prefs.deviceDisplayName.ifBlank { android.os.Build.MODEL },
            aiCompressionEnabled = prefs.aiCompressionEnabled,
            aiDeduplicationEnabled = prefs.aiDeduplicationEnabled,
            onboardingCompleted = prefs.onboardingCompleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun setDeviceName(name: String) {
        viewModelScope.launch { prefsDataStore.setDeviceName(name) }
    }
}
