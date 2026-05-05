package com.swiftshare.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.swiftshare.di.IoDispatcher
import com.swiftshare.domain.model.FileItem
import com.swiftshare.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileRepository {

    override suspend fun resolveUri(uri: Uri): FileItem? = withContext(ioDispatcher) {
        try {
            val cr: ContentResolver = context.contentResolver
            val cursor = cr.query(uri, null, null, null, null) ?: return@withContext null
            cursor.use { c ->
                if (!c.moveToFirst()) return@withContext null
                val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) c.getString(nameIdx) else uri.lastPathSegment ?: "file"
                val size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L
                val mime = cr.getType(uri)
                    ?: MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(name.substringAfterLast('.', ""))
                    ?: "application/octet-stream"
                FileItem(uri = uri.toString(), name = name, mimeType = mime, sizeBytes = size)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun resolveUris(uris: List<Uri>): List<FileItem> =
        uris.mapNotNull { resolveUri(it) }

    override fun getReceiveDirectory(): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SwiftShare"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun openInputStream(fileItem: FileItem): InputStream? =
        try {
            context.contentResolver.openInputStream(Uri.parse(fileItem.uri))
        } catch (e: Exception) {
            null
        }
}
