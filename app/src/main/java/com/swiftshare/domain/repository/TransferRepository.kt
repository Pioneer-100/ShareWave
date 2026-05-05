package com.swiftshare.domain.repository

import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.data.db.entity.TransferStatus
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun observeAll(): Flow<List<TransferRecord>>
    fun search(query: String): Flow<List<TransferRecord>>
    fun observeTotalBytesSent(): Flow<Long>
    fun observeTotalBytesSaved(): Flow<Long>
    suspend fun insertRecord(record: TransferRecord): Long
    suspend fun updateStatus(id: Long, status: TransferStatus, error: String? = null)
    suspend fun markCompleted(id: Long, durationMs: Long, speedBps: Long)
    suspend fun deleteById(id: Long)
    suspend fun pruneOldRecords(retentionDays: Int)
}
