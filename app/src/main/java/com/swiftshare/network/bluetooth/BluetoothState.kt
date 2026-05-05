package com.swiftshare.network.bluetooth

/**
 * Sealed class representing Bluetooth subsystem state.
 * Kept separate from [BluetoothTransport.BtState] for consumers
 * that only need to observe availability, not connection details.
 */
sealed class BluetoothState {
    data object Available : BluetoothState()
    data object Unavailable : BluetoothState()
    data object Connecting : BluetoothState()
    data class Connected(val deviceName: String) : BluetoothState()
    data object Disconnected : BluetoothState()
}
