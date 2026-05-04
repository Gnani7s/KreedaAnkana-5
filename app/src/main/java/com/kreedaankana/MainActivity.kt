package com.kreedaankana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kreedaankana.navigation.Screen
import com.kreedaankana.ui.screens.HomeScreen
import com.kreedaankana.ui.screens.OnboardingScreen
import com.kreedaankana.ui.theme.KreedaAnkanaTheme
import com.kreedaankana.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KreedaAnkanaTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val isRegistered by viewModel.isRegistered.collectAsState()
                val message      by viewModel.message.collectAsState()
                val snackbar      = remember { SnackbarHostState() }

                LaunchedEffect(message) {
                    message?.let { snackbar.showSnackbar(it.text); viewModel.clearMessage() }
                }

                // Use a simple conditional — no NavController race condition
                if (isRegistered) {
                    val navController = rememberNavController()
                    HomeScreen(navController, viewModel)
                } else {
                    OnboardingScreen(viewModel)
                }
            }
        }
    }
}
