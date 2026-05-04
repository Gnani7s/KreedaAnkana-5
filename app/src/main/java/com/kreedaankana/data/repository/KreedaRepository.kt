package com.kreedaankana.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kreedaankana.data.local.dao.*
import com.kreedaankana.data.local.entities.*
import com.kreedaankana.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class KreedaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val teamDao: TeamDao,
    private val slotDao: SlotDao,
    private val challengeDao: ChallengeDao,
    private val matchResultDao: MatchResultDao
) {
    private val TAG = "KreedaRepo"
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val teamsCol      get() = firestore.collection("teams")
    private val slotsCol      get() = firestore.collection("slots")
    private val challengesCol get() = firestore.collection("challenges")
    private val resultsCol    get() = firestore.collection("results")

    private suspend fun <T> tryFirebase(block: suspend () -> T): T? =
        withTimeoutOrNull(10_000L) {
            try { block() } catch (e: Exception) {
                Log.w(TAG, "Firebase: ${e.message}"); null
            }
        }

    // ── Teams ─────────────────────────────────────────────────────────────────

    suspend fun isTeamNameTaken(teamName: String): Boolean {
        return try {
            val snap = tryFirebase { teamsCol.whereEqualTo("name", teamName).get().await() }
            snap != null && !snap.isEmpty
        } catch (e: Exception) { false }
    }

    suspend fun createTeam(team: Team): Result<Team> {
        try {
            if (isTeamNameTaken(team.name))
                return Result.Error("Team name \"${team.name}\" is already taken.")
            val id     = UUID.randomUUID().toString()
            val withId = team.copy(id = id)
            teamDao.insertTeam(withId.toEntity())
            bgScope.launch { tryFirebase { teamsCol.document(id).set(withId).await() } }
            return Result.Success(withId)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to create team")
        }
    }

    suspend fun loginByTeamName(teamName: String, captainPhone: String): Result<Team> {
        try {
            val snap = tryFirebase { teamsCol.whereEqualTo("name", teamName).limit(1).get().await() }
                ?: return Result.Error("Network error. Check your connection.")
            if (snap.isEmpty) return Result.Error("No team found with name \"$teamName\".")
            val team = snap.documents[0].toObject(Team::class.java)
                ?: return Result.Error("Failed to read team data.")
            teamDao.insertTeam(team.toEntity())
            return Result.Success(team)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Login failed")
        }
    }

    /**
     * KEY FIX for W/D/L not updating:
     * - Single source of truth: Firestore only. Room is cache, never source.
     * - NO conflate() — we must never drop Firestore emissions.
     * - Room seed happens once at start; Firestore snapshot replaces it entirely.
     * - No dual-stream race condition.
     */
    fun observeTeams(): Flow<List<Team>> = callbackFlow {
        // Seed from Room once for instant display while Firestore loads
        val localTeams = teamDao.getAllTeamsOnce()
        if (localTeams.isNotEmpty()) trySend(localTeams.map { it.toModel() })

        val reg: ListenerRegistration = teamsCol
            .orderBy("wins", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.w(TAG, "Teams error", err); return@addSnapshotListener }
                val teams = snap?.documents?.mapNotNull { it.toObject(Team::class.java) } ?: return@addSnapshotListener
                bgScope.launch {
                    // Replace Room cache with authoritative Firestore data
                    teamDao.clearAll()
                    teamDao.insertAll(teams.map { it.toEntity() })
                    trySend(teams)
                }
            }
        awaitClose { reg.remove() }
    }

    // ── Slots ─────────────────────────────────────────────────────────────────

    /**
     * KEY FIX for Calendar glitch (BookMyShow-style real-time seats):
     * bookSlot now uses a Firestore TRANSACTION synchronously (awaited) so:
     * 1. Concurrency-safe: two users booking same slot → only one wins.
     * 2. Room is updated optimistically BEFORE the Firestore round-trip for instant UI.
     * 3. If Firestore rejects (conflict), Room is rolled back and error shown.
     * 4. The real-time Firestore listener in observeSlots pushes the result to ALL devices.
     */
    suspend fun bookSlot(slot: Slot, teamId: String, teamName: String, village: String): Result<Slot> {
        try {
            val slotId = "${slot.date}_${slot.startTime}_${slot.sport}"

            // Check Room cache first (fast path)
            val cached = slotDao.getSlotById(slotId)
            if (cached != null && cached.status == SlotStatus.BOOKED.name && cached.teamId != teamId) {
                return Result.Error("Slot already booked by ${cached.teamName}!")
            }

            val booked = slot.copy(id = slotId, status = SlotStatus.BOOKED.name,
                teamId = teamId, teamName = teamName, village = village)

            // 1. Optimistic local write — UI updates instantly
            slotDao.insertSlot(booked.toEntity())

            // 2. Firestore transaction — server-authoritative, prevents double-booking
            val result = tryFirebase {
                firestore.runTransaction { tx ->
                    val ref    = slotsCol.document(slotId)
                    val remote = tx.get(ref).toObject(Slot::class.java)
                    if (remote != null && remote.status == SlotStatus.BOOKED.name && remote.teamId != teamId)
                        throw Exception("Slot just booked by ${remote.teamName}. Please choose another.")
                    tx.set(ref, booked)
                }.await()
            }

            if (result == null) {
                // Firestore failed/timed-out — roll back Room optimistic write
                slotDao.deleteSlotById(slotId)
                return Result.Error("Booking failed — check your connection and try again.")
            }

            return Result.Success(booked)
        } catch (e: Exception) {
            // Roll back on any exception
            try { slotDao.deleteSlotById("${slot.date}_${slot.startTime}_${slot.sport}") } catch (_: Exception) {}
            return Result.Error(e.message ?: "Booking failed")
        }
    }

    /**
     * KEY FIX for Calendar glitch:
     * - Single Firestore listener. Room is only a seed cache.
     * - NO conflate() — we must not drop slot updates.
     * - Each Firestore snapshot replaces the Room cache for that date range.
     * - This means when any device books a slot, every other device sees it instantly.
     */
    fun observeSlots(fromDate: String): Flow<List<Slot>> = callbackFlow {
        // Seed from Room once
        val local = slotDao.getSlotsFromDateOnce(fromDate)
        if (local.isNotEmpty()) trySend(local.map { it.toModel() })

        val reg = slotsCol
            .whereGreaterThanOrEqualTo("date", fromDate)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.w(TAG, "Slots error", err); return@addSnapshotListener }
                val slots = snap?.documents?.mapNotNull { it.toObject(Slot::class.java) } ?: return@addSnapshotListener
                bgScope.launch {
                    // Full replace — ensures deleted/cancelled slots also disappear from cache
                    slotDao.deleteFromDate(fromDate)
                    if (slots.isNotEmpty()) slotDao.insertAll(slots.map { it.toEntity() })
                    trySend(slots)
                }
            }
        awaitClose { reg.remove() }
    }

    // ── Challenges ────────────────────────────────────────────────────────────

    suspend fun postChallenge(challenge: Challenge): Result<Challenge> {
        try {
            val id     = UUID.randomUUID().toString()
            val withId = challenge.copy(id = id)
            challengeDao.insertChallenge(withId.toEntity())
            bgScope.launch { tryFirebase { challengesCol.document(id).set(withId).await() } }
            return Result.Success(withId)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to post challenge")
        }
    }

    suspend fun acceptChallenge(challengeId: String, acceptorTeamId: String, acceptorTeamName: String): Result<Unit> {
        try {
            challengeDao.getChallengeById(challengeId)?.let {
                challengeDao.insertChallenge(it.copy(
                    status = ChallengeStatus.ACCEPTED.name,
                    acceptorTeamId = acceptorTeamId, acceptorTeamName = acceptorTeamName))
            }
            bgScope.launch {
                tryFirebase {
                    challengesCol.document(challengeId).update(mapOf(
                        "status" to ChallengeStatus.ACCEPTED.name,
                        "acceptorTeamId" to acceptorTeamId,
                        "acceptorTeamName" to acceptorTeamName
                    )).await()
                }
            }
            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to accept challenge")
        }
    }

    fun observeChallenges(): Flow<List<Challenge>> = callbackFlow {
        val local = challengeDao.getOpenChallengesOnce()
        if (local.isNotEmpty()) trySend(local.map { it.toModel() })

        val reg = challengesCol
            .whereEqualTo("status", ChallengeStatus.OPEN.name)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.mapNotNull { it.toObject(Challenge::class.java) } ?: return@addSnapshotListener
                bgScope.launch {
                    challengeDao.clearAll()
                    if (list.isNotEmpty()) challengeDao.insertAll(list.map { it.toEntity() })
                    trySend(list)
                }
            }
        awaitClose { reg.remove() }
    }

    // ── Results ───────────────────────────────────────────────────────────────

    /**
     * KEY FIX for Rankings/Profile W/D/L:
     * 1. Result saved to Room + Firestore (awaited, not fire-and-forget).
     * 2. Team counters updated via Firestore batch (awaited).
     * 3. The observeTeams() Firestore listener then fires automatically and
     *    pushes the updated wins/draws/losses to ALL devices in real-time.
     * 4. Local Room counters also updated immediately so offline UI is correct.
     */
    suspend fun postResult(result: MatchResult): Result<MatchResult> {
        try {
            val id     = UUID.randomUUID().toString()
            val withId = result.copy(id = id)

            // Save locally first
            matchResultDao.insertResult(withId.toEntity())
            updateLocalTeamCounters(withId)

            // Push to Firestore + update team counters atomically
            val ok = tryFirebase {
                // Write result
                resultsCol.document(id).set(withId).await()

                // Atomic counter update batch
                val batch = firestore.batch()
                val inc   = com.google.firebase.firestore.FieldValue.increment(1L)
                when {
                    withId.isDraw -> {
                        if (withId.team1Id.isNotEmpty()) batch.update(teamsCol.document(withId.team1Id), "draws", inc)
                        if (withId.team2Id.isNotEmpty()) batch.update(teamsCol.document(withId.team2Id), "draws", inc)
                    }
                    withId.winnerId.isNotEmpty() -> {
                        val loserId = if (withId.winnerId == withId.team1Id) withId.team2Id else withId.team1Id
                        batch.update(teamsCol.document(withId.winnerId), "wins", inc)
                        if (loserId.isNotEmpty()) batch.update(teamsCol.document(loserId), "losses", inc)
                    }
                }
                batch.commit().await()
            }
            if (ok == null) Log.w(TAG, "postResult: Firestore failed — local cache has correct values")
            return Result.Success(withId)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to post result")
        }
    }

    private suspend fun updateLocalTeamCounters(r: MatchResult) {
        try {
            if (r.isDraw) {
                if (r.team1Id.isNotEmpty()) teamDao.getTeamById(r.team1Id)?.let { teamDao.insertTeam(it.copy(draws = it.draws + 1)) }
                if (r.team2Id.isNotEmpty()) teamDao.getTeamById(r.team2Id)?.let { teamDao.insertTeam(it.copy(draws = it.draws + 1)) }
            } else if (r.winnerId.isNotEmpty()) {
                val loserId = if (r.winnerId == r.team1Id) r.team2Id else r.team1Id
                teamDao.getTeamById(r.winnerId)?.let { teamDao.insertTeam(it.copy(wins = it.wins + 1)) }
                if (loserId.isNotEmpty()) teamDao.getTeamById(loserId)?.let { teamDao.insertTeam(it.copy(losses = it.losses + 1)) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "updateLocalTeamCounters: ${e.message}")
        }
    }

    fun observeResults(): Flow<List<MatchResult>> = callbackFlow {
        val local = matchResultDao.getRecentResultsOnce()
        if (local.isNotEmpty()) trySend(local.map { it.toModel() })

        val reg = resultsCol
            .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.mapNotNull { it.toObject(MatchResult::class.java) } ?: return@addSnapshotListener
                bgScope.launch {
                    matchResultDao.clearAll()
                    if (list.isNotEmpty()) matchResultDao.insertAll(list.map { it.toEntity() })
                    trySend(list)
                }
            }
        awaitClose { reg.remove() }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────
fun Team.toEntity()           = TeamEntity(id, name, captainName, captainPhone, village, sport, wins, losses, draws)
fun TeamEntity.toModel()      = Team(id, name, captainName, captainPhone, village, sport, wins, losses, draws)
fun Slot.toEntity()           = SlotEntity(id, date, startTime, endTime, sport, status, teamId, teamName, village)
fun SlotEntity.toModel()      = Slot(id, date, startTime, endTime, sport, status, teamId, teamName, village)
fun Challenge.toEntity()      = ChallengeEntity(id, challengerTeamId, challengerTeamName, challengerVillage, sport, date, startTime, endTime, caption, status, acceptorTeamId, acceptorTeamName)
fun ChallengeEntity.toModel() = Challenge(id, challengerTeamId, challengerTeamName, challengerVillage, sport, date, startTime, endTime, caption, status, acceptorTeamId, acceptorTeamName)
fun MatchResult.toEntity()    = MatchResultEntity(id, team1Id, team1Name, team1Village, team1Score, team2Id, team2Name, team2Village, team2Score, sport, date, winnerId, winnerName, isDraw)
fun MatchResultEntity.toModel() = MatchResult(id, team1Id, team1Name, team1Village, team1Score, team2Id, team2Name, team2Village, team2Score, sport, date, winnerId, winnerName, isDraw)
