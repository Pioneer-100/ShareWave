package com.swiftshare.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.swiftshare.data.repository.DeviceRepositoryImpl
import com.swiftshare.di.IoDispatcher
import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.domain.model.TransportChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth Classic fallback transport.
 *
 * Activates when Wi-Fi Direct is unavailable or the peer lacks P2P support.
 * Uses a well-known UUID for SwiftShare so both devices can agree on the RFCOMM channel.
 */
@Singleton
class BluetoothTransport @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val deviceRepository: DeviceRepositoryImpl,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TAG = "BtTransport"
        /** Well-known UUID for SwiftShare RFCOMM service. */
        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        private const val SERVICE_NAME = "SwiftShare"
    }

    sealed class BtState {
        data object Idle : BtState()
        data object Listening : BtState()
        data class Connected(val deviceName: String) : BtState()
        data object Unavailable : BtState()
        data class Error(val message: String) : BtState()
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _state = MutableStateFlow<BtState>(BtState.Idle)
    val state: StateFlow<BtState> = _state.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    val isAvailable: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    // ──────────────────────────── Server (Receiver side) ────────────────

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _state.value = BtState.Unavailable
            return
        }

        scope.launch {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME, SERVICE_UUID
                )
                _state.value = BtState.Listening
                Log.d(TAG, "Bluetooth server listening")

                // Blocking accept — runs on IO dispatcher
                val socket = serverSocket?.accept()
                if (socket != null) {
                    clientSocket = socket
                    val remoteName = socket.remoteDevice?.name ?: "Unknown"
                    _state.value = BtState.Connected(remoteName)
                    Log.d(TAG, "Accepted connection from $remoteName")

                    deviceRepository.addOrUpdateDevice(
                        NearbyDevice(
                            id = socket.remoteDevice?.address ?: "bt-unknown",
                            name = remoteName,
                            channel = TransportChannel.BLUETOOTH,
                            isConnected = true,
                        )
                    )
                    deviceRepository.setActiveChannel(TransportChannel.BLUETOOTH)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission denied", e)
                _state.value = BtState.Error("Bluetooth permission denied")
            } catch (e: IOException) {
                if (_state.value is BtState.Listening) {
                    Log.e(TAG, "Server socket accept failed", e)
                    _state.value = BtState.Error(e.message ?: "Accept failed")
                }
                // else: socket was closed intentionally via stopListening()
            }
        }
    }

    fun stopListening() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null
        if (_state.value is BtState.Listening) {
            _state.value = BtState.Idle
        }
    }

    // ──────────────────────────── Client (Sender side) ────────────────

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: NearbyDevice, onResult: (Boolean) -> Unit) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _state.value = BtState.Unavailable
            onResult(false)
            return
        }

        scope.launch {
            try {
                val remoteDevice = bluetoothAdapter.getRemoteDevice(device.id)
                val socket = remoteDevice.createRfcommSocketToServiceRecord(SERVICE_UUID)
                bluetoothAdapter.cancelDiscovery() // must stop discovery before connect

                socket.connect()
                clientSocket = socket
                _state.value = BtState.Connected(device.name)
                deviceRepository.setActiveChannel(TransportChannel.BLUETOOTH)
                Log.d(TAG, "Connected to ${device.name}")
                withContext(Dispatchers.Main) { onResult(true) }
            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission denied", e)
                _state.value = BtState.Error("Bluetooth permission denied")
                withContext(Dispatchers.Main) { onResult(false) }
            } catch (e: IOException) {
                Log.e(TAG, "Bluetooth connect failed", e)
                _state.value = BtState.Error(e.message ?: "Connect failed")
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // ──────────────────────────── I/O ────────────────────────────

    fun getInputStream(): InputStream? = clientSocket?.inputStream

    fun getOutputStream(): OutputStream? = clientSocket?.outputStream

    // ──────────────────────────── Cleanup ────────────────────────────

    fun disconnect() {
        try {
            clientSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing client socket", e)
        }
        clientSocket = null
        _state.value = BtState.Idle
    }

    fun teardown() {
        stopListening()
        disconnect()
        scope.cancel()
    }
}
