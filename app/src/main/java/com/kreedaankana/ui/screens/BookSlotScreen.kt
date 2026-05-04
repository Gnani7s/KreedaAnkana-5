package com.kreedaankana.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import com.kreedaankana.model.SlotStatus
import com.kreedaankana.model.Sport
import com.kreedaankana.ui.components.GradientHeader
import com.kreedaankana.ui.components.SportChip
import com.kreedaankana.ui.theme.*
import com.kreedaankana.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSlotScreen(navController: NavController, viewModel: MainViewModel) {
    val dateFmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today      = LocalDate.now()
    val next14Days = (0..13).map { today.plusDays(it.toLong()) }

    var selectedDate  by remember { mutableStateOf(today) }
    var selectedSport by remember { mutableStateOf(Sport.CRICKET) }
    var selectedStart by remember { mutableStateOf("") }
    var selectedEnd   by remember { mutableStateOf("") }
    val isLoading     by viewModel.isLoading.collectAsState()
    val message       by viewModel.message.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }

    /**
     * FIX: Collect the Eagerly-started slots stream so the slot grid is always
     * in sync with Firestore. When another device books a slot, this screen
     * recomposes immediately showing that slot as Booked.
     */
    val slots by viewModel.slots.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbarState.showSnackbar(it.text)
            viewModel.clearMessage()
            if (!it.isError) navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Book a Slot", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SportGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            // ── Team banner ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(viewModel.currentTeamName.collectAsState().value,
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(viewModel.currentVillage.collectAsState().value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Date selector ─────────────────────────────────────────────────
            SectionLabel("Select Date")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(next14Days) { date ->
                    val label = if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("EEE d"))
                    FilterChip(
                        selected = date == selectedDate,
                        onClick  = { selectedDate = date; selectedStart = ""; selectedEnd = "" },
                        label    = { Text(label, fontWeight = FontWeight.SemiBold) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SportGreenMid, selectedLabelColor = Color.White)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Sport selector ────────────────────────────────────────────────
            SectionLabel("Select Sport")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Sport.values()) { sport ->
                    SportChip(sport = sport, selected = selectedSport == sport,
                        onClick = { selectedSport = sport; selectedStart = ""; selectedEnd = "" })
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Time slots ────────────────────────────────────────────────────
            SectionLabel("Select Time Slot")
            val dateStr = selectedDate.format(dateFmt)
            val isPast  = selectedDate.isBefore(today)

            viewModel.timeSlots.forEach { (start, end) ->
                /**
                 * FIX: Derive status from live `slots` StateFlow — same source as CalendarScreen.
                 * This means if another user books a slot while you have this screen open,
                 * it flips to Booked here in real-time (BookMyShow behaviour).
                 */
                val bookedSlot = slots.find {
                    it.date == dateStr && it.startTime == start && it.sport == selectedSport.name
                }
                val status = when {
                    isPast                                       -> SlotStatus.PAST
                    bookedSlot?.statusEnum == SlotStatus.BOOKED -> SlotStatus.BOOKED
                    else                                         -> SlotStatus.FREE
                }
                val isSelected = selectedStart == start
                val isFree     = status == SlotStatus.FREE

                // Animated card background
                val cardBg by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        !isFree    -> MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                        else       -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(200), label = "slotCard$start"
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = cardBg),
                    border   = if (isSelected)
                        CardDefaults.outlinedCardBorder()
                    else null,
                    onClick  = { if (isFree) { selectedStart = start; selectedEnd = end } }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule, null,
                                tint = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isFree     -> SlotAvailable
                                    else       -> SlotPast
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("$start – $end", fontWeight = FontWeight.SemiBold)
                                if (status == SlotStatus.BOOKED && bookedSlot != null) {
                                    Text(
                                        "Booked by ${bookedSlot.teamName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotBooked
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                isSelected -> SportGreenMid
                                isFree     -> SlotAvailable.copy(0.15f)
                                else       -> SlotBooked.copy(0.15f)
                            }
                        ) {
                            Text(
                                when {
                                    isSelected -> "✓ Selected"
                                    isFree     -> "Free"
                                    isPast     -> "Past"
                                    else       -> "Booked"
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSelected -> Color.White
                                    isFree     -> SlotAvailable
                                    else       -> SlotBooked
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.bookSlot(
                        date = selectedDate.format(dateFmt),
                        startTime = selectedStart,
                        endTime   = selectedEnd,
                        sport     = selectedSport
                    )
                },
                enabled  = selectedStart.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SportGreenMid)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.BookOnline, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Booking", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}
