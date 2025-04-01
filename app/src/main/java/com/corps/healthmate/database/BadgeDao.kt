package com.corps.healthmate.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.corps.healthmate.models.Badge


@Dao
interface BadgeDao {
    @Insert
    suspend fun insert(badge: Badge)

    @Query("SELECT * FROM badges")
    suspend fun getAllBadges(): List<Badge>
}