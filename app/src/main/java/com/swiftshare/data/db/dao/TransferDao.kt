package com.swiftshare.data.db.dao

import androidx.room.*
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.data.db.entity.TransferStatus
import com.swiftshare.domain.model.TransferDirection
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    // ── Insert / Update ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TransferRecord): Long

    @Update
    suspend fun update(record: TransferRecord)

    @Query("UPDATE transfer_records SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransferStatus, errorMessage: String? = null)

    @Query("UPDATE transfer_records SET status = :status, duration_ms = :durationMs, speed_bps = :speedBps WHERE id = :id")
    suspend fun markCompleted(id: Long, status: TransferStatus, durationMs: Long, speedBps: Long)

    // ── Queries ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM transfer_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransferRecord>>

    @Query("SELECT * FROM transfer_records WHERE direction = :direction ORDER BY timestamp DESC")
    fun observeByDirection(direction: TransferDirection): Flow<List<TransferRecord>>

    @Query("SELECT * FROM transfer_records WHERE status = :status ORDER BY timestamp DESC")
    fun observeByStatus(status: TransferStatus): Flow<List<TransferRecord>>

    @Query("""
        SELECT * FROM transfer_records
        WHERE file_name LIKE '%' || :query || '%'
           OR peer_name LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<TransferRecord>>

    @Query("SELECT * FROM transfer_records WHERE id = :id")
    suspend fun getById(id: Long): TransferRecord?

    @Query("SELECT * FROM transfer_records WHERE session_id = :sessionId")
    suspend fun getBySessionId(sessionId: String): List<TransferRecord>

    @Query("SELECT * FROM transfer_records WHERE peer_id = :peerId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByPeer(peerId: String, limit: Int = 50): List<TransferRecord>

    // ── Stats ────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM transfer_records WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(file_size), 0) FROM transfer_records WHERE status = 'COMPLETED' AND direction = 'SEND'")
    fun observeTotalBytesSent(): Flow<Long>

    @Query("SELECT COALESCE(SUM(file_size - compressed_size), 0) FROM transfer_records WHERE status = 'COMPLETED'")
    fun observeTotalBytesSaved(): Flow<Long>

    // ── Delete ───────────────────────────────────────────────────────────────

    @Delete
    suspend fun delete(record: TransferRecord)

    @Query("DELETE FROM transfer_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transfer_records WHERE timestamp < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}
