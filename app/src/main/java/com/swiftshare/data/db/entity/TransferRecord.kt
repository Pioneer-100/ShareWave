package com.swiftshare.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.swiftshare.domain.model.TransportChannel
import com.swiftshare.domain.model.TransferDirection

/**
 * Room entity representing a completed or in-progress file transfer record.
 * Indexed on [peerId] and [timestamp] for efficient history queries.
 */
@Entity(
    tableName = "transfer_records",
    indices = [
        Index(value = ["peer_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
    ]
)
data class TransferRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "compressed_size")
    val compressedSize: Long = fileSize,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "local_path")
    val localPath: String,              // absolute path on this device

    @ColumnInfo(name = "peer_id")
    val peerId: String,

    @ColumnInfo(name = "peer_name")
    val peerName: String,

    @ColumnInfo(name = "direction")
    val direction: TransferDirection,

    @ColumnInfo(name = "channel")
    val channel: TransportChannel,

    @ColumnInfo(name = "status")
    val status: TransferStatus,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,

    @ColumnInfo(name = "speed_bps")
    val speedBps: Long = 0L,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
) {
    val compressionSavingsPercent: Int
        get() = if (fileSize > 0) ((1f - compressedSize / fileSize.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
}

enum class TransferStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED;
}
