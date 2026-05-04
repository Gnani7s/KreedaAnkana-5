package com.kreedaankana.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kreedaankana.data.local.dao.*
import com.kreedaankana.data.local.entities.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE teams ADD COLUMN draws INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE match_results ADD COLUMN isDraw INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add team ID and winnerId columns to match_results for proper stat tracking
        database.execSQL("ALTER TABLE match_results ADD COLUMN team1Id TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE match_results ADD COLUMN team2Id TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE match_results ADD COLUMN winnerId TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [TeamEntity::class, SlotEntity::class, ChallengeEntity::class, MatchResultEntity::class],
    version  = 3,
    exportSchema = false
)
abstract class KreedaDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun slotDao(): SlotDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun matchResultDao(): MatchResultDao
}
