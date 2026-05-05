package com.swiftshare.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.swiftshare.domain.model.TransportChannel
import com.swiftshare.ui.theme.*

/**
 * Always-visible chip showing the currently active transport channel.
 * Pulses with a color that reflects the channel state.
 */
@Composable
fun ChannelStatusChip(
    channel: TransportChannel,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (channel) {
        TransportChannel.WIFI_DIRECT -> PrimaryCyan to "Wi-Fi Direct"
        TransportChannel.BLUETOOTH -> SecondaryPurple to "Bluetooth"
        TransportChannel.UNKNOWN -> SecondaryAmber to "Scanning…"
    }

    val animatedColor by animateColorAsState(targetValue = color, label = "chipColor")

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = animatedColor.copy(alpha = 0.15f),
        contentColor = animatedColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Pulsing dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(animatedColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = animatedColor,
            )
        }
    }
}
