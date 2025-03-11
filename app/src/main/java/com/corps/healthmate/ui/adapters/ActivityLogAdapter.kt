package com.corps.healthmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.data.model.ActivityLog
import com.corps.healthmate.utils.TimeUtils.getRelativeTime

import java.text.SimpleDateFormat
import java.util.*

class ActivityLogAdapter(private val activityLogs: List<ActivityLog>) : RecyclerView.Adapter<ActivityLogAdapter.ActivityLogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity_log, parent, false)
        return ActivityLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        val log = activityLogs[position]
        holder.bind(log)
    }

    override fun getItemCount(): Int {
        return activityLogs.size
    }

    inner class ActivityLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(log: ActivityLog) {
            stepsTextView.text = "Walked ${log.steps} steps"
            timestampTextView.text = getRelativeTime(log.timestamp)
        }
    }
} 