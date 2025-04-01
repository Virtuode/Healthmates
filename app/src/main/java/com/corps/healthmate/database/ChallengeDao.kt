package com.corps.healthmate.database




import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.corps.healthmate.models.Challenge



@Dao
interface ChallengeDao {
    @Insert
    suspend fun insert(challenge: Challenge)

    @Update
    suspend fun update(challenge: Challenge)

    @Query("SELECT * FROM challenges WHERE status = 'active' LIMIT 1")
    suspend fun getActiveChallenge(): Challenge?
}