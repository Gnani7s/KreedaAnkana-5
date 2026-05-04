package com.kreedaankana.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kreedaankana.model.*
import com.kreedaankana.ui.theme.*

// ── Sport Chip ────────────────────────────────────────────────────────────────
@Composable
fun SportChip(
    sport: Sport,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text("${sport.emoji} ${sport.displayName}", fontWeight = FontWeight.SemiBold) },
        modifier = modifier,
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// ── Slot Cell ─────────────────────────────────────────────────────────────────
@Composable
fun SlotCell(
    status: SlotStatus,
    teamName: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, label) = when (status) {
        SlotStatus.FREE        -> Pair(SlotAvailable, "Free")
        SlotStatus.BOOKED      -> Pair(SlotBooked, if (teamName.isNotEmpty()) teamName.take(8) else "Booked")
        SlotStatus.PAST        -> Pair(SlotPast, "Past")
        SlotStatus.MAINTENANCE -> Pair(SlotMaintenance, "Closed")
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg.copy(alpha = 0.85f))
            .clickable(enabled = status == SlotStatus.FREE, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ── Section Header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Challenge Card ────────────────────────────────────────────────────────────
@Composable
fun ChallengeCard(
    challenge: Challenge,
    currentTeamId: String,
    onAccept: (String) -> Unit
) {
    val sport = challenge.sportEnum
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sport.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        challenge.challengerTeamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        challenge.challengerVillage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
                Spacer(Modifier.weight(1f))
                SportBadge(sport)
            }

            Spacer(Modifier.height(8.dp))
            if (challenge.caption.isNotEmpty()) {
                Text(
                    "\"${challenge.caption}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoChip(Icons.Default.CalendarToday, challenge.date)
                InfoChip(Icons.Default.Schedule, "${challenge.startTime}–${challenge.endTime}")
            }

            if (challenge.challengerTeamId != currentTeamId) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = { onAccept(challenge.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = SportOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Accept Challenge", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SportGreenSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Your Challenge — waiting for opponent",
                        modifier = Modifier.padding(8.dp),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = SportGreenMid,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SportBadge(sport: Sport) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            sport.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── Result Card ───────────────────────────────────────────────────────────────
@Composable
fun ResultCard(result: com.kreedaankana.model.MatchResult) {
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
                Text(
                    try { Sport.valueOf(result.sport).let { "${it.emoji} ${it.displayName}" } }
                    catch (e: Exception) { result.sport },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(result.date, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                TeamScore(result.team1Name, result.team1Score,
                    isWinner = !result.isDraw && result.winnerName == result.team1Name)
                Text("VS", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontWeight = FontWeight.Black)
                TeamScore(result.team2Name, result.team2Score,
                    isWinner = !result.isDraw && result.winnerName == result.team2Name)
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (result.isDraw) MaterialTheme.colorScheme.secondaryContainer else SportGreenSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (result.isDraw) "🤝 Match Drawn"
                    else "🏆 ${result.winnerName} wins!",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (result.isDraw) MaterialTheme.colorScheme.onSecondaryContainer else SportGreenMid,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TeamScore(teamName: String, score: String, isWinner: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(teamName, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        Text(score, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = if (isWinner) SportOrange else MaterialTheme.colorScheme.onSurface)
    }
}

// ── Leaderboard Row ───────────────────────────────────────────────────────────
@Composable
fun LeaderboardRow(entry: com.kreedaankana.model.LeaderboardEntry) {
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurface.copy(0.4f)
    }
    val rankEmoji = when (entry.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${entry.rank}" }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (entry.rank == 1) SportGreenSurface else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (entry.rank <= 3) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rankEmoji, fontSize = 22.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Column(Modifier.weight(1f)) {
                Text(entry.teamName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${entry.village} • ${try { Sport.valueOf(entry.sport).displayName } catch (e: Exception) { entry.sport }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "W: ${entry.wins}  D: ${entry.draws}  L: ${entry.losses}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                val points = entry.wins * 2 + entry.draws
                Text(
                    "Pts: $points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(0.7f)
                )
            }
        }
    }
}

// ── Gradient Header Banner ────────────────────────────────────────────────────
@Composable
fun GradientHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(SportGreen, SportGreenMid))
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(content = content)
    }
}
