package com.kreedaankana.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kreeda_prefs")

/** Lightweight data class stored per registered team on this device */
data class SavedTeamProfile(
    val teamId: String,
    val teamName: String,
    val captainName: String,
    val captainPhone: String,
    val village: String,
    val sport: String
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store   = context.dataStore
    private val gson    = Gson()

    companion object {
        val KEY_ACTIVE_TEAM_ID  = stringPreferencesKey("team_id")
        val KEY_TEAM_NAME       = stringPreferencesKey("team_name")
        val KEY_CAPTAIN_NAME    = stringPreferencesKey("captain_name")
        val KEY_CAPTAIN_PHONE   = stringPreferencesKey("captain_phone")
        val KEY_VILLAGE         = stringPreferencesKey("village")
        val KEY_SPORT           = stringPreferencesKey("sport")
        val KEY_IS_REGISTERED   = booleanPreferencesKey("is_registered")
        /** JSON array of SavedTeamProfile — all teams ever registered on this device */
        val KEY_SAVED_PROFILES  = stringPreferencesKey("saved_profiles")
    }

    val isRegistered: Flow<Boolean> = store.data.map { it[KEY_IS_REGISTERED] ?: false }
    val teamId:       Flow<String>  = store.data.map { it[KEY_ACTIVE_TEAM_ID] ?: "" }
    val teamName:     Flow<String>  = store.data.map { it[KEY_TEAM_NAME]      ?: "" }
    val captainName:  Flow<String>  = store.data.map { it[KEY_CAPTAIN_NAME]   ?: "" }
    val captainPhone: Flow<String>  = store.data.map { it[KEY_CAPTAIN_PHONE]  ?: "" }
    val village:      Flow<String>  = store.data.map { it[KEY_VILLAGE]        ?: "" }
    val sport:        Flow<String>  = store.data.map { it[KEY_SPORT]          ?: "" }

    /** All team profiles saved on this device (for account switching) */
    val savedProfiles: Flow<List<SavedTeamProfile>> = store.data.map { prefs ->
        val json = prefs[KEY_SAVED_PROFILES] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<SavedTeamProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /** Legacy compat — list of teamIds */
    val allTeamIds: Flow<List<String>> = savedProfiles.map { list -> list.map { it.teamId } }

    suspend fun saveTeam(
        teamId: String, teamName: String,
        captainName: String, captainPhone: String,
        village: String, sport: String
    ) {
        store.edit { prefs ->
            // Upsert into saved profiles JSON
            val existing = parseProfiles(prefs)
            val updated  = existing.filter { it.teamId != teamId }.toMutableList()
            updated.add(SavedTeamProfile(teamId, teamName, captainName, captainPhone, village, sport))
            prefs[KEY_SAVED_PROFILES] = gson.toJson(updated)

            prefs[KEY_ACTIVE_TEAM_ID] = teamId
            prefs[KEY_TEAM_NAME]      = teamName
            prefs[KEY_CAPTAIN_NAME]   = captainName
            prefs[KEY_CAPTAIN_PHONE]  = captainPhone
            prefs[KEY_VILLAGE]        = village
            prefs[KEY_SPORT]          = sport
            prefs[KEY_IS_REGISTERED]  = true
        }
    }

    /** Switch active team without clearing saved profiles */
    suspend fun switchTeam(profile: SavedTeamProfile) {
        store.edit { prefs ->
            prefs[KEY_ACTIVE_TEAM_ID] = profile.teamId
            prefs[KEY_TEAM_NAME]      = profile.teamName
            prefs[KEY_CAPTAIN_NAME]   = profile.captainName
            prefs[KEY_CAPTAIN_PHONE]  = profile.captainPhone
            prefs[KEY_VILLAGE]        = profile.village
            prefs[KEY_SPORT]          = profile.sport
            prefs[KEY_IS_REGISTERED]  = true
        }
    }

    /** Full logout — clears active session but preserves saved profiles so user can log back in */
    suspend fun logout() {
        store.edit { prefs ->
            prefs[KEY_ACTIVE_TEAM_ID] = ""
            prefs[KEY_TEAM_NAME]      = ""
            prefs[KEY_CAPTAIN_NAME]   = ""
            prefs[KEY_CAPTAIN_PHONE]  = ""
            prefs[KEY_VILLAGE]        = ""
            prefs[KEY_SPORT]          = ""
            prefs[KEY_IS_REGISTERED]  = false
            // NOTE: KEY_SAVED_PROFILES is intentionally kept — so switch accounts still works
        }
    }

    /** Full wipe (e.g. debug / uninstall) */
    suspend fun clear() { store.edit { it.clear() } }

    private fun parseProfiles(prefs: Preferences): List<SavedTeamProfile> {
        val json = prefs[KEY_SAVED_PROFILES] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedTeamProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
