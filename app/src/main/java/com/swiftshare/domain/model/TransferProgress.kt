package com.swiftshare.domain.model

/**
 * Real-time progress snapshot for an ongoing file transfer.
 *
 * @param bytesTransferred  Total bytes moved across the wire so far
 * @param totalBytes        Total bytes to transfer (sum of all files in session)
 * @param speedBps          Current throughput in bytes/second (rolling 1-second average)
 * @param etaSeconds        Estimated seconds remaining (-1 if unknown)
 * @param currentFileName   Name of the file currently being transferred
 * @param chunkIndex        Current chunk index within the current file
 * @param totalChunks       Total chunks in the current file
 */
data class TransferProgress(
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBps: Long = 0L,
    val etaSeconds: Long = -1L,
    val currentFileName: String = "",
    val chunkIndex: Int = 0,
    val totalChunks: Int = 0,
) {
    val overallPercent: Float
        get() = if (totalBytes > 0) (bytesTransferred / totalBytes.toFloat()) * 100f else 0f

    val filePercent: Float
        get() = if (totalChunks > 0) (chunkIndex / totalChunks.toFloat()) * 100f else 0f

    val speedDisplayMbps: String
        get() = "%.1f MB/s".format(speedBps / 1_048_576.0)

    val etaDisplay: String
        get() = when {
            etaSeconds < 0 -> "Calculating…"
            etaSeconds < 60 -> "${etaSeconds}s left"
            else -> "${etaSeconds / 60}m ${etaSeconds % 60}s left"
        }
}
