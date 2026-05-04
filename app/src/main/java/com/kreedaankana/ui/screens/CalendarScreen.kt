package com.kreedaankana.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreedaankana.model.SlotStatus
import com.kreedaankana.model.Sport
import com.kreedaankana.navigation.Screen
import com.kreedaankana.ui.components.*
import com.kreedaankana.ui.theme.*
import com.kreedaankana.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(navController: NavController, viewModel: MainViewModel) {
    /**
     * FIX: slots is now Eagerly started in ViewModel — the Firestore listener never
     * stops. So this collect always reflects the live server state with no stale-cache
     * glitch. No need to restart or re-subscribe when navigating back.
     */
    val slots        by viewModel.slots.collectAsState()
    val dateFmt       = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today         = LocalDate.now()
    val next14Days    = (0..13).map { today.plusDays(it.toLong()) }

    var selectedDate  by remember { mutableStateOf(today) }
    var selectedSport by remember { mutableStateOf(Sport.CRICKET) }

    Column(Modifier.fillMaxSize()) {
        GradientHeader {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Ground Calendar", style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                    val bookedToday = slots.count { it.date == today.format(dateFmt) && it.status == "BOOKED" }
                    Text(
                        "Real-time slot booking · $bookedToday booked today",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.75f)
                    )
                }
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.BookSlot.route) },
                    containerColor = SportOrange, modifier = Modifier.size(48.dp)
                ) { Icon(Icons.Default.Add, "Book Slot", tint = Color.White) }
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // ── Date picker ───────────────────────────────────────────────────
            Text("Select Date", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(next14Days) { date ->
                    val ds         = date.format(dateFmt)
                    val hasBooking = slots.any { it.date == ds && it.sport == selectedSport.name && it.status == "BOOKED" }
                    DayChip(date = date, selected = date == selectedDate,
                        hasBooking = hasBooking, onClick = { selectedDate = date })
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Sport picker ──────────────────────────────────────────────────
            Text("Sport", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Sport.values()) { sport ->
                    SportChip(sport = sport, selected = selectedSport == sport,
                        onClick = { selectedSport = sport })
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Day heading ───────────────────────────────────────────────────
            Text(
                "${selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(SlotAvailable, "Free")
                LegendDot(SlotBooked,    "Booked")
                LegendDot(SlotPast,      "Past")
            }
            Spacer(Modifier.height(12.dp))

            // ── Slot rows ─────────────────────────────────────────────────────
            val ds = selectedDate.format(dateFmt)
            viewModel.timeSlots.forEach { (start, end) ->
                /**
                 * FIX: Derive status from the live `slots` StateFlow directly.
                 * Previously stale Room snapshots could show wrong status.
                 * Now every recomposition reads the current Firestore-backed state.
                 */
                val isPast = selectedDate.isBefore(today)
                val bookedSlot = slots.find {
                    it.date == ds && it.startTime == start && it.sport == selectedSport.name
                }
                val status = when {
                    isPast                                          -> SlotStatus.PAST
                    bookedSlot?.statusEnum == SlotStatus.BOOKED    -> SlotStatus.BOOKED
                    else                                            -> SlotStatus.FREE
                }

                SlotRowItem(
                    startTime = start, endTime = end, status = status,
                    teamName  = bookedSlot?.teamName ?: "",
                    village   = bookedSlot?.village ?: "",
                    onClick   = { if (status == SlotStatus.FREE) navController.navigate(Screen.BookSlot.route) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Quick stats card ──────────────────────────────────────────────
            val daySlots  = slots.filter { it.date == ds && it.sport == selectedSport.name }
            val booked    = daySlots.count { it.status == "BOOKED" }
            val available = viewModel.timeSlots.size - booked
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CalStatItem("${selectedSport.emoji} ${selectedSport.displayName}", "Sport")
                    CalStatItem("$booked / ${viewModel.timeSlots.size}", "Booked")
                    CalStatItem("$available", "Free")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Upcoming bookings for selected sport ──────────────────────────
            val upcoming = slots
                .filter { it.sport == selectedSport.name && it.status == "BOOKED" && it.date >= today.format(dateFmt) }
                .sortedWith(compareBy({ it.date }, { it.startTime }))
                .take(6)

            if (upcoming.isNotEmpty()) {
                Text("Upcoming — ${selectedSport.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                upcoming.forEach { slot ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(slot.date, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("${slot.startTime}–${slot.endTime}",
                                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(slot.teamName, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(slot.village, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── SlotRowItem ───────────────────────────────────────────────────────────────
@Composable
private fun SlotRowItem(
    startTime: String, endTime: String,
    status: SlotStatus, teamName: String, village: String,
    onClick: () -> Unit
) {
    // Animate background colour so status changes animate smoothly (BookMyShow feel)
    val bgColor by animateColorAsState(
        targetValue = when (status) {
            SlotStatus.FREE        -> SlotAvailable.copy(0.12f)
            SlotStatus.BOOKED      -> SlotBooked.copy(0.12f)
            SlotStatus.PAST        -> SlotPast.copy(0.12f)
            SlotStatus.MAINTENANCE -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300), label = "slotBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(if (status == SlotStatus.FREE) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Schedule, null,
            tint = when (status) {
                SlotStatus.FREE   -> SlotAvailable
                SlotStatus.BOOKED -> SlotBooked
                else              -> SlotPast
            },
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text("$startTime – $endTime", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(108.dp))
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (status) {
                    SlotStatus.FREE   -> SlotAvailable
                    SlotStatus.BOOKED -> SlotBooked
                    SlotStatus.PAST   -> SlotPast
                    else              -> MaterialTheme.colorScheme.outline
                }
            ) {
                Text(
                    when (status) {
                        SlotStatus.FREE   -> "Tap to Book"
                        SlotStatus.BOOKED -> teamName.ifEmpty { "Booked" }
                        SlotStatus.PAST   -> "Past"
                        else              -> "Closed"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
            if (status == SlotStatus.BOOKED && village.isNotEmpty()) {
                Text(village, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun DayChip(date: LocalDate, selected: Boolean, hasBooking: Boolean, onClick: () -> Unit) {
    val today   = LocalDate.now()
    val isToday = date == today
    val bg by animateColorAsState(
        targetValue = when {
            selected -> SportGreenMid
            isToday  -> MaterialTheme.colorScheme.primaryContainer
            else     -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200), label = "dayChipBg"
    )
    Surface(
        modifier = Modifier.width(58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), color = bg,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                date.dayOfMonth.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            if (isToday || hasBooking) {
                Box(
                    Modifier.size(5.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (selected) Color.White else if (hasBooking) SlotBooked else SportOrange)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CalStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
    }
}
