package com.swiftshare.ui.screen.progress

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swiftshare.domain.model.TransferSessionState
import com.swiftshare.ui.theme.*

@Composable
fun TransferProgressScreen(
    sessionId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: TransferProgressViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    val isComplete = sessionState is TransferSessionState.Completed
    val isFailed = sessionState is TransferSessionState.Failed
    val isCancelled = sessionState is TransferSessionState.Cancelled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ──────── Top bar ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(Modifier.weight(1f))
            if (!isComplete && !isFailed && !isCancelled) {
                TextButton(onClick = { viewModel.cancelTransfer() }) {
                    Text("Cancel", color = SecondaryPink)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // ──────── Circular progress ────────
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (progress.overallPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.size(200.dp),
                strokeWidth = 10.dp,
                color = PrimaryCyan,
                trackColor = PrimaryCyan.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${progress.overallPercent.toInt()}%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = when {
                        isComplete -> "Complete!"
                        isFailed -> "Failed"
                        isCancelled -> "Cancelled"
                        else -> progress.speedDisplayMbps
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isComplete -> SecondaryGreen
                        isFailed -> SecondaryPink
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ──────── File info ────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoRow(label = "File", value = progress.currentFileName.ifBlank { "—" })
                InfoRow(label = "Speed", value = progress.speedDisplayMbps)
                InfoRow(label = "ETA", value = progress.etaDisplay)
                InfoRow(
                    label = "Transferred",
                    value = "${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}"
                )

                // Per-file progress bar
                if (progress.totalChunks > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.filePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryAccent,
                        trackColor = PrimaryAccent.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ──────── Done button ────────
        if (isComplete || isFailed || isCancelled) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isComplete) SecondaryGreen else PrimaryAccent,
                ),
            ) {
                Text(
                    text = if (isComplete) "Done" else "Back to Home",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576.0)
    else -> "%.2f GB".format(bytes / 1_073_741_824.0)
}
