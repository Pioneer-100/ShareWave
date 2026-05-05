package com.swiftshare.domain.model

/**
 * Represents a nearby peer device discovered via NSD or Wi-Fi Direct.
 *
 * @param id            Unique identifier (MAC address or NSD-assigned UUID)
 * @param name          Human-readable display name (device model or user-set name)
 * @param ipAddress     IP address when connected over Wi-Fi Direct group; null otherwise
 * @param rssi          Signal strength in dBm (used for device scoring)
 * @param channel       Active transport channel (Wi-Fi Direct or Bluetooth)
 * @param isConnected   Whether an active socket connection exists with this peer
 * @param throughputKbps Historical throughput with this device (Kbps); 0 if unknown
 */
data class NearbyDevice(
    val id: String,
    val name: String,
    val ipAddress: String? = null,
    val rssi: Int = Int.MIN_VALUE,
    val channel: TransportChannel = TransportChannel.UNKNOWN,
    val isConnected: Boolean = false,
    val throughputKbps: Long = 0L,
) {
    /** Composite score used by the device scorer (Phase 2 AI will refine this). */
    val score: Float
        get() {
            val rssiScore = if (rssi != Int.MIN_VALUE) ((rssi + 100f) / 100f).coerceIn(0f, 1f) else 0f
            val channelBonus = if (channel == TransportChannel.WIFI_DIRECT) 0.4f else 0f
            val throughputScore = (throughputKbps / 50_000f).coerceIn(0f, 0.4f) // max bonus at 50 Mbps
            return rssiScore * 0.2f + channelBonus + throughputScore
        }
}

enum class TransportChannel {
    WIFI_DIRECT,
    BLUETOOTH,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            WIFI_DIRECT -> "Wi-Fi Direct"
            BLUETOOTH -> "Bluetooth"
            UNKNOWN -> "Scanning…"
        }
}
