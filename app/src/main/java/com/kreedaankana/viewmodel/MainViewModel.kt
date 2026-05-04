package com.kreedaankana.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreedaankana.data.repository.KreedaRepository
import com.kreedaankana.data.repository.Result
import com.kreedaankana.model.*
import com.kreedaankana.utils.SavedTeamProfile
import com.kreedaankana.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class UiMessage(val text: String, val isError: Boolean = false)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: KreedaRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Session ───────────────────────────────────────────────────────────────
    val isRegistered    = prefs.isRegistered.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val currentTeamId   = prefs.teamId.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val currentTeamName = prefs.teamName.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val currentVillage  = prefs.village.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val currentSport    = prefs.sport.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val captainName     = prefs.captainName.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val savedProfiles: StateFlow<List<SavedTeamProfile>> =
        prefs.savedProfiles.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTeamIds: StateFlow<List<String>> =
        prefs.allTeamIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _message   = MutableStateFlow<UiMessage?>(null)
    val message = _message.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // ── Live Firestore streams ────────────────────────────────────────────────
    val today: String get() = LocalDate.now().format(dateFmt)

    /**
     * Slots — Eagerly started so the Firestore listener stays alive even when
     * navigating between tabs. This prevents the "booked slot disappears" glitch
     * caused by the stream restarting and re-seeding from stale Room cache.
     */
    val slots: StateFlow<List<Slot>> = repo.observeSlots(today)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val challenges: StateFlow<List<Challenge>> = repo.observeChallenges()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Teams — Eagerly started so the Firestore listener stays alive.
     * When postResult() commits the batch, Firestore pushes the updated team docs
     * here immediately, which flows through to leaderboard and myLeaderboardEntry.
     */
    val teams: StateFlow<List<Team>> = repo.observeTeams()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Leaderboard — derived from live team data.
     * Sorted by points (W×2 + D×1) DESC, then wins DESC, then losses ASC.
     */
    val leaderboard: StateFlow<List<LeaderboardEntry>> = teams.map { list ->
        list.sortedWith(
            compareByDescending<Team> { it.wins * 2 + it.draws }
                .thenByDescending { it.wins }
                .thenByDescending { it.draws }
                .thenBy { it.losses }
        ).mapIndexed { i, t ->
            LeaderboardEntry(t.id, t.name, t.village, t.sport, t.wins, t.losses, t.draws, i + 1)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val results: StateFlow<List<MatchResult>> = repo.observeResults()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Live profile stats for the currently active team */
    val myLeaderboardEntry: StateFlow<LeaderboardEntry?> =
        combine(leaderboard, currentTeamId) { lb, id -> lb.find { it.teamId == id } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Auth ──────────────────────────────────────────────────────────────────

    fun registerTeam(teamName: String, captainName: String, captainPhone: String, village: String, sport: Sport) {
        viewModelScope.launch {
            _isLoading.value = true
            val team = Team(name = teamName, captainName = captainName,
                captainPhone = captainPhone, village = village, sport = sport.name)
            when (val r = repo.createTeam(team)) {
                is Result.Success -> {
                    prefs.saveTeam(r.data.id, teamName, captainName, captainPhone, village, sport.name)
                    _message.value = UiMessage("Team '$teamName' registered! 🎉")
                }
                is Result.Error -> _message.value = UiMessage(r.message, true)
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun loginTeam(teamName: String, captainPhone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repo.loginByTeamName(teamName, captainPhone)) {
                is Result.Success -> {
                    val t = r.data
                    prefs.saveTeam(t.id, t.name, t.captainName, t.captainPhone, t.village, t.sport)
                    _message.value = UiMessage("Welcome back, ${t.captainName}! 👋")
                }
                is Result.Error -> _message.value = UiMessage(r.message, true)
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun switchTeam(profile: SavedTeamProfile) {
        viewModelScope.launch { prefs.switchTeam(profile); _message.value = UiMessage("Switched to ${profile.teamName} ✅") }
    }

    fun logout() { viewModelScope.launch { prefs.logout() } }

    // ── Match Actions ─────────────────────────────────────────────────────────

    fun bookSlot(date: String, startTime: String, endTime: String, sport: Sport) {
        viewModelScope.launch {
            _isLoading.value = true
            val slot = Slot(date = date, startTime = startTime, endTime = endTime, sport = sport.name)
            when (val r = repo.bookSlot(slot, currentTeamId.value, currentTeamName.value, currentVillage.value)) {
                is Result.Success -> _message.value = UiMessage("Slot booked for ${sport.displayName} at $startTime! ✅")
                is Result.Error   -> _message.value = UiMessage(r.message, true)
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun postChallenge(sport: Sport, date: String, startTime: String, endTime: String, caption: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val challenge = Challenge(
                challengerTeamId = currentTeamId.value, challengerTeamName = currentTeamName.value,
                challengerVillage = currentVillage.value, sport = sport.name,
                date = date, startTime = startTime, endTime = endTime, caption = caption)
            when (val r = repo.postChallenge(challenge)) {
                is Result.Success -> _message.value = UiMessage("Challenge posted! 🏆")
                is Result.Error   -> _message.value = UiMessage(r.message, true)
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun acceptChallenge(challengeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repo.acceptChallenge(challengeId, currentTeamId.value, currentTeamName.value)) {
                is Result.Success -> _message.value = UiMessage("Challenge accepted! Game on! 🔥")
                is Result.Error   -> _message.value = UiMessage(r.message, true)
                else -> {}
            }
            _isLoading.value = false
        }
    }

    /**
     * FIX: Team lookup is case-insensitive and trims whitespace.
     * Uses the live `teams` StateFlow (Eagerly started) so it always
     * has the latest data when postResult is called.
     */
    fun postResult(
        team1Name: String, team1Score: String,
        team2Name: String, team2Score: String,
        sport: Sport, date: String,
        winnerName: String, isDraw: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val opponent = teams.value.find { it.name.trim().equals(team2Name.trim(), ignoreCase = true) }
            val result = MatchResult(
                team1Id      = currentTeamId.value,
                team1Name    = team1Name,
                team1Village = currentVillage.value,
                team1Score   = team1Score,
                team2Id      = opponent?.id ?: "",
                team2Name    = team2Name,
                team2Village = opponent?.village ?: "",
                team2Score   = team2Score,
                sport        = sport.name,
                date         = date,
                winnerId     = when {
                    isDraw               -> ""
                    winnerName == team1Name -> currentTeamId.value
                    else                 -> opponent?.id ?: ""
                },
                winnerName   = if (isDraw) "" else winnerName,
                isDraw       = isDraw
            )
            when (val r = repo.postResult(result)) {
                is Result.Success -> _message.value = UiMessage(if (isDraw) "Draw posted! 🤝" else "Result posted! Rankings updated 📊")
                is Result.Error   -> _message.value = UiMessage(r.message, true)
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() { _message.value = null }

    // ── Helpers ───────────────────────────────────────────────────────────────
    val timeSlots = listOf(
        "06:00" to "08:00", "08:00" to "10:00", "10:00" to "12:00",
        "14:00" to "16:00", "16:00" to "18:00", "18:00" to "20:00"
    )

    fun getSlotStatus(date: String, startTime: String, sport: Sport): SlotStatus {
        val now      = LocalDate.now()
        val slotDate = try { LocalDate.parse(date, dateFmt) } catch (e: Exception) { now }
        if (slotDate.isBefore(now)) return SlotStatus.PAST
        val existing = slots.value.find {
            it.date == date && it.startTime == startTime && it.sport == sport.name
        }
        return existing?.statusEnum ?: SlotStatus.FREE
    }
}
