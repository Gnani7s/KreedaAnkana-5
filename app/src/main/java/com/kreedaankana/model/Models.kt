package com.kreedaankana.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class Sport(val displayName: String, val emoji: String) {
    CRICKET("Cricket", "🏏"),
    VOLLEYBALL("Volleyball", "🏐"),
    KABADDI("Kabaddi", "🤼"),
    FOOTBALL("Football", "⚽"),
    BADMINTON("Badminton", "🏸")
}

enum class SlotStatus { FREE, BOOKED, MAINTENANCE, PAST }

enum class ChallengeStatus { OPEN, ACCEPTED, CANCELLED, COMPLETED }

// ── Firestore Models ──────────────────────────────────────────────────────────

data class Team(
    @DocumentId val id: String = "",
    val name: String = "",
    val captainName: String = "",
    val captainPhone: String = "",
    val village: String = "",
    val sport: String = Sport.CRICKET.name,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
) {
    val sportEnum: Sport get() = try { Sport.valueOf(sport) } catch (e: Exception) { Sport.CRICKET }
}

data class Slot(
    @DocumentId val id: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val sport: String = "",
    val status: String = SlotStatus.FREE.name,
    val teamId: String = "",
    val teamName: String = "",
    val village: String = "",
    val bookedAt: Timestamp? = null
) {
    val statusEnum: SlotStatus get() = try { SlotStatus.valueOf(status) } catch (e: Exception) { SlotStatus.FREE }
    val sportEnum: Sport get() = try { Sport.valueOf(sport) } catch (e: Exception) { Sport.CRICKET }
}

data class Challenge(
    @DocumentId val id: String = "",
    val challengerTeamId: String = "",
    val challengerTeamName: String = "",
    val challengerVillage: String = "",
    val sport: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val caption: String = "",
    val status: String = ChallengeStatus.OPEN.name,
    val acceptorTeamId: String = "",
    val acceptorTeamName: String = "",
    val postedAt: Timestamp = Timestamp.now()
) {
    val statusEnum: ChallengeStatus get() = try { ChallengeStatus.valueOf(status) } catch (e: Exception) { ChallengeStatus.OPEN }
    val sportEnum: Sport get() = try { Sport.valueOf(sport) } catch (e: Exception) { Sport.CRICKET }
}

data class MatchResult(
    @DocumentId val id: String = "",
    val team1Id: String = "",
    val team1Name: String = "",
    val team1Village: String = "",
    val team1Score: String = "",
    val team2Id: String = "",
    val team2Name: String = "",
    val team2Village: String = "",
    val team2Score: String = "",
    val sport: String = "",
    val date: String = "",
    val winnerId: String = "",
    val winnerName: String = "",
    val isDraw: Boolean = false,
    val postedAt: Timestamp = Timestamp.now()
)

// ── UI State helpers ──────────────────────────────────────────────────────────

data class LeaderboardEntry(
    val teamId: String,
    val teamName: String,
    val village: String,
    val sport: String,
    val wins: Int,
    val losses: Int,
    val draws: Int = 0,
    val rank: Int
)
