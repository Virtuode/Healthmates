package com.corps.healthmate.database



import androidx.room.Dao
import androidx.room.Insert
import com.corps.healthmate.models.ActivityLog


@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insert(log: ActivityLog)
}