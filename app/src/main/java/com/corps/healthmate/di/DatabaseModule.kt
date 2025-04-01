package com.corps.healthmate.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.corps.healthmate.database.* // Adjusted to your package
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGamificationDatabase(@ApplicationContext context: Context): GamificationDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            GamificationDatabase::class.java,
            "gamification_database"
        ).fallbackToDestructiveMigration() // For dev; remove in production
            .build()
    }

    @Provides
    fun provideActivityLogDao(database: GamificationDatabase): ActivityLogDao = database.activityLogDao()

    @Provides
    fun provideChallengeDao(database: GamificationDatabase): ChallengeDao = database.challengeDao()

    @Provides
    fun provideUserProfileDao(database: GamificationDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun provideBadgeDao(database: GamificationDatabase): BadgeDao = database.badgeDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}