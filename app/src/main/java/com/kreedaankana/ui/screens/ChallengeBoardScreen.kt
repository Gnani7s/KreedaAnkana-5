package com.kreedaankana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreedaankana.navigation.Screen
import com.kreedaankana.ui.components.*
import com.kreedaankana.ui.theme.*
import com.kreedaankana.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeBoardScreen(navController: NavController, viewModel: MainViewModel) {
    val challenges  by viewModel.challenges.collectAsState()
    val currentTeam by viewModel.currentTeamId.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val message     by viewModel.message.collectAsState()
    val snackbar     = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it.text); viewModel.clearMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GradientHeader {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Challenge Board", style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("${challenges.size} open challenges", style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.75f))
                    }
                    FloatingActionButton(
                        onClick = { navController.navigate(Screen.PostChallenge.route) },
                        containerColor = SportOrange, modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Post Challenge", tint = Color.White)
                    }
                }
            }

            if (challenges.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚔️", fontSize = 64.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No open challenges", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Be the first to post one!", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { navController.navigate(Screen.PostChallenge.route) },
                            colors  = ButtonDefaults.buttonColors(containerColor = SportOrange)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Post a Challenge", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(challenges, key = { it.id }) { challenge ->
                        ChallengeCard(
                            challenge     = challenge,
                            currentTeamId = currentTeam,
                            onAccept      = { id ->
                                viewModel.acceptChallenge(id)
                            }
                        )
                    }
                }
            }
        }
    }
}
