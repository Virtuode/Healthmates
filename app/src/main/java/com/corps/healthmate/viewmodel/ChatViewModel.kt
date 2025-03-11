package com.corps.healthmate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corps.healthmate.models.Chat
import com.corps.healthmate.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.getActiveChats().collect { chatList ->
                _chats.value = chatList
                Log.d("ChatViewModel", "Loaded chats: $chatList")
            }
        }
    }

    fun isChatTimeValid(chat: Chat): Boolean {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentTime = sdf.parse(chat.appointmentTime) ?: run {
                Log.e("ChatViewModel", "Failed to parse appointmentTime: ${chat.appointmentTime}")
                return false
            }
            val currentTime = Date()
            Log.d("ChatViewModel", "Chat ID: ${chat.id}")
            Log.d("ChatViewModel", "Raw appointmentTime: ${chat.appointmentTime}")
            Log.d("ChatViewModel", "Parsed appointmentTime: $appointmentTime (${appointmentTime.time} ms)")
            Log.d("ChatViewModel", "Current time: $currentTime (${currentTime.time} ms)")
            Log.d("ChatViewModel", "Status: ${chat.status}")
            val isValid = currentTime >= appointmentTime && (chat.status == "confirmed" || chat.status == "pending")
            Log.d("ChatViewModel", "Is valid? $isValid")
            return isValid
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Exception parsing time for chat ${chat.id}: ${chat.appointmentTime}", e)
            return false
        }
    }
}