package com.swiftshare.network.transfer

import android.util.Log
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.data.db.entity.TransferStatus
import com.swiftshare.data.repository.DeviceRepositoryImpl
import com.swiftshare.di.IoDispatcher
import com.swiftshare.domain.model.*
import com.swiftshare.domain.repository.FileRepository
import com.swiftshare.domain.repository.TransferRepository
import com.swiftshare.network.bluetooth.BluetoothTransport
import com.swiftshare.network.nsd.NsdDiscoveryManager
import com.swiftshare.network.wifidirect.WifiDirectManager
import com.swiftshare.network.wifidirect.WifiDirectState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the entire transfer lifecycle:
 *
 *   1. Discovery (NSD + Wi-Fi Direct peer scan)
 *   2. Channel selection (prefer Wi-Fi Direct, fall back to Bluetooth)
 *   3. PIN-based session authorization
 *   4. Chunked file transfer with progress tracking
 *   5. Record persistence to Room
 *
 * Call [startSending] or [startReceiving] to begin a session.
 * Observe [transferProgress] for real-time updates.
 */
@Singleton
class TransferEngine @Inject constructor(
    private val wifiDirectManager: WifiDirectManager,
    private val nsdDiscoveryManager: NsdDiscoveryManager,
    private val bluetoothTransport: BluetoothTransport,
    private val deviceRepository: DeviceRepositoryImpl,
    private val transferRepository: TransferRepository,
    private val fileRepository: FileRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TAG = "TransferEngine"
        private const val TRANSFER_PORT = 49152
        private const val SOCKET_TIMEOUT_MS = 30_000
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val chunkedTransfer = ChunkedFileTransfer()

    val transferProgress: StateFlow<TransferProgress> = chunkedTransfer.progress

    private val _sessionState = MutableStateFlow<TransferSessionState>(TransferSessionState.AwaitingPin)
    val sessionState: StateFlow<TransferSessionState> = _sessionState.asStateFlow()

    private var currentSession: TransferSession? = null
    private var transferJob: Job? = null

    // ──────────────────────────── Discovery ────────────────────────────

    fun startDiscovery(deviceName: String) {
        Log.d(TAG, "Starting discovery as '$deviceName'")
        wifiDirectManager.register()
        wifiDirectManager.discoverPeers()
        nsdDiscoveryManager.startAdvertising(TRANSFER_PORT, deviceName)
        nsdDiscoveryManager.startBrowsing()
    }

    fun stopDiscovery() {
        wifiDirectManager.stopDiscovery()
        nsdDiscoveryManager.stopAdvertising()
        nsdDiscoveryManager.stopBrowsing()
    }

    // ──────────────────────────── Send Flow ────────────────────────────

    /**
     * Initiates a send session to [targetDevice].
     *
     * 1. Connects via Wi-Fi Direct (or Bluetooth fallback)
     * 2. Creates a TransferSession with a 6-digit PIN
     * 3. Opens a socket and sends files via [ChunkedFileTransfer]
     */
    fun startSending(
        targetDevice: NearbyDevice,
        files: List<FileItem>,
        pin: String,
        onSessionCreated: (TransferSession) -> Unit,
    ) {
        val session = TransferSession(
            pin = pin,
            peerId = targetDevice.id,
            peerName = targetDevice.name,
            direction = TransferDirection.SEND,
            files = files,
            channel = targetDevice.channel,
        )
        currentSession = session
        onSessionCreated(session)

        transferJob = scope.launch {
            _sessionState.value = TransferSessionState.AwaitingPin

            // Attempt Wi-Fi Direct connection first
            val connected = attemptWifiDirectConnection(targetDevice)
            val channel: TransportChannel

            if (connected) {
                channel = TransportChannel.WIFI_DIRECT
            } else if (bluetoothTransport.isAvailable) {
                Log.d(TAG, "Wi-Fi Direct failed, falling back to Bluetooth")
                channel = TransportChannel.BLUETOOTH
                bluetoothTransport.connectToDevice(targetDevice) { /* result handled in flow */ }
            } else {
                _sessionState.value = TransferSessionState.Failed("No transport available")
                return@launch
            }

            deviceRepository.setActiveChannel(channel)

            // Get socket streams based on active channel
            val (inputStream, outputStream) = getStreams(channel, isSender = true) ?: run {
                _sessionState.value = TransferSessionState.Failed("Could not open connection")
                return@launch
            }

            // Send PIN for verification
            try {
                val dos = DataOutputStream(outputStream)
                dos.writeUTF(pin)
                dos.flush()
                _sessionState.value = TransferSessionState.PinVerified
            } catch (e: Exception) {
                _sessionState.value = TransferSessionState.Failed("PIN exchange failed: ${e.message}")
                return@launch
            }

            // Transfer each file
            val startTime = System.currentTimeMillis()
            for ((index, fileItem) in files.withIndex()) {
                val fileInput = fileRepository.openInputStream(fileItem)
                if (fileInput == null) {
                    Log.e(TAG, "Cannot open file: ${fileItem.name}")
                    continue
                }

                _sessionState.value = TransferSessionState.Transferring(
                    currentFileIndex = index,
                    progress = TransferProgress(currentFileName = fileItem.name, totalBytes = fileItem.sizeBytes)
                )

                val success = chunkedTransfer.sendFile(
                    fileName = fileItem.name,
                    fileSize = fileItem.sizeBytes,
                    inputStream = fileInput,
                    outputStream = outputStream,
                    ackInputStream = inputStream,
                )
                fileInput.close()

                // Persist transfer record
                val durationMs = System.currentTimeMillis() - startTime
                val record = TransferRecord(
                    sessionId = session.sessionId,
                    fileName = fileItem.name,
                    fileSize = fileItem.sizeBytes,
                    compressedSize = fileItem.compressedSizeBytes,
                    mimeType = fileItem.mimeType,
                    localPath = fileItem.uri,
                    peerId = targetDevice.id,
                    peerName = targetDevice.name,
                    direction = TransferDirection.SEND,
                    channel = channel,
                    status = if (success) TransferStatus.COMPLETED else TransferStatus.FAILED,
                    durationMs = durationMs,
                )
                transferRepository.insertRecord(record)

                if (!success) {
                    _sessionState.value = TransferSessionState.Failed("Transfer failed: ${fileItem.name}")
                    return@launch
                }
            }

            _sessionState.value = TransferSessionState.Completed
            Log.d(TAG, "All files sent successfully")
        }
    }

    // ──────────────────────────── Receive Flow ────────────────────────────

    /**
     * Starts listening for incoming transfers.
     *
     * Opens a ServerSocket on [TRANSFER_PORT] and waits for a sender to connect.
     * Once the PIN is verified, receives files and saves them to the receive directory.
     */
    fun startReceiving(expectedPin: String, onSessionReady: (TransferSession) -> Unit) {
        transferJob = scope.launch {
            _sessionState.value = TransferSessionState.AwaitingPin

            try {
                val serverSocket = ServerSocket(TRANSFER_PORT)
                serverSocket.soTimeout = SOCKET_TIMEOUT_MS
                Log.d(TAG, "Listening on port $TRANSFER_PORT")

                val clientSocket = serverSocket.accept()
                Log.d(TAG, "Sender connected: ${clientSocket.inetAddress.hostAddress}")

                val inputStream = clientSocket.getInputStream()
                val outputStream = clientSocket.getOutputStream()

                // Verify PIN
                val dis = DataInputStream(inputStream)
                val receivedPin = dis.readUTF()

                if (receivedPin != expectedPin) {
                    Log.e(TAG, "PIN mismatch: expected=$expectedPin, received=$receivedPin")
                    _sessionState.value = TransferSessionState.Failed("Incorrect PIN")
                    clientSocket.close()
                    serverSocket.close()
                    return@launch
                }
                _sessionState.value = TransferSessionState.PinVerified

                val session = TransferSession(
                    pin = expectedPin,
                    peerId = clientSocket.inetAddress.hostAddress ?: "unknown",
                    peerName = "Sender",
                    direction = TransferDirection.RECEIVE,
                    files = emptyList(),
                    channel = TransportChannel.WIFI_DIRECT,
                )
                currentSession = session
                onSessionReady(session)

                // Receive files until socket closes
                val receiveDir = fileRepository.getReceiveDirectory()
                var fileIndex = 0

                while (isActive && clientSocket.isConnected) {
                    val outFile = File(receiveDir, "received_${System.currentTimeMillis()}")
                    val fileOut = FileOutputStream(outFile)

                    val result = chunkedTransfer.receiveFile(
                        inputStream = inputStream,
                        fileOutputStream = fileOut,
                        ackOutputStream = outputStream,
                    )
                    fileOut.close()

                    if (result != null) {
                        val (fileName, bytesReceived) = result
                        // Rename to actual file name
                        val actualFile = File(receiveDir, fileName)
                        if (actualFile.exists()) {
                            actualFile.delete()
                        }
                        outFile.renameTo(actualFile)

                        _sessionState.value = TransferSessionState.Transferring(
                            currentFileIndex = fileIndex,
                            progress = TransferProgress(
                                bytesTransferred = bytesReceived,
                                totalBytes = bytesReceived,
                                currentFileName = fileName,
                            )
                        )

                        val record = TransferRecord(
                            sessionId = session.sessionId,
                            fileName = fileName,
                            fileSize = bytesReceived,
                            mimeType = "application/octet-stream",
                            localPath = actualFile.absolutePath,
                            peerId = session.peerId,
                            peerName = session.peerName,
                            direction = TransferDirection.RECEIVE,
                            channel = TransportChannel.WIFI_DIRECT,
                            status = TransferStatus.COMPLETED,
                        )
                        transferRepository.insertRecord(record)
                        fileIndex++
                    } else {
                        // No more files or error
                        break
                    }
                }

                clientSocket.close()
                serverSocket.close()
                _sessionState.value = TransferSessionState.Completed
                Log.d(TAG, "Receive session complete ($fileIndex files)")

            } catch (e: Exception) {
                Log.e(TAG, "startReceiving failed", e)
                _sessionState.value = TransferSessionState.Failed(e.message ?: "Receive failed")
            }
        }
    }

    // ──────────────────────────── Channel Helpers ────────────────────────────

    private suspend fun attemptWifiDirectConnection(device: NearbyDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            wifiDirectManager.connectToDevice(device) { success ->
                if (cont.isActive) cont.resume(success) {}
            }
        }

    private fun getStreams(channel: TransportChannel, isSender: Boolean): Pair<InputStream, OutputStream>? {
        return when (channel) {
            TransportChannel.WIFI_DIRECT -> {
                val wifiState = wifiDirectManager.state.value
                if (wifiState is WifiDirectState.Connected) {
                    try {
                        val socket = if (isSender) {
                            Socket().apply {
                                connect(InetSocketAddress(wifiState.groupOwnerAddress, TRANSFER_PORT), SOCKET_TIMEOUT_MS)
                            }
                        } else {
                            // Receiver already has a ServerSocket — handled in startReceiving
                            return null
                        }
                        socket.getInputStream() to socket.getOutputStream()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open Wi-Fi Direct socket", e)
                        null
                    }
                } else null
            }

            TransportChannel.BLUETOOTH -> {
                val ins = bluetoothTransport.getInputStream()
                val outs = bluetoothTransport.getOutputStream()
                if (ins != null && outs != null) ins to outs else null
            }

            TransportChannel.UNKNOWN -> null
        }
    }

    // ──────────────────────────── Cancel / Cleanup ────────────────────────────

    fun cancelTransfer() {
        transferJob?.cancel()
        _sessionState.value = TransferSessionState.Cancelled
        chunkedTransfer.resetProgress()
    }

    fun teardown() {
        cancelTransfer()
        stopDiscovery()
        wifiDirectManager.teardown()
        nsdDiscoveryManager.teardown()
        bluetoothTransport.teardown()
        scope.cancel()
    }
}
