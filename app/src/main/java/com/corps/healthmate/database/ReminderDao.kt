package com.corps.healthmate.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(reminder: Reminder)

    @Update
    fun update(reminder: Reminder)

    @Delete
    fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY time ASC")
    fun getAllRemindersForUser(userId: String): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY time ASC")
    fun getAllRemindersForUserSync(userId: String): List<Reminder>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND isActive = 1 ORDER BY nextTriggerTime ASC")
    fun getActiveRemindersForUserSync(userId: String): List<Reminder>

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :reminderId")
    fun deactivateReminder(reminderId: Int)

    @Query("UPDATE reminders SET lastTakenTime = :time WHERE id = :reminderId")
    fun updateLastTakenTime(reminderId: Int, time: Long)

    @Query("UPDATE reminders SET nextTriggerTime = :time WHERE id = :reminderId")
    fun updateNextTriggerTime(reminderId: Int, time: Long)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    fun deleteAllRemindersForUser(userId: String)

    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    fun getActiveReminders(): List<Reminder>
}
