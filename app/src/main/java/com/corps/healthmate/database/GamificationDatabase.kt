package com.corps.healthmate.database


import androidx.room.Database
import androidx.room.RoomDatabase
import com.corps.healthmate.models.ActivityLog
import com.corps.healthmate.models.Badge
import com.corps.healthmate.models.Challenge
import com.corps.healthmate.models.UserProfile


@Database(
    entities = [ActivityLog::class, Challenge::class, UserProfile::class, Badge::class],
    version = 1,
    exportSchema = false
)
abstract class GamificationDatabase : RoomDatabase() {
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun badgeDao(): BadgeDao
}