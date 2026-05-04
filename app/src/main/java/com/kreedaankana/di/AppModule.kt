package com.kreedaankana.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kreedaankana.data.local.KreedaDatabase
import com.kreedaankana.data.local.MIGRATION_1_2
import com.kreedaankana.data.local.MIGRATION_2_3
import com.kreedaankana.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return try {
            val fs = Firebase.firestore
            fs.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            fs
        } catch (e: Exception) {
            Firebase.firestore
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KreedaDatabase =
        Room.databaseBuilder(context, KreedaDatabase::class.java, "kreeda_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTeamDao(db: KreedaDatabase): TeamDao = db.teamDao()
    @Provides fun provideSlotDao(db: KreedaDatabase): SlotDao = db.slotDao()
    @Provides fun provideChallengeDao(db: KreedaDatabase): ChallengeDao = db.challengeDao()
    @Provides fun provideMatchResultDao(db: KreedaDatabase): MatchResultDao = db.matchResultDao()
}
