package com.swiftshare.domain.repository

import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.domain.model.TransportChannel
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    /** Live list of discovered nearby peers, updated by NSD + Wi-Fi Direct. */
    val discoveredDevices: StateFlow<List<NearbyDevice>>

    /** Active transport channel currently in use. */
    val activeChannel: StateFlow<TransportChannel>

    fun startDiscovery()
    fun stopDiscovery()
    fun addOrUpdateDevice(device: NearbyDevice)
    fun removeDevice(deviceId: String)
    fun clearDevices()
    suspend fun updateThroughput(deviceId: String, throughputKbps: Long)
}
