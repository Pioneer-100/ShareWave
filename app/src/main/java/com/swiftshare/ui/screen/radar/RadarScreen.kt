package com.swiftshare.ui.screen.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
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

            // Dynamic pulsing center halo
            val infiniteTransition = rememberInfiniteTransition(label = "centerPulse")
            val centerPulse by infiniteTransition.animateFloat(
                initialValue = 16.dp.value,
                targetValue = 36.dp.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "centerPulse"
            )
            Box(
                modifier = Modifier
                    .size(centerPulse.dp)
                    .clip(CircleShape)
                    .background(PrimaryCyan.copy(alpha = (1f - (centerPulse - 16) / 20).coerceIn(0f, 1f) * 0.3f)),
            )

            // Inner glowing ring
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PrimaryCyan.copy(alpha = 0.15f)),
            )

            // Center dot (this device) with radial gradient
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeutralWhite, PrimaryCyan),
                            radius = 25f
                        )
                    ),
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
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    // Sweep rotation animation
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    // Three phase-shifted wave ripples expanding outward
    val waveProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave1"
    )
    val waveProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, delayMillis = 1333, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave2"
    )
    val waveProgress3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, delayMillis = 2666, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave3"
    )

    // Breathing glow animation for discovered device dots
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // 1. Draw tech mesh grid dots
        val gridSize = 24.dp.toPx()
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (col in 0..cols) {
            for (row in 0..rows) {
                val ptX = col * gridSize
                val ptY = row * gridSize
                val dist = Offset(ptX, ptY).minus(center).getDistance()
                if (dist < maxRadius) {
                    drawCircle(
                        color = RadarMeshColor,
                        radius = 1.5f,
                        center = Offset(ptX, ptY),
                    )
                }
            }
        }

        // 2. Draw static target faint circles
        val targetRings = 3
        for (i in 1..targetRings) {
            val radius = maxRadius * i / targetRings
            drawCircle(
                color = RadarRingColor.copy(alpha = 0.05f),
                radius = radius,
                center = center,
                style = Stroke(width = 1f),
            )
        }

        // 3. Draw cross-hairs
        drawLine(
            color = RadarRingColor.copy(alpha = 0.1f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 0.8f,
        )
        drawLine(
            color = RadarRingColor.copy(alpha = 0.1f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 0.8f,
        )

        // 4. Draw multi-layered expanding wave ripples
        if (isScanning) {
            listOf(waveProgress1, waveProgress2, waveProgress3).forEach { progress ->
                val radius = maxRadius * progress
                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.22f
                drawCircle(
                    color = PrimaryCyan.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f),
                )
            }
        }

        // 5. Draw sweep line and trail
        if (isScanning) {
            rotate(sweepAngle, pivot = center) {
                // Fading wide sweep trail (arc of 90 degrees behind the line)
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.6f to Color.Transparent,
                        0.8f to RadarSweepColor.copy(alpha = 0.05f),
                        1f to RadarSweepColor.copy(alpha = 0.35f),
                    ),
                    startAngle = -90f,
                    sweepAngle = -90f,
                    useCenter = true,
                    size = size,
                )

                // Neon glowing sweep leading-edge line
                drawLine(
                    color = PrimaryCyan.copy(alpha = 0.3f),
                    start = center,
                    end = Offset(center.x, center.y - maxRadius),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = NeutralWhite,
                    start = center,
                    end = Offset(center.x, center.y - maxRadius),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // 6. Draw device dots with premium light reflections & pulsating beacon halos
        devices.forEachIndexed { index, device ->
            val angle = (index * 137.5f) * (Math.PI / 180f) // golden angle distribution
            val distance = maxRadius * 0.3f + (maxRadius * 0.5f * (index % 3 + 1) / 4f)
            val dotX = center.x + (cos(angle) * distance).toFloat()
            val dotY = center.y + (sin(angle) * distance).toFloat()

            // Dynamic beacon breathing ring
            val rippleRadius = 14f + (16f * (breathingScale - 0.7f) / 0.6f)
            val rippleAlpha = (0.3f * (1.3f - breathingScale)).coerceIn(0f, 1f)
            drawCircle(
                color = RadarDotGlow.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = Offset(dotX, dotY),
            )

            // Neon static glow
            drawCircle(
                color = RadarDotGlow,
                radius = 12f,
                center = Offset(dotX, dotY),
            )

            // Inner primary dot
            drawCircle(
                color = RadarDotColor,
                radius = 7f,
                center = Offset(dotX, dotY),
            )

            // High-fidelity light reflection specular dot (top-left)
            drawCircle(
                color = NeutralWhite,
                radius = 2.5f,
                center = Offset(dotX - 2f, dotY - 2f),
            )
        }
    }
}

// ──────────────────────── Device list item (Glassmorphic) ────────────────────────

@Composable
private fun DeviceListItem(
    device: NearbyDevice,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth tactile spring compression on tap
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom visual feedback without generic gray ripple
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    GlassBorderStart,
                    GlassBorderEnd
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Glowing circular avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryAccent, PrimaryCyan))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = device.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = NeutralWhite,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small neon indicator for channel type (Green for Wi-Fi, Purple for other/BLE)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (device.channel.displayName.lowercase().contains("wifi")) SecondaryGreen
                                else SecondaryPurple
                            )
                    )
                    Text(
                        text = device.channel.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Signal strength technical badge
            if (device.rssi != Int.MIN_VALUE) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(0.5.dp, GlassBorderEnd)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
