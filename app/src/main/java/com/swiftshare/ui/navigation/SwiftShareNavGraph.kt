package com.swiftshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swiftshare.ui.screen.home.HomeScreen
import com.swiftshare.ui.screen.radar.RadarScreen
import com.swiftshare.ui.screen.history.TransferHistoryScreen
import com.swiftshare.ui.screen.progress.TransferProgressScreen
import com.swiftshare.ui.screen.onboarding.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val FILE_PICKER = "file_picker"
    const val RADAR = "radar/{mode}"         // mode = "send" | "receive"
    const val PROGRESS = "progress/{sessionId}"
    const val HISTORY = "history"

    fun radar(mode: String) = "radar/$mode"
    fun progress(sessionId: String) = "progress/$sessionId"
}

@Composable
fun SwiftShareNavGraph(startOnboarding: Boolean = false) {
    val navController = rememberNavController()
    val startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onCompleted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onSendClick = { navController.navigate(Routes.FILE_PICKER) },
                onReceiveClick = { navController.navigate(Routes.radar("receive")) },
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.FILE_PICKER) {
            com.swiftshare.ui.screen.filepicker.FilePickerScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.radar("send"))
                }
            )
        }

        composable(Routes.RADAR) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "send"
            RadarScreen(
                mode = mode,
                onDeviceSelected = { device ->
                    // For now navigate to progress; will be wired to TransferEngine
                    navController.navigate(Routes.progress("new"))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROGRESS) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            TransferProgressScreen(
                sessionId = sessionId,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            TransferHistoryScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
