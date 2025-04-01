package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.models.Notification
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onMarkAsRead: (String) -> Unit,
    private val onBookAgain: (String, String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)
        val actionButton: MaterialButton = itemView.findViewById(R.id.action_button)
        val rescheduleButton: MaterialButton = itemView.findViewById(R.id.reschedule_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.messageText.text = notification.message
        holder.timestampText.text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
        holder.messageText.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (notification.read) R.color.grey_501 else R.color.black
            )
        )
        holder.actionButton.text = if (notification.read) "Mark Unread" else "Mark Read"
        holder.actionButton.setOnClickListener {
            onMarkAsRead(notification.id)
        }

        if (notification.type == "missed" && notification.appointmentId != null) {
            holder.rescheduleButton.visibility = View.VISIBLE
            holder.rescheduleButton.setOnClickListener {
                onBookAgain(notification.appointmentId, notification.id)
            }
        } else {
            holder.rescheduleButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = notifications.size
}