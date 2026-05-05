package com.swiftshare.data.repository

import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.domain.model.TransportChannel
import com.swiftshare.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor() : DeviceRepository {

    private val _discoveredDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<NearbyDevice>> = _discoveredDevices.asStateFlow()

    private val _activeChannel = MutableStateFlow(TransportChannel.UNKNOWN)
    override val activeChannel: StateFlow<TransportChannel> = _activeChannel.asStateFlow()

    // NSD/Wi-Fi Direct managers call startDiscovery/stopDiscovery via TransferEngine
    override fun startDiscovery() { /* orchestrated by TransferEngine */ }
    override fun stopDiscovery()  { /* orchestrated by TransferEngine */ }

    override fun addOrUpdateDevice(device: NearbyDevice) {
        _discoveredDevices.update { current ->
            val idx = current.indexOfFirst { it.id == device.id }
            if (idx >= 0) current.toMutableList().also { it[idx] = device }
            else current + device
        }
    }

    override fun removeDevice(deviceId: String) {
        _discoveredDevices.update { it.filter { d -> d.id != deviceId } }
    }

    override fun clearDevices() {
        _discoveredDevices.value = emptyList()
    }

    override suspend fun updateThroughput(deviceId: String, throughputKbps: Long) {
        _discoveredDevices.update { current ->
            current.map { if (it.id == deviceId) it.copy(throughputKbps = throughputKbps) else it }
        }
    }

    fun setActiveChannel(channel: TransportChannel) {
        _activeChannel.value = channel
    }
}
