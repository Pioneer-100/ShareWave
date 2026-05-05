package com.swiftshare.ui.screen.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.swiftshare.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val permissions: List<String>,
)

private fun buildPages(): List<OnboardingPage> {
    val pages = mutableListOf<OnboardingPage>()

    // Page 1: Wi-Fi & Location
    val wifiPerms = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        wifiPerms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
    wifiPerms.add(Manifest.permission.ACCESS_FINE_LOCATION)
    pages.add(
        OnboardingPage(
            icon = Icons.Default.Wifi,
            title = "Nearby Devices",
            description = "SwiftShare needs Wi-Fi permissions to discover and connect to nearby devices for lightning-fast file transfers.",
            permissions = wifiPerms,
        )
    )

    // Page 2: Bluetooth
    val btPerms = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        btPerms.add(Manifest.permission.BLUETOOTH_SCAN)
        btPerms.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    pages.add(
        OnboardingPage(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth Fallback",
            description = "When Wi-Fi Direct isn't available, SwiftShare uses Bluetooth as a backup channel to ensure your files always get through.",
            permissions = btPerms,
        )
    )

    // Page 3: Media access
    val mediaPerms = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        mediaPerms.add(Manifest.permission.READ_MEDIA_IMAGES)
        mediaPerms.add(Manifest.permission.READ_MEDIA_VIDEO)
        mediaPerms.add(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        mediaPerms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        mediaPerms.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    pages.add(
        OnboardingPage(
            icon = Icons.Default.PhotoLibrary,
            title = "Access Your Files",
            description = "To share photos, videos, and documents, SwiftShare needs access to your media files. Your data never leaves your device.",
            permissions = mediaPerms,
        )
    )

    return pages
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages = remember { buildPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // Collect all permissions across all pages for a unified state
    val allPermissions = remember { pages.flatMap { it.permissions }.distinct() }
    val permissionsState = rememberMultiplePermissionsState(permissions = allPermissions)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ──────── Skip button ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = {
                viewModel.completeOnboarding()
                onCompleted()
            }) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ──────── Pager ────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // ──────── Page indicators ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                val isActive = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isActive) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) PrimaryCyan
                            else PrimaryCyan.copy(alpha = 0.2f)
                        ),
                )
            }
        }

        // ──────── Action button ────────
        val isLastPage = pagerState.currentPage == pages.size - 1

        Button(
            onClick = {
                if (isLastPage) {
                    // Request all permissions then complete
                    permissionsState.launchMultiplePermissionRequest()
                    viewModel.completeOnboarding()
                    onCompleted()
                } else {
                    scope.launch {
                        // Request permissions for current page first
                        val currentPerms = pages[pagerState.currentPage].permissions
                        permissionsState.launchMultiplePermissionRequest()
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
        ) {
            Text(
                text = if (isLastPage) "Get Started" else "Grant & Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(PrimaryAccent, PrimaryCyan))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                page.icon,
                contentDescription = page.title,
                modifier = Modifier.size(56.dp),
                tint = NeutralWhite,
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        )
    }
}
