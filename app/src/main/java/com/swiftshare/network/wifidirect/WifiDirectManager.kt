package com.swiftshare.network.wifidirect

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.WpsInfo
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps [WifiP2pManager] for peer discovery, connection handshake, and group management.
 *
 * All public methods are safe to call from any thread.
 * OEM-specific quirks are guarded with try/catch and TODO comments.
 */
@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val p2pManager: WifiP2pManager,
    private val p2pChannel: WifiP2pManager.Channel,
    private val deviceRepository: DeviceRepositoryImpl,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TAG = "WifiDirectMgr"
        private const val DISCOVERY_TIMEOUT_MS = 30_000L
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _state = MutableStateFlow<WifiDirectState>(WifiDirectState.Disabled)
    val state: StateFlow<WifiDirectState> = _state.asStateFlow()

    private var receiver: WifiDirectReceiver? = null
    private var discoveryJob: Job? = null

    // ──────────────────────────── Lifecycle ────────────────────────────

    fun register() {
        receiver = WifiDirectReceiver(p2pManager, p2pChannel) { newState ->
            _state.value = newState
        }
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        Log.d(TAG, "Registered WifiDirectReceiver")
    }

    fun unregister() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "unregisterReceiver", e)
            }
        }
        receiver = null
        discoveryJob?.cancel()
    }

    // ──────────────────────────── Discovery ────────────────────────────

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        discoveryJob?.cancel()
        _state.value = WifiDirectState.Discovering

        try {
            p2pManager.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Peer discovery initiated")
                }

                override fun onFailure(reason: Int) {
                    val msg = when (reason) {
                        WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
                        WifiP2pManager.BUSY -> "Framework busy"
                        WifiP2pManager.ERROR -> "Internal error"
                        else -> "Unknown error ($reason)"
                    }
                    Log.e(TAG, "discoverPeers failed: $msg")
                    _state.value = WifiDirectState.Error(msg)
                    // TODO: OEM — Samsung may return BUSY on first call; retry after 2s
                }
            })
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for discoverPeers", e)
            _state.value = WifiDirectState.Error("Missing NEARBY_WIFI_DEVICES permission")
        } catch (e: Exception) {
            // TODO: OEM — catch unexpected exceptions from vendor P2P implementations
            Log.e(TAG, "discoverPeers threw", e)
            _state.value = WifiDirectState.Error(e.message ?: "Unknown error")
        }

        // Auto-stop discovery after timeout to save battery
        discoveryJob = scope.launch {
            delay(DISCOVERY_TIMEOUT_MS)
            stopDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun requestPeerList(onResult: (List<NearbyDevice>) -> Unit) {
        try {
            p2pManager.requestPeers(p2pChannel) { peerList ->
                val devices = peerList.deviceList.map { it.toNearbyDevice() }
                devices.forEach { deviceRepository.addOrUpdateDevice(it) }
                onResult(devices)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for requestPeers", e)
            onResult(emptyList())
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        try {
            p2pManager.stopPeerDiscovery(p2pChannel, null)
        } catch (e: Exception) {
            Log.e(TAG, "stopPeerDiscovery threw", e)
        }
    }

    // ──────────────────────────── Connect ────────────────────────────

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: NearbyDevice, onResult: (Boolean) -> Unit) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.id
            wps.setup = WpsInfo.PBC
            // TODO: OEM — on some Xiaomi devices, groupOwnerIntent must be set to 15
            groupOwnerIntent = 0 // let framework decide
        }

        try {
            p2pManager.connect(p2pChannel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Connection initiated to ${device.name}")
                    onResult(true)
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "connect failed: reason=$reason")
                    _state.value = WifiDirectState.Error("Connection failed (code $reason)")
                    onResult(false)
                }
            })
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for connect", e)
            onResult(false)
        } catch (e: Exception) {
            Log.e(TAG, "connect threw", e)
            onResult(false)
        }
    }

    // ──────────────────────────── Disconnect ────────────────────────────

    fun disconnect() {
        try {
            p2pManager.removeGroup(p2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Group removed")
                    _state.value = WifiDirectState.Disconnected
                }

                override fun onFailure(reason: Int) {
                    // TODO: OEM — OnePlus: removeGroup sometimes hangs; use withTimeout(5s)
                    Log.e(TAG, "removeGroup failed: reason=$reason")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "removeGroup threw", e)
        }
    }

    // ──────────────────────────── Cleanup ────────────────────────────

    fun teardown() {
        unregister()
        disconnect()
        scope.cancel()
    }

    // ──────────────────────────── Helpers ────────────────────────────

    private fun WifiP2pDevice.toNearbyDevice() = NearbyDevice(
        id = deviceAddress,
        name = deviceName.ifBlank { deviceAddress },
        channel = TransportChannel.WIFI_DIRECT,
        isConnected = status == WifiP2pDevice.CONNECTED,
    )
}
