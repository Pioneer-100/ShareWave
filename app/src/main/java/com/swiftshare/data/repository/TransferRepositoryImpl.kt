package com.swiftshare.data.repository

import com.swiftshare.data.db.dao.TransferDao
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.data.db.entity.TransferStatus
import com.swiftshare.di.IoDispatcher
import com.swiftshare.domain.repository.TransferRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val dao: TransferDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransferRepository {

    override fun observeAll(): Flow<List<TransferRecord>> = dao.observeAll()

    override fun search(query: String): Flow<List<TransferRecord>> = dao.search(query)

    override fun observeTotalBytesSent(): Flow<Long> = dao.observeTotalBytesSent()

    override fun observeTotalBytesSaved(): Flow<Long> = dao.observeTotalBytesSaved()

    override suspend fun insertRecord(record: TransferRecord): Long =
        withContext(ioDispatcher) { dao.insert(record) }

    override suspend fun updateStatus(id: Long, status: TransferStatus, error: String?) =
        withContext(ioDispatcher) { dao.updateStatus(id, status, error) }

    override suspend fun markCompleted(id: Long, durationMs: Long, speedBps: Long) =
        withContext(ioDispatcher) {
            dao.markCompleted(id, TransferStatus.COMPLETED, durationMs, speedBps)
        }

    override suspend fun deleteById(id: Long) =
        withContext(ioDispatcher) { dao.deleteById(id) }

    override suspend fun pruneOldRecords(retentionDays: Int) =
        withContext(ioDispatcher) {
            val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            dao.deleteOlderThan(cutoffMs)
        }
}
