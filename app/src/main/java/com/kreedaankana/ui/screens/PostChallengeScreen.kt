package com.kreedaankana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.navigation.NavController
import com.kreedaankana.model.Sport
import com.kreedaankana.ui.components.SportChip
import com.kreedaankana.ui.theme.*
import com.kreedaankana.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostChallengeScreen(navController: NavController, viewModel: MainViewModel) {
    val dateFmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today     = LocalDate.now()
    val next7Days = (0..6).map { today.plusDays(it.toLong()) }

    var selectedDate  by remember { mutableStateOf(today) }
    var selectedSport by remember { mutableStateOf(Sport.CRICKET) }
    var selectedStart by remember { mutableStateOf("") }
    var selectedEnd   by remember { mutableStateOf("") }
    var caption       by remember { mutableStateOf("") }
    val teamName      by viewModel.currentTeamName.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val message       by viewModel.message.collectAsState()
    val snackbar       = remember { SnackbarHostState() }

    // Auto-caption suggestion
    fun suggestCaption() {
        caption = "$teamName invites all for a ${selectedSport.displayName} showdown on ${
            selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))} at $selectedStart! Who's up for the challenge? ⚡"
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it.text)
            viewModel.clearMessage()
            if (!it.isError) navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Post a Challenge", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SportOrange, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            SLabel("Select Date")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(next7Days) { date ->
                    val label = if (date == today) "Today" else
                        date.format(DateTimeFormatter.ofPattern("EEE d"))
                    FilterChip(
                        selected = date == selectedDate, onClick = { selectedDate = date },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SportOrange, selectedLabelColor = Color.White)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SLabel("Select Sport")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Sport.values()) { sport ->
                    SportChip(sport = sport, selected = selectedSport == sport,
                        onClick = { selectedSport = sport; caption = "" })
                }
            }

            Spacer(Modifier.height(16.dp))
            SLabel("Select Time Slot")
            viewModel.timeSlots.forEach { (start, end) ->
                val selected = selectedStart == start
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape    = RoundedCornerShape(12.dp),
                    onClick  = { selectedStart = start; selectedEnd = end; caption = "" }
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("$start – $end", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        if (selected) Icon(Icons.Default.CheckCircle, null, tint = SportOrange)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                SLabel("Challenge Caption")
                TextButton(onClick = { suggestCaption() }) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Suggest", style = MaterialTheme.typography.labelMedium)
                }
            }
            OutlinedTextField(
                value = caption, onValueChange = { caption = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Write a challenge message... or use AI Suggest ✨") },
                minLines = 3, maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportOrange, focusedLabelColor = SportOrange,
                    cursorColor = SportOrange
                )
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.postChallenge(
                        sport = selectedSport,
                        date  = selectedDate.format(dateFmt),
                        startTime = selectedStart, endTime = selectedEnd,
                        caption   = caption
                    )
                },
                enabled  = selectedStart.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Default.EmojiEvents, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Post Challenge", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = SportOrange,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}
