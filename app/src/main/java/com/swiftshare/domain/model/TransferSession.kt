package com.swiftshare.domain.model

import java.util.UUID

/**
 * Represents an active file transfer session between two peers.
 *
 * A session is identified by a 6-digit PIN that both parties must agree on
 * before any file bytes are exchanged. The PIN is valid for [PIN_TIMEOUT_MS].
 */
data class TransferSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val pin: String,
    val peerId: String,
    val peerName: String,
    val direction: TransferDirection,
    val files: List<FileItem>,
    val channel: TransportChannel,
    val state: TransferSessionState = TransferSessionState.AWAITING_PIN,
    val createdAtMs: Long = System.currentTimeMillis(),
) {
    companion object {
        const val PIN_TIMEOUT_MS = 30_000L

        /** Generates a cryptographically adequate 6-digit PIN. */
        fun generatePin(): String = (100_000..999_999).random().toString()
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() - createdAtMs > PIN_TIMEOUT_MS
}

enum class TransferDirection { SEND, RECEIVE }

sealed class TransferSessionState {
    data object AwaitingPin : TransferSessionState()
    data object PinVerified : TransferSessionState()
    data class Transferring(val currentFileIndex: Int, val progress: TransferProgress) : TransferSessionState()
    data object Completed : TransferSessionState()
    data class Failed(val reason: String) : TransferSessionState()
    data object Cancelled : TransferSessionState()
}
