package com.swiftshare.domain.repository

import android.net.Uri
import com.swiftshare.domain.model.FileItem

interface FileRepository {
    /** Resolves a content URI to a [FileItem] with name, size, and MIME type. */
    suspend fun resolveUri(uri: Uri): FileItem?

    /** Resolves multiple URIs into a list of [FileItem]s. */
    suspend fun resolveUris(uris: List<Uri>): List<FileItem>

    /**
     * Returns the absolute path to the app's default receive directory,
     * creating it if it does not exist.
     */
    fun getReceiveDirectory(): String

    /**
     * Opens the raw bytes of [fileItem] as a stream.
     * Callers are responsible for closing the returned stream.
     */
    fun openInputStream(fileItem: FileItem): java.io.InputStream?
}
