package com.kreedaankana.data.local.dao

import androidx.room.*
import com.kreedaankana.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY wins DESC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    /** One-shot (non-Flow) — used for seeding observeTeams without a dual-stream race */
    @Query("SELECT * FROM teams ORDER BY wins DESC")
    suspend fun getAllTeamsOnce(): List<TeamEntity>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: String): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Query("DELETE FROM teams")
    suspend fun clearAll()
}

@Dao
interface SlotDao {
    @Query("SELECT * FROM slots WHERE date >= :fromDate ORDER BY date ASC, startTime ASC")
    fun getSlotsFromDate(fromDate: String): Flow<List<SlotEntity>>

    /** One-shot seed query */
    @Query("SELECT * FROM slots WHERE date >= :fromDate ORDER BY date ASC, startTime ASC")
    suspend fun getSlotsFromDateOnce(fromDate: String): List<SlotEntity>

    @Query("SELECT * FROM slots WHERE date = :date ORDER BY startTime ASC")
    fun getSlotsByDate(date: String): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots WHERE id = :id")
    suspend fun getSlotById(id: String): SlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: SlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<SlotEntity>)

    /** Delete a single slot by id — used for rollback on booking failure */
    @Query("DELETE FROM slots WHERE id = :id")
    suspend fun deleteSlotById(id: String)

    /** Full replace for the observed date range — ensures stale slots are removed */
    @Query("DELETE FROM slots WHERE date >= :fromDate")
    suspend fun deleteFromDate(fromDate: String)

    @Query("DELETE FROM slots WHERE date < :beforeDate")
    suspend fun deletePastSlots(beforeDate: String)
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE status = 'OPEN' ORDER BY rowid DESC")
    fun getOpenChallenges(): Flow<List<ChallengeEntity>>

    /** One-shot seed */
    @Query("SELECT * FROM challenges WHERE status = 'OPEN' ORDER BY rowid DESC")
    suspend fun getOpenChallengesOnce(): List<ChallengeEntity>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getChallengeById(id: String): ChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(challenges: List<ChallengeEntity>)

    @Query("DELETE FROM challenges")
    suspend fun clearAll()
}

@Dao
interface MatchResultDao {
    @Query("SELECT * FROM match_results ORDER BY date DESC LIMIT 50")
    fun getRecentResults(): Flow<List<MatchResultEntity>>

    /** One-shot seed */
    @Query("SELECT * FROM match_results ORDER BY date DESC LIMIT 50")
    suspend fun getRecentResultsOnce(): List<MatchResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: MatchResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<MatchResultEntity>)

    @Query("DELETE FROM match_results")
    suspend fun clearAll()
}
