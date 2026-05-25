package com.swiftshare.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.data.db.entity.TransferStatus
import com.swiftshare.domain.model.TransferDirection
import com.swiftshare.ui.component.ChannelStatusChip
import com.swiftshare.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Breathtaking ambient glowing background
        AuroraBackgroundCanvas()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 60.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ──────── Header ────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "SwiftShare",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = state.deviceName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ChannelStatusChip(channel = state.activeChannel)
                    }
                }
            }

            // ──────── Send / Receive cards ────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Send",
                        subtitle = "Share files nearby",
                        icon = Icons.AutoMirrored.Filled.Send,
                        gradientColors = listOf(PrimaryAccent, PrimaryCyan),
                        onClick = onSendClick,
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Receive",
                        subtitle = "Get files from peer",
                        icon = Icons.Default.Download,
                        gradientColors = listOf(PrimaryCyan, SecondaryPurple),
                        onClick = onReceiveClick,
                    )
                }
            }

            // ──────── AI Feature chips ────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AiChip(
                        label = "Smart Compress",
                        enabled = state.aiCompressionEnabled,
                        icon = Icons.Default.Compress,
                    )
                    AiChip(
                        label = "Dedup Detect",
                        enabled = state.aiDeduplicationEnabled,
                        icon = Icons.Default.FindReplace,
                    )
                    if (state.totalBytesSaved > 0) {
                        AiChip(
                            label = formatBytes(state.totalBytesSaved) + " saved",
                            enabled = true,
                            icon = Icons.Default.Savings,
                        )
                    }
                }
            }

            // ──────── Stats row ────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Transferred",
                        value = formatBytes(state.totalBytesSent),
                        icon = Icons.Default.SwapVert,
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Files",
                        value = state.recentTransfers.size.toString(),
                        icon = Icons.Default.Folder,
                    )
                }
            }

            // ──────── Recent transfers ────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent Transfers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onHistoryClick) {
                        Text("View All")
                    }
                }
            }

            if (state.recentTransfers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No transfers yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Text(
                                "Tap Send or Receive to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            } else {
                items(state.recentTransfers, key = { it.id }) { record ->
                    TransferRecordItem(record = record)
                }
            }
        }
    }
}

// ──────────────────────── Sub-components ────────────────────────

@Composable
private fun AuroraBackgroundCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Slow moving coordinates for first glowing blob (Cyan)
    val floatX1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = SineWaveEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )
    val floatY1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = SineWaveEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )

    // Slow moving coordinates for second glowing blob (Purple)
    val floatX2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = SineWaveEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x2"
    )
    val floatY2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 21000, easing = SineWaveEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y2"
    )

    val isDark = isSystemInDarkTheme()
    val baseBackground = if (isDark) DarkBackground else LightBackground

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        val width = size.width
        val height = size.height

        // First blob (Cyan glow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonGlowCyan.copy(alpha = if (isDark) 0.08f else 0.04f),
                    Color.Transparent
                ),
                radius = width * 0.9f
            ),
            radius = width * 0.9f,
            center = Offset(width * floatX1, height * floatY1)
        )

        // Second blob (Purple glow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonGlowPurple.copy(alpha = if (isDark) 0.08f else 0.04f),
                    Color.Transparent
                ),
                radius = width * 0.8f
            ),
            radius = width * 0.8f,
            center = Offset(width * floatX2, height * floatY2)
        )
    }
}

private val SineWaveEasing = Easing { fraction ->
    val sinVal = kotlin.math.sin(fraction * Math.PI - Math.PI / 2)
    ((sinVal + 1) / 2).toFloat()
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(gradientColors),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
        ) {
            // Specular shiny overlay highlight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isPressed) 0.05f else 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = if (isPressed) 0.15f else 0.0f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeutralWhite.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = title, tint = NeutralWhite, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = NeutralWhite, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NeutralWhite.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun AiChip(label: String, enabled: Boolean, icon: ImageVector) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) SecondaryPurple.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = if (enabled) SecondaryPurple
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (enabled) SecondaryPurple.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (enabled) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(SecondaryPurple.copy(alpha = pulseAlpha))
                )
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(GlassBorderStart, GlassBorderEnd))
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TransferRecordItem(record: TransferRecord) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val statusColor = when (record.status) {
        TransferStatus.COMPLETED -> SecondaryGreen
        TransferStatus.FAILED -> SecondaryPink
        TransferStatus.IN_PROGRESS -> SecondaryAmber
        TransferStatus.CANCELLED -> NeutralGray400
    }
    val directionIcon = if (record.direction == TransferDirection.SEND) Icons.AutoMirrored.Filled.Send else Icons.Default.Download

    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(GlassBorderStart, GlassBorderEnd))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(directionIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (record.status == TransferStatus.IN_PROGRESS) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${record.peerName} · ${dateFormat.format(Date(record.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatBytes(record.fileSize),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (record.compressionSavingsPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecondaryGreen.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            text = "-${record.compressionSavingsPercent}% Saved",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGreen,
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576.0)
    else -> "%.2f GB".format(bytes / 1_073_741_824.0)
}
