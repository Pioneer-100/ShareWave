package com.swiftshare.network.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.swiftshare.data.repository.DeviceRepositoryImpl
import com.swiftshare.di.IoDispatcher
import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.domain.model.TransportChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages NSD (Network Service Discovery) for advertising and browsing
 * SwiftShare services on the local network segment formed by Wi-Fi Direct groups.
 *
 * Service type: _swiftshare._tcp.
 * Service TXT records carry the transfer port and device name.
 */
@Singleton
class NsdDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nsdManager: NsdManager,
    private val deviceRepository: DeviceRepositoryImpl,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TAG = "NsdDiscovery"
        private const val SERVICE_TYPE = "_swiftshare._tcp."
        private const val SERVICE_NAME_PREFIX = "SwiftShare"
        private const val KEY_PORT = "port"
        private const val KEY_DEVICE_NAME = "name"
        private const val MAX_RETRY_DELAY_MS = 16_000L
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isBrowsing = MutableStateFlow(false)
    val isBrowsing: StateFlow<Boolean> = _isBrowsing.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /** Tracks services we've already resolved to debounce duplicates. */
    private val resolvedServices = ConcurrentHashMap<String, NearbyDevice>()

    private var transferPort: Int = 49152
    private var localDeviceName: String = android.os.Build.MODEL

    // ──────────────────────────── Advertise ────────────────────────────

    fun startAdvertising(port: Int, deviceName: String) {
        if (_isAdvertising.value) return
        transferPort = port
        localDeviceName = deviceName

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "${SERVICE_NAME_PREFIX}_${deviceName.take(10)}"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute(KEY_PORT, port.toString())
            setAttribute(KEY_DEVICE_NAME, deviceName)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${info.serviceName}")
                _isAdvertising.value = true
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: error=$errorCode")
                _isAdvertising.value = false
                // Retry with exponential backoff
                retryWithBackoff { startAdvertising(port, deviceName) }
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${info.serviceName}")
                _isAdvertising.value = false
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed: error=$errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            // TODO: OEM fragmentation — some devices throw IllegalArgumentException here
            Log.e(TAG, "registerService threw", e)
            retryWithBackoff { startAdvertising(port, deviceName) }
        }
    }

    fun stopAdvertising() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e(TAG, "unregisterService threw", e)
            }
        }
        registrationListener = null
        _isAdvertising.value = false
    }

    // ──────────────────────────── Browse ────────────────────────────

    fun startBrowsing() {
        if (_isBrowsing.value) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started: $serviceType")
                _isBrowsing.value = true
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${info.serviceName}")
                // Don't resolve our own service
                if (info.serviceName.contains(localDeviceName.take(10))) return
                resolveService(info)
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${info.serviceName}")
                val deviceId = info.serviceName
                resolvedServices.remove(deviceId)
                deviceRepository.removeDevice(deviceId)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
                _isBrowsing.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: error=$errorCode")
                _isBrowsing.value = false
                // TODO: OEM — Huawei EMUI < 10 may never fire callbacks; fall back to broadcast
                retryWithBackoff { startBrowsing() }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: error=$errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "discoverServices threw", e)
            retryWithBackoff { startBrowsing() }
        }
    }

    fun stopBrowsing() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "stopServiceDiscovery threw", e)
            }
        }
        discoveryListener = null
        _isBrowsing.value = false
        resolvedServices.clear()
    }

    // ──────────────────────────── Resolve ────────────────────────────

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onServiceResolved(info: NsdServiceInfo) {
                Log.d(TAG, "Resolved: ${info.serviceName} → ${info.host?.hostAddress}:${info.port}")
                val deviceName = info.attributes[KEY_DEVICE_NAME]?.let { String(it) }
                    ?: info.serviceName.removePrefix("${SERVICE_NAME_PREFIX}_")
                val port = info.attributes[KEY_PORT]?.let { String(it).toIntOrNull() }
                    ?: info.port

                val device = NearbyDevice(
                    id = info.serviceName,
                    name = deviceName,
                    ipAddress = info.host?.hostAddress,
                    channel = TransportChannel.WIFI_DIRECT,
                )
                resolvedServices[info.serviceName] = device
                deviceRepository.addOrUpdateDevice(device)
            }

            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${info.serviceName}: error=$errorCode")
                // TODO: OEM — retry once for transient NSD errors
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "resolveService threw", e)
        }
    }

    // ──────────────────────────── Retry ────────────────────────────

    private var retryDelayMs = 1_000L

    private fun retryWithBackoff(action: () -> Unit) {
        scope.launch {
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
            action()
        }
    }

    fun resetRetry() {
        retryDelayMs = 1_000L
    }

    fun teardown() {
        stopAdvertising()
        stopBrowsing()
        scope.cancel()
    }
}
