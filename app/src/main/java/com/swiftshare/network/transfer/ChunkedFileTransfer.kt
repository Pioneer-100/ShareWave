package com.swiftshare.network.transfer

import android.util.Log
import com.swiftshare.domain.model.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import kotlin.coroutines.coroutineContext

/**
 * Chunked file transfer over raw socket streams.
 *
 * Protocol per file:
 *   1. Sender writes: [fileName: UTF] [fileSize: Long] [chunkCount: Int]
 *   2. For each chunk:
 *       Sender writes: [chunkSize: Int] [chunkData: ByteArray] [crc32: Long]
 *       Receiver reads, verifies CRC32, writes ACK byte (0x01 = OK, 0x00 = retry)
 *   3. After all chunks: sender writes END marker (chunkSize = -1)
 *
 * Chunk size defaults to 256 KB (configurable via [chunkSizeBytes]).
 * RAM ceiling is maintained by reusing a single buffer.
 */
class ChunkedFileTransfer(
    private val chunkSizeBytes: Int = 262_144, // 256 KB
) {
    companion object {
        private const val TAG = "ChunkedTransfer"
        private const val ACK_OK: Byte = 0x01
        private const val ACK_RETRY: Byte = 0x00
        private const val END_MARKER = -1
        private const val MAX_RETRIES_PER_CHUNK = 3
        private const val SPEED_WINDOW_MS = 1_000L
    }

    private val _progress = MutableStateFlow(TransferProgress())
    val progress: StateFlow<TransferProgress> = _progress.asStateFlow()

    // ──────────────────────────── Send ────────────────────────────

    /**
     * Sends a single file over [outputStream] with progress tracking.
     *
     * @param fileName       Display name of the file
     * @param fileSize       Total size in bytes
     * @param inputStream    Source data
     * @param outputStream   Socket output (to receiver)
     * @param ackInputStream Socket input (for receiver ACK bytes)
     */
    suspend fun sendFile(
        fileName: String,
        fileSize: Long,
        inputStream: InputStream,
        outputStream: OutputStream,
        ackInputStream: InputStream,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dos = DataOutputStream(outputStream)
            val dis = DataInputStream(ackInputStream)
            val buffer = ByteArray(chunkSizeBytes)
            val crc = CRC32()

            val totalChunks = ((fileSize + chunkSizeBytes - 1) / chunkSizeBytes).toInt()

            // Write file header
            dos.writeUTF(fileName)
            dos.writeLong(fileSize)
            dos.writeInt(totalChunks)
            dos.flush()

            var bytesTransferred = 0L
            var chunkIndex = 0
            var speedWindowStart = System.currentTimeMillis()
            var speedWindowBytes = 0L

            while (coroutineContext.isActive) {
                val bytesRead = inputStream.read(buffer, 0, chunkSizeBytes)
                if (bytesRead <= 0) break

                var sent = false
                var retries = 0

                while (!sent && retries < MAX_RETRIES_PER_CHUNK) {
                    crc.reset()
                    crc.update(buffer, 0, bytesRead)

                    dos.writeInt(bytesRead)
                    dos.write(buffer, 0, bytesRead)
                    dos.writeLong(crc.value)
                    dos.flush()

                    val ack = dis.readByte()
                    if (ack == ACK_OK) {
                        sent = true
                    } else {
                        retries++
                        Log.w(TAG, "Chunk $chunkIndex CRC mismatch, retry $retries/$MAX_RETRIES_PER_CHUNK")
                    }
                }

                if (!sent) {
                    Log.e(TAG, "Chunk $chunkIndex failed after $MAX_RETRIES_PER_CHUNK retries")
                    return@withContext false
                }

                bytesTransferred += bytesRead
                chunkIndex++
                speedWindowBytes += bytesRead

                // Update progress with rolling speed calculation
                val now = System.currentTimeMillis()
                val elapsed = now - speedWindowStart
                val speedBps = if (elapsed > 0) (speedWindowBytes * 1000 / elapsed) else 0L
                val remaining = fileSize - bytesTransferred
                val etaSeconds = if (speedBps > 0) remaining / speedBps else -1L

                if (elapsed >= SPEED_WINDOW_MS) {
                    speedWindowStart = now
                    speedWindowBytes = 0L
                }

                _progress.value = TransferProgress(
                    bytesTransferred = bytesTransferred,
                    totalBytes = fileSize,
                    speedBps = speedBps,
                    etaSeconds = etaSeconds,
                    currentFileName = fileName,
                    chunkIndex = chunkIndex,
                    totalChunks = totalChunks,
                )
            }

            // Write END marker
            dos.writeInt(END_MARKER)
            dos.flush()

            Log.d(TAG, "Send complete: $fileName ($bytesTransferred bytes, $chunkIndex chunks)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendFile failed", e)
            false
        }
    }

    // ──────────────────────────── Receive ────────────────────────────

    /**
     * Receives a single file from [inputStream] and writes to [fileOutputStream].
     *
     * @param inputStream      Socket input (from sender)
     * @param fileOutputStream Destination file output
     * @param ackOutputStream  Socket output (for sending ACK bytes)
     * @return Pair of (fileName, totalBytesReceived), or null on failure
     */
    suspend fun receiveFile(
        inputStream: InputStream,
        fileOutputStream: OutputStream,
        ackOutputStream: OutputStream,
    ): Pair<String, Long>? = withContext(Dispatchers.IO) {
        try {
            val dis = DataInputStream(inputStream)
            val dos = DataOutputStream(ackOutputStream)
            val crc = CRC32()

            // Read file header
            val fileName = dis.readUTF()
            val fileSize = dis.readLong()
            val totalChunks = dis.readInt()

            Log.d(TAG, "Receiving: $fileName ($fileSize bytes, $totalChunks chunks)")

            var bytesReceived = 0L
            var chunkIndex = 0
            var speedWindowStart = System.currentTimeMillis()
            var speedWindowBytes = 0L

            while (coroutineContext.isActive) {
                val chunkSize = dis.readInt()
                if (chunkSize == END_MARKER) break

                val chunkData = ByteArray(chunkSize)
                dis.readFully(chunkData)
                val expectedCrc = dis.readLong()

                // Verify CRC32
                crc.reset()
                crc.update(chunkData, 0, chunkSize)

                if (crc.value == expectedCrc) {
                    fileOutputStream.write(chunkData, 0, chunkSize)
                    dos.writeByte(ACK_OK.toInt())
                    dos.flush()

                    bytesReceived += chunkSize
                    chunkIndex++
                    speedWindowBytes += chunkSize

                    val now = System.currentTimeMillis()
                    val elapsed = now - speedWindowStart
                    val speedBps = if (elapsed > 0) (speedWindowBytes * 1000 / elapsed) else 0L
                    val remaining = fileSize - bytesReceived
                    val etaSeconds = if (speedBps > 0) remaining / speedBps else -1L

                    if (elapsed >= SPEED_WINDOW_MS) {
                        speedWindowStart = now
                        speedWindowBytes = 0L
                    }

                    _progress.value = TransferProgress(
                        bytesTransferred = bytesReceived,
                        totalBytes = fileSize,
                        speedBps = speedBps,
                        etaSeconds = etaSeconds,
                        currentFileName = fileName,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                    )
                } else {
                    Log.w(TAG, "CRC mismatch on chunk $chunkIndex — requesting retry")
                    dos.writeByte(ACK_RETRY.toInt())
                    dos.flush()
                }
            }

            fileOutputStream.flush()
            Log.d(TAG, "Receive complete: $fileName ($bytesReceived bytes)")
            fileName to bytesReceived
        } catch (e: Exception) {
            Log.e(TAG, "receiveFile failed", e)
            null
        }
    }

    fun resetProgress() {
        _progress.value = TransferProgress()
    }
}
