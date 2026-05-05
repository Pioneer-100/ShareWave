package com.swiftshare.network.wifidirect

/**
 * Sealed class representing the Wi-Fi Direct subsystem state.
 */
sealed class WifiDirectState {
    /** Wi-Fi P2P is enabled and ready for discovery. */
    data object Ready : WifiDirectState()

    /** Wi-Fi P2P is disabled (user turned off Wi-Fi or hardware unsupported). */
    data object Disabled : WifiDirectState()

    /** Actively searching for peers. */
    data object Discovering : WifiDirectState()

    /** Connected to a peer; [isGroupOwner] indicates if this device owns the group. */
    data class Connected(
        val groupOwnerAddress: String,
        val isGroupOwner: Boolean,
    ) : WifiDirectState()

    /** Connection was lost or intentionally disconnected. */
    data object Disconnected : WifiDirectState()

    /** An error occurred. */
    data class Error(val message: String) : WifiDirectState()
}
