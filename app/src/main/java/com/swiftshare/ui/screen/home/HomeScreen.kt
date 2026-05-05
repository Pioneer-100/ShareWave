package com.swiftshare.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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

// ──────────────────────── Sub-components ────────────────────────

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<androidx.compose.ui.graphics.Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) SecondaryPurple.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) SecondaryPurple
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(28.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
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
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(directionIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (record.compressionSavingsPercent > 0) {
                    Text(
                        text = "-${record.compressionSavingsPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGreen,
                    )
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
