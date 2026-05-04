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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreedaankana.model.Sport
import com.kreedaankana.ui.components.*
import com.kreedaankana.ui.theme.*
import com.kreedaankana.utils.SavedTeamProfile
import com.kreedaankana.viewmodel.MainViewModel

// ── Leaderboard ───────────────────────────────────────────────────────────────
@Composable
fun LeaderboardScreen(viewModel: MainViewModel) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val results     by viewModel.results.collectAsState()
    var filterSport by remember { mutableStateOf<Sport?>(null) }
    var showResults by remember { mutableStateOf(false) }

    val filtered = if (filterSport == null) leaderboard
    else leaderboard.filter { it.sport == filterSport!!.name }

    Column(Modifier.fillMaxSize()) {
        GradientHeader {
            Text("Village Rankings", style = MaterialTheme.typography.headlineSmall,
                color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Live standings · updates in real-time across all phones",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.75f))
        }

        TabRow(
            selectedTabIndex = if (showResults) 1 else 0,
            containerColor   = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(selected = !showResults, onClick = { showResults = false },
                text = { Text("🏆 Rankings", fontWeight = FontWeight.Bold) })
            Tab(selected = showResults, onClick = { showResults = true },
                text = { Text("📊 Match Results", fontWeight = FontWeight.Bold) })
        }

        if (!showResults) {
            // Sport filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterSport == null,
                    onClick  = { filterSport = null },
                    label    = { Text("All", fontWeight = FontWeight.Bold) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SportGreenMid, selectedLabelColor = Color.White)
                )
                Sport.values().forEach { sport ->
                    FilterChip(
                        selected = filterSport == sport,
                        onClick  = { filterSport = sport },
                        label    = { Text(sport.emoji, fontSize = 16.sp) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 64.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No teams yet", style = MaterialTheme.typography.titleMedium)
                        Text("Register and play to appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.teamId }) { entry ->
                        LeaderboardRow(entry)
                    }
                }
            }
        } else {
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 64.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No results posted yet", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.id }) { result ->
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(3.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = if (result.isDraw)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val sport = try { Sport.valueOf(result.sport) }
                                        catch (e: Exception) { Sport.CRICKET }
                                    Text("${sport.emoji} ${sport.displayName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold)
                                    Text(result.date, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(result.team1Name,
                                            fontWeight = if (!result.isDraw && result.winnerName == result.team1Name)
                                                FontWeight.ExtraBold else FontWeight.Normal)
                                        Text(result.team1Score,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (!result.isDraw && result.winnerName == result.team1Name)
                                                SportOrange else MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text("VS", style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                        fontWeight = FontWeight.Black)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(result.team2Name,
                                            fontWeight = if (!result.isDraw && result.winnerName == result.team2Name)
                                                FontWeight.ExtraBold else FontWeight.Normal)
                                        Text(result.team2Score,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (!result.isDraw && result.winnerName == result.team2Name)
                                                SportOrange else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (result.isDraw) MaterialTheme.colorScheme.secondaryContainer else SportGreenSurface,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        if (result.isDraw) "🤝 Match Drawn — both teams earn a draw"
                                        else "🏆 ${result.winnerName} wins! (+1 Win in Rankings)",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (result.isDraw) MaterialTheme.colorScheme.onSecondaryContainer else SportGreenMid,
                                        fontWeight = FontWeight.Bold,
                                        textAlign  = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(viewModel: MainViewModel, navController: NavController) {
    val teamId   by viewModel.currentTeamId.collectAsState()
    val teamName by viewModel.currentTeamName.collectAsState()
    val village  by viewModel.currentVillage.collectAsState()
    val sport    by viewModel.currentSport.collectAsState()
    val captain  by viewModel.captainName.collectAsState()

    // Live stats from leaderboard (Firestore real-time)
    val myEntry        by viewModel.myLeaderboardEntry.collectAsState()
    val savedProfiles  by viewModel.savedProfiles.collectAsState()

    var showLogoutDialog      by remember { mutableStateOf(false) }
    var showSwitchDialog      by remember { mutableStateOf(false) }
    var showRegisterDialog    by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val message  by viewModel.message.collectAsState()
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it.text); viewModel.clearMessage() }
    }

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon  = { Text("👋", fontSize = 32.sp) },
            title = { Text("Log Out?", fontWeight = FontWeight.ExtraBold) },
            text  = { Text("You can log back in anytime with your team name.") },
            confirmButton = {
                Button(onClick = { viewModel.logout(); showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Log Out") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            icon  = { Text("➕", fontSize = 32.sp) },
            title = { Text("Register Another Team", fontWeight = FontWeight.ExtraBold) },
            text  = { Text("You'll be taken to sign-up. Your current team is saved — you can switch back anytime.") },
            confirmButton = {
                Button(onClick = {
                    showRegisterDialog = false
                    viewModel.logout() // go to onboarding sign up screen
                }) { Text("Continue") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRegisterDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Switch Account dialog
    if (showSwitchDialog) {
        val otherProfiles = savedProfiles.filter { it.teamId != teamId }
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            icon  = { Text("🔄", fontSize = 32.sp) },
            title = { Text("Switch Account", fontWeight = FontWeight.ExtraBold) },
            text  = {
                if (otherProfiles.isEmpty()) {
                    Text("No other accounts on this device. Register another team to switch.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select account to switch to:",
                            style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        otherProfiles.forEach { profile ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(containerColor = SportGreenSurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.teamName, fontWeight = FontWeight.Bold,
                                            color = SportGreen)
                                        Text(profile.village,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray)
                                    }
                                    TextButton(onClick = {
                                        viewModel.switchTeam(profile)
                                        showSwitchDialog = false
                                    }) { Text("Switch", color = SportGreenMid, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showSwitchDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            GradientHeader {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)) {
                        Text(
                            try { Sport.valueOf(sport).emoji } catch (e: Exception) { "🏟️" },
                            fontSize = 56.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(teamName.ifEmpty { "My Team" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center)
                        Text(village, style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.75f))
                        Text("Captain: $captain", style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.6f))
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout",
                            tint = Color.White.copy(0.85f))
                    }
                }
            }

            Column(Modifier.padding(20.dp)) {
                // ── Live Stats (from Firestore via leaderboard) ───────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCard("Rank",   myEntry?.let { "#${it.rank}" } ?: "—",   "🏅")
                    StatCard("Wins",   "${myEntry?.wins   ?: 0}",                "🏆")
                    StatCard("Draws",  "${myEntry?.draws  ?: 0}",                "🤝")
                    StatCard("Losses", "${myEntry?.losses ?: 0}",                "📉")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stats update live from Firestore — same across all devices",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                // ── Team Info ─────────────────────────────────────────────────
                SectionHeader("Team Info", Icons.Default.Group)
                Spacer(Modifier.height(12.dp))
                InfoRow(Icons.Default.Group, "Team Name", teamName)
                InfoRow(Icons.Default.LocationOn, "Village", village)
                InfoRow(
                    Icons.Default.SportsCricket, "Primary Sport",
                    try { "${Sport.valueOf(sport).emoji} ${Sport.valueOf(sport).displayName}" }
                    catch (e: Exception) { sport }
                )

                Spacer(Modifier.height(24.dp))
                SectionHeader("Quick Actions", Icons.Default.Bolt)
                Spacer(Modifier.height(12.dp))
                InfoCard("📅 Book a Slot",
                    "Go to Calendar → tap any green (Free) cell to reserve your time.")
                Spacer(Modifier.height(8.dp))
                InfoCard("⚔️ Post a Challenge",
                    "Head to Challenge Board → tap + to post an open challenge.")
                Spacer(Modifier.height(8.dp))
                InfoCard("📊 Post Results",
                    "After your match, go to Score Wall → + to record the final score. Rankings update live on every phone!")

                Spacer(Modifier.height(24.dp))
                SectionHeader("Account", Icons.Default.ManageAccounts)
                Spacer(Modifier.height(12.dp))

                // Switch Account
                OutlinedButton(
                    onClick  = { showSwitchDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, SportGreenMid)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SportGreenMid)
                    Spacer(Modifier.width(8.dp))
                    Text("Switch Account", fontWeight = FontWeight.SemiBold, color = SportGreenMid)
                    if (savedProfiles.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Badge { Text("${savedProfiles.size}") }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Register Another Team
                OutlinedButton(
                    onClick  = { showRegisterDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Register Another Team", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(10.dp))

                // Log Out
                OutlinedButton(
                    onClick  = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error),
                    border   = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(label: String, value: String, emoji: String) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SportGreenSurface),
        modifier = Modifier.width(80.dp)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, color = SportGreen)
            Text(label, style = MaterialTheme.typography.labelSmall, color = SportGreenMid,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SportGreenMid, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            Text(value.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
        }
    }
}
