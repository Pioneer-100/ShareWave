package com.swiftshare.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.swiftshare.R
import com.swiftshare.domain.model.FileItem
import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.domain.model.TransferSession
import com.swiftshare.domain.model.TransportChannel
import com.swiftshare.domain.model.TransferDirection
import com.swiftshare.network.transfer.TransferEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker for continuing file transfers when the app is backgrounded.
 *
 * Receives the sessionId, peerId, and file URIs via [Data].
 * Runs as a foreground service with a progress notification.
 */
@HiltWorker
class TransferWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transferEngine: TransferEngine,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TransferWorker"
        const val CHANNEL_ID = "swiftshare_transfer"
        const val NOTIFICATION_ID = 42
        const val KEY_SESSION_ID = "session_id"
        const val KEY_PEER_ID = "peer_id"
        const val KEY_PEER_NAME = "peer_name"
        const val KEY_PIN = "pin"
        const val KEY_DIRECTION = "direction"
        const val KEY_FILE_URIS = "file_uris"
        const val KEY_FILE_NAMES = "file_names"
        const val KEY_FILE_SIZES = "file_sizes"
        const val KEY_FILE_MIMES = "file_mimes"

        fun buildWorkRequest(session: TransferSession): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(KEY_SESSION_ID, session.sessionId)
                .putString(KEY_PEER_ID, session.peerId)
                .putString(KEY_PEER_NAME, session.peerName)
                .putString(KEY_PIN, session.pin)
                .putString(KEY_DIRECTION, session.direction.name)
                .putStringArray(KEY_FILE_URIS, session.files.map { it.uri }.toTypedArray())
                .putStringArray(KEY_FILE_NAMES, session.files.map { it.name }.toTypedArray())
                .putLongArray(KEY_FILE_SIZES, session.files.map { it.sizeBytes }.toLongArray())
                .putStringArray(KEY_FILE_MIMES, session.files.map { it.mimeType }.toTypedArray())
                .build()

            return OneTimeWorkRequestBuilder<TransferWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // offline-first
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "TransferWorker started")

        createNotificationChannel()
        setForeground(createForegroundInfo("Preparing transfer…"))

        val direction = inputData.getString(KEY_DIRECTION)
        val pin = inputData.getString(KEY_PIN) ?: return Result.failure()

        return try {
            when (direction) {
                TransferDirection.SEND.name -> handleSend(pin)
                TransferDirection.RECEIVE.name -> handleReceive(pin)
                else -> Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.failure()
        }
    }

    private suspend fun handleSend(pin: String): Result {
        val peerId = inputData.getString(KEY_PEER_ID) ?: return Result.failure()
        val peerName = inputData.getString(KEY_PEER_NAME) ?: "Unknown"
        val uris = inputData.getStringArray(KEY_FILE_URIS) ?: return Result.failure()
        val names = inputData.getStringArray(KEY_FILE_NAMES) ?: return Result.failure()
        val sizes = inputData.getLongArray(KEY_FILE_SIZES) ?: return Result.failure()
        val mimes = inputData.getStringArray(KEY_FILE_MIMES) ?: return Result.failure()

        val files = uris.indices.map { i ->
            FileItem(uri = uris[i], name = names[i], mimeType = mimes[i], sizeBytes = sizes[i])
        }

        val device = NearbyDevice(id = peerId, name = peerName, channel = TransportChannel.WIFI_DIRECT)

        transferEngine.startSending(device, files, pin) { /* session created */ }

        // Observe progress for notification updates
        transferEngine.transferProgress.collect { progress ->
            if (progress.totalBytes > 0) {
                setForeground(
                    createForegroundInfo(
                        "Sending ${progress.currentFileName} — ${progress.overallPercent.toInt()}%"
                    )
                )
            }
        }

        return Result.success()
    }

    private suspend fun handleReceive(pin: String): Result {
        transferEngine.startReceiving(pin) { /* session ready */ }

        transferEngine.transferProgress.collect { progress ->
            if (progress.totalBytes > 0) {
                setForeground(
                    createForegroundInfo(
                        "Receiving ${progress.currentFileName} — ${progress.overallPercent.toInt()}%"
                    )
                )
            }
        }

        return Result.success()
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("SwiftShare Transfer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Transfers",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active file transfer progress"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
