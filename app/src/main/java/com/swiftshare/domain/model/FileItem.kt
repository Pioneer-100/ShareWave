package com.swiftshare.domain.model

/**
 * A single file queued for transfer.
 */
data class FileItem(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val compressedSizeBytes: Long = sizeBytes, // updated by AI compression pipeline (Phase 2)
    val fileType: FileType = FileType.fromMime(mimeType),
) {
    val compressionRatio: Float
        get() = if (sizeBytes > 0) compressedSizeBytes / sizeBytes.toFloat() else 1f

    val savingsPercent: Int
        get() = ((1f - compressionRatio) * 100).toInt().coerceIn(0, 100)
}

enum class FileType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER;

    companion object {
        fun fromMime(mime: String): FileType = when {
            mime.startsWith("image/") -> IMAGE
            mime.startsWith("video/") -> VIDEO
            mime.startsWith("audio/") -> AUDIO
            mime in listOf("application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain", "text/csv") -> DOCUMENT
            mime in listOf("application/zip", "application/x-rar-compressed",
                "application/x-7z-compressed", "application/gzip") -> ARCHIVE
            else -> OTHER
        }
    }
}
