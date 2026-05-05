package com.swiftshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.swiftshare.ui.navigation.SwiftShareNavGraph
import com.swiftshare.ui.theme.SwiftShareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwiftShareTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SwiftShareNavGraph()
                }
            }
        }
    }
}
