package com.corps.healthmate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.corps.healthmate.R
import com.corps.healthmate.databinding.ItemChatPatientBinding
import com.corps.healthmate.models.Chat
import com.corps.healthmate.viewmodel.ChatViewModel

class ChatAdapterPatient(
    private val context: Context,
    private val onChatClick: (Chat) -> Unit,
    private val viewModel: ChatViewModel
) : ListAdapter<Chat, ChatAdapterPatient.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        holder.bind(chat)
    }

    inner class ChatViewHolder(private val binding: ItemChatPatientBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) onChatClick(getItem(position))
            }
        }

        fun bind(chat: Chat) {
            binding.tvDoctorName.text = chat.doctorName ?: "Unknown Doctor"
            binding.lastMessage.text = chat.lastMessage.takeIf { it.isNotBlank() } ?: "No messages yet"
            binding.lastMessage.isSingleLine = true
            binding.lastMessage.maxLines = 1

            val isAvailable = viewModel.isChatTimeValid(chat)
            binding.root.alpha = if (isAvailable) 1.0f else 0.5f
            if (isAvailable) {
                binding.tvAppointmentTime.text = "Available now"
                binding.tvAppointmentTime.setTextColor(context.getColor(R.color.green_501))
            } else {
                val remainingDays = chat.remainingDays
                binding.tvAppointmentTime.text = when {
                    remainingDays > 1 -> "In $remainingDays days"
                    remainingDays == 1 -> "Tomorrow"
                    remainingDays == 0 -> "Later today"
                    else -> "Time TBD"
                }
                binding.tvAppointmentTime.setTextColor(context.getColor(android.R.color.darker_gray))
            }

            Glide.with(context)
                .load(chat.doctorImageUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.userpro)
                .error(R.drawable.userpro)
                .override(100, 100)
                .into(binding.doctorProfileImage)
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem == newItem
    }
}