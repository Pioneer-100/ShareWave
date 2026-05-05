package com.swiftshare.network.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

/**
 * BroadcastReceiver for Wi-Fi Direct system broadcasts.
 *
 * Forwards state changes to [WifiDirectManager] via the [onStateChanged] callback.
 * Registered dynamically in the manifest and via context for lifecycle awareness.
 */
class WifiDirectReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val onStateChanged: (WifiDirectState) -> Unit,
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiDirectReceiver"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d(TAG, "Wi-Fi P2P enabled")
                    onStateChanged(WifiDirectState.Ready)
                } else {
                    Log.d(TAG, "Wi-Fi P2P disabled")
                    onStateChanged(WifiDirectState.Disabled)
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d(TAG, "Peers changed — requesting peer list")
                try {
                    manager.requestPeers(channel) { peerList ->
                        // WifiDirectManager processes the peer list
                        Log.d(TAG, "Found ${peerList.deviceList.size} peers")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied for requestPeers", e)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(
                    WifiP2pManager.EXTRA_NETWORK_INFO
                )
                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel) { info ->
                        info?.let {
                            val ownerAddr = it.groupOwnerAddress?.hostAddress ?: ""
                            Log.d(TAG, "Connected — GO=${it.isGroupOwner}, addr=$ownerAddr")
                            onStateChanged(
                                WifiDirectState.Connected(
                                    groupOwnerAddress = ownerAddr,
                                    isGroupOwner = it.isGroupOwner,
                                )
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "Disconnected")
                    onStateChanged(WifiDirectState.Disconnected)
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Can extract this device's details if needed for display
                Log.d(TAG, "This device info changed")
            }
        }
    }
}
