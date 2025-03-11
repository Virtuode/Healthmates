package com.corps.healthmate.utils

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeConverter {
    private const val TAG = "TimeConverter"
    private const val TIME_FORMAT = "hh:mm a"

    fun convertToMillis(timeString: String?): Long {
        if (timeString == null || timeString.isEmpty()) {
            Log.e(TAG, "Invalid time string: null or empty")
            return System.currentTimeMillis()
        }

        try {
            val sdf = SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH)
            sdf.isLenient = false
            val date = sdf.parse(timeString)
                ?: throw ParseException("Failed to parse time string", 0)

            val calendar = Calendar.getInstance()
            val timeCalendar = Calendar.getInstance()
            timeCalendar.time = date

            calendar[Calendar.HOUR_OF_DAY] = timeCalendar[Calendar.HOUR_OF_DAY]
            calendar[Calendar.MINUTE] = timeCalendar[Calendar.MINUTE]
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            return calendar.timeInMillis
        } catch (e: ParseException) {
            Log.e(TAG, "Error converting time string: $timeString", e)
            return System.currentTimeMillis()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error converting time string: $timeString", e)
            return System.currentTimeMillis()
        }
    }

    fun formatTimeForDisplay(timeString: String): String {
        try {
            val inputFormat = SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val date = inputFormat.parse(timeString)
            return if (date != null) outputFormat.format(date) else timeString
        } catch (e: ParseException) {
            Log.e(TAG, "Error formatting time: $timeString", e)
            return timeString
        }
    }
}