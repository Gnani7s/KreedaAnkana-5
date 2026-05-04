package com.kreedaankana.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val captainName: String,
    val captainPhone: String,
    val village: String,
    val sport: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)

@Entity(tableName = "slots")
data class SlotEntity(
    @PrimaryKey val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val sport: String,
    val status: String,
    val teamId: String = "",
    val teamName: String = "",
    val village: String = ""
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String,
    val challengerTeamId: String,
    val challengerTeamName: String,
    val challengerVillage: String,
    val sport: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val caption: String,
    val status: String,
    val acceptorTeamId: String = "",
    val acceptorTeamName: String = ""
)

@Entity(tableName = "match_results")
data class MatchResultEntity(
    @PrimaryKey val id: String,
    val team1Id: String = "",
    val team1Name: String,
    val team1Village: String,
    val team1Score: String,
    val team2Id: String = "",
    val team2Name: String,
    val team2Village: String,
    val team2Score: String,
    val sport: String,
    val date: String,
    val winnerId: String = "",
    val winnerName: String,
    val isDraw: Boolean = false
)
