package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R

class ChatAiAdapter(private val chatMessageAis: List<ChatMessageAi>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        val message = chatMessageAis[position]
        return if (message.userMessage != null) {
            VIEW_TYPE_USER // User message
        } else {
            VIEW_TYPE_AI // AI message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_message, parent, false)
            return UserMessageViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_ai_message, parent, false)
            return AiMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = chatMessageAis[position]

        if (holder.itemViewType == VIEW_TYPE_USER) {
            (holder as UserMessageViewHolder).bind(chatMessage.userMessage)
        } else {
            (holder as AiMessageViewHolder).bind(chatMessage.aiResponse)
        }
    }

    override fun getItemCount(): Int {
        return chatMessageAis.size
    }

    // ViewHolder for user messages
    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userMessageTextView: TextView =
            itemView.findViewById(R.id.user_message_textview)

        fun bind(userMessage: String?) {
            userMessageTextView.text = userMessage
        }
    }

    // ViewHolder for AI messages
    inner class AiMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val aiMessageTextView: TextView = itemView.findViewById(R.id.ai_message_textview)

        fun bind(aiMessage: String?) {
            aiMessageTextView.text = aiMessage
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }
}
