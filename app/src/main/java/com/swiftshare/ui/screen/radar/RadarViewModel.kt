package com.swiftshare.ui.screen.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshare.data.datastore.UserPreferencesDataStore
import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.domain.model.TransportChannel
import com.swiftshare.domain.repository.DeviceRepository
import com.swiftshare.network.transfer.TransferEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadarUiState(
    val devices: List<NearbyDevice> = emptyList(),
    val activeChannel: TransportChannel = TransportChannel.UNKNOWN,
    val isScanning: Boolean = false,
    val deviceName: String = android.os.Build.MODEL,
)

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val transferEngine: TransferEngine,
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    val uiState: StateFlow<RadarUiState> = combine(
        deviceRepository.discoveredDevices,
        deviceRepository.activeChannel,
        prefsDataStore.preferences,
    ) { devices, channel, prefs ->
        RadarUiState(
            devices = devices,
            activeChannel = channel,
            isScanning = devices.isEmpty() || channel == TransportChannel.UNKNOWN,
            deviceName = prefs.deviceDisplayName.ifBlank { android.os.Build.MODEL },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RadarUiState(),
    )

    fun startScanning() {
        val name = uiState.value.deviceName
        transferEngine.startDiscovery(name)
    }

    fun stopScanning() {
        transferEngine.stopDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }
}
