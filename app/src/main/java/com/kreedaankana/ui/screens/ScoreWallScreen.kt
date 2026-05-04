package com.kreedaankana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreedaankana.model.Sport
import com.kreedaankana.navigation.Screen
import com.kreedaankana.ui.components.*
import com.kreedaankana.ui.theme.*
import com.kreedaankana.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Score Wall ────────────────────────────────────────────────────────────────
@Composable
fun ScoreWallScreen(navController: NavController, viewModel: MainViewModel) {
    val results   by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GradientHeader {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Score Wall", style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Latest village match results · live", style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.75f))
                }
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.PostResult.route) },
                    containerColor = SportOrange, modifier = Modifier.size(48.dp)
                ) { Icon(Icons.Default.Add, null, tint = Color.White) }
            }
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No results yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Post your first match result!", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results, key = { it.id }) { result -> ResultCard(result) }
            }
        }
    }
}

// ── Post Result ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostResultScreen(navController: NavController, viewModel: MainViewModel) {
    val myTeamName    by viewModel.currentTeamName.collectAsState()
    var opponentName  by remember { mutableStateOf("") }
    var myScore       by remember { mutableStateOf("") }
    var opponentScore by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf(Sport.CRICKET) }
    var outcomeIndex  by remember { mutableIntStateOf(0) } // 0=Win 1=Draw 2=Loss

    val isLoading by viewModel.isLoading.collectAsState()
    val message   by viewModel.message.collectAsState()
    val snackbar   = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it.text)
            viewModel.clearMessage()
            if (!it.isError) navController.popBackStack()
        }
    }

    // FIX: Use MaterialTheme text colours everywhere — no hardcoded dark values.
    // textFieldColors() derives from theme so it works on both light AND dark.
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor        = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
        disabledTextColor       = MaterialTheme.colorScheme.onSurface.copy(0.6f),
        focusedLabelColor       = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor      = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
        focusedContainerColor   = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
        cursorColor             = MaterialTheme.colorScheme.primary,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Post Result", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SportGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionHeader("Match Result", Icons.Default.EmojiEvents)
            Spacer(Modifier.height(20.dp))

            // ── Teams row ─────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Your Team", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = myTeamName, onValueChange = {},
                        modifier = Modifier.fillMaxWidth(), enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Opponent", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = opponentName, onValueChange = { opponentName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Team name") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = textFieldColors
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Scores row ────────────────────────────────────────────────────
            Text("Scores", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = myScore, onValueChange = { myScore = it },
                    label = { Text("Your Score") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )
                Text("VS", modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(
                    value = opponentScore, onValueChange = { opponentScore = it },
                    label = { Text("Opponent") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Sport selector ────────────────────────────────────────────────
            Text("Sport", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sport.values().take(3).forEach { sport ->
                    SportChip(sport = sport, selected = selectedSport == sport, onClick = { selectedSport = sport })
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sport.values().drop(3).forEach { sport ->
                    SportChip(sport = sport, selected = selectedSport == sport, onClick = { selectedSport = sport })
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Outcome selector ──────────────────────────────────────────────
            Text("Match Outcome", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                    selected = outcomeIndex == 0, onClick = { outcomeIndex = 0 },
                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                ) { Text("🏆 We Won", fontWeight = FontWeight.SemiBold) }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                    selected = outcomeIndex == 1, onClick = { outcomeIndex = 1 },
                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                ) { Text("🤝 Draw", fontWeight = FontWeight.SemiBold) }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                    selected = outcomeIndex == 2, onClick = { outcomeIndex = 2 },
                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                ) { Text("📉 We Lost", fontWeight = FontWeight.SemiBold) }
            }

            // Outcome info card
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (outcomeIndex) {
                        0    -> MaterialTheme.colorScheme.primaryContainer
                        1    -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Text(
                    when (outcomeIndex) {
                        0    -> "✅ ${myTeamName.ifEmpty { "Your team" }} wins! Rankings will reflect +1 Win."
                        1    -> "🤝 Match drawn. Both teams receive +1 Draw in rankings."
                        else -> "📉 ${opponentName.ifEmpty { "Opponent" }} wins. Rankings updated accordingly."
                    },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    // FIX: use onXxxContainer colours — visible on both light and dark
                    color = when (outcomeIndex) {
                        0    -> MaterialTheme.colorScheme.onPrimaryContainer
                        1    -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.postResult(
                        team1Name  = myTeamName, team1Score = myScore,
                        team2Name  = opponentName, team2Score = opponentScore,
                        sport      = selectedSport,
                        date       = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        winnerName = when (outcomeIndex) { 0 -> myTeamName; 2 -> opponentName; else -> "" },
                        isDraw     = outcomeIndex == 1
                    )
                },
                enabled  = opponentName.isNotBlank() && myScore.isNotBlank() && opponentScore.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SportGreenMid)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Default.Scoreboard, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Post Result", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
