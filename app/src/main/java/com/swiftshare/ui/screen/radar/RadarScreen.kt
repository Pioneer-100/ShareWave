package com.swiftshare.ui.screen.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swiftshare.domain.model.NearbyDevice
import com.swiftshare.ui.component.ChannelStatusChip
import com.swiftshare.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    mode: String,
    onDeviceSelected: (NearbyDevice) -> Unit,
    onBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Start scanning when screen appears
    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ──────── Top bar ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.weight(1f))
            ChannelStatusChip(channel = state.activeChannel)
        }

        // ──────── Title ────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (mode == "send") "Looking for receivers…" else "Waiting for sender…",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${state.devices.size} device${if (state.devices.size != 1) "s" else ""} nearby",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        // ──────── Radar canvas ────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            RadarCanvas(
                devices = state.devices,
                isScanning = state.isScanning,
                onDeviceTapped = onDeviceSelected,
            )

            // Center dot (this device)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(PrimaryCyan),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ──────── Device list below radar ────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.devices.forEach { device ->
                DeviceListItem(
                    device = device,
                    onClick = { onDeviceSelected(device) },
                )
            }
        }
    }
}

// ──────────────────────── Animated Radar Canvas ────────────────────────

@Composable
private fun RadarCanvas(
    devices: List<NearbyDevice>,
    isScanning: Boolean,
    onDeviceTapped: (NearbyDevice) -> Unit,
) {
    // Sweep rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    // Pulse animation for rings
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // Draw concentric rings
        val ringCount = 4
        for (i in 1..ringCount) {
            val radius = maxRadius * i / ringCount
            drawCircle(
                color = RadarRingColor.copy(alpha = pulseAlpha * (1f - i * 0.15f)),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f),
            )
        }

        // Draw cross-hairs
        drawLine(
            color = RadarRingColor,
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 0.8f,
        )
        drawLine(
            color = RadarRingColor,
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 0.8f,
        )

        // Draw sweep line
        if (isScanning) {
            rotate(sweepAngle, pivot = center) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, RadarSweepColor),
                        start = center,
                        end = Offset(center.x, center.y - maxRadius),
                    ),
                    start = center,
                    end = Offset(center.x, center.y - maxRadius),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                )

                // Sweep trail (fading arc)
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.15f to RadarSweepColor.copy(alpha = 0.15f),
                        0.25f to Color.Transparent,
                    ),
                    startAngle = -90f,
                    sweepAngle = -60f,
                    useCenter = true,
                    size = size,
                )
            }
        }

        // Draw device dots
        devices.forEachIndexed { index, device ->
            val angle = (index * 137.5f) * (Math.PI / 180f) // golden angle distribution
            val distance = maxRadius * 0.3f + (maxRadius * 0.5f * (index % 3 + 1) / 4f)
            val dotX = center.x + (cos(angle) * distance).toFloat()
            val dotY = center.y + (sin(angle) * distance).toFloat()

            // Glow
            drawCircle(
                color = RadarDotGlow,
                radius = 14f,
                center = Offset(dotX, dotY),
            )
            // Dot
            drawCircle(
                color = RadarDotColor,
                radius = 8f,
                center = Offset(dotX, dotY),
            )
        }
    }
}

// ──────────────────────── Device list item ────────────────────────

@Composable
private fun DeviceListItem(
    device: NearbyDevice,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryAccent, PrimaryCyan))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = device.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeutralWhite,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = device.channel.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Signal strength indicator
            if (device.rssi != Int.MIN_VALUE) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
