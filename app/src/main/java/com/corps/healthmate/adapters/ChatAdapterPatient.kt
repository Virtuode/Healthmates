package com.corps.healthmate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.corps.healthmate.R
import com.corps.healthmate.models.Chat
import com.corps.healthmate.viewmodel.ChatViewModel
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapterPatient(
    private val context: Context,
    private var chatList: List<Chat>,
    private val onChatClick: (Chat) -> Unit,
    private val viewModel: ChatViewModel
) : RecyclerView.Adapter<ChatAdapterPatient.ChatViewHolder>() {

    fun updateChats(newChats: List<Chat>) {
        chatList = newChats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_chat_patient, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.bind(chat)
        val isAvailable = viewModel.isChatTimeValid(chat)
        holder.itemView.alpha = if (isAvailable) 1.0f else 0.5f
        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val doctorNameTextView: TextView = itemView.findViewById(R.id.tv_doctor_name)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.last_message)
        private val appointmentTimeTextView: TextView = itemView.findViewById(R.id.tv_appointment_time)
        private val doctorProfileImageView: CircleImageView = itemView.findViewById(R.id.doctor_profile_image)

        fun bind(chat: Chat) {
            doctorNameTextView.text = chat.doctorName
            lastMessageTextView.text = chat.lastMessage ?: "No messages yet"

            val isAvailable = viewModel.isChatTimeValid(chat)
            if (isAvailable) {
                appointmentTimeTextView.text = "Available now"
                appointmentTimeTextView.setTextColor(context.getColor(R.color.green_501)) // Optional: Green for available
            } else {
                val remainingDays = chat.remainingDays
                appointmentTimeTextView.text = if (remainingDays > 0) "In $remainingDays days" else "Later today"
                appointmentTimeTextView.setTextColor(context.getColor(android.R.color.darker_gray))
            }

            if (!chat.doctorImageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(chat.doctorImageUrl)
                    .placeholder(R.drawable.userpro)
                    .error(R.drawable.userpro)
                    .into(doctorProfileImageView)
            } else {
                doctorProfileImageView.setImageResource(R.drawable.userpro)
            }
        }
    }
}