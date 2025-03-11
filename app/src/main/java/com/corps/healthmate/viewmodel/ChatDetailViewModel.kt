package com.corps.healthmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corps.healthmate.models.Message
import com.corps.healthmate.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private var currentChatId: String? = null

    fun setChatId(chatId: String) {
        currentChatId = chatId
        loadMessages()
    }

    private fun loadMessages() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                chatRepository.getMessages(chatId).collect { messagesList ->
                    _messages.value = messagesList
                }
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || currentChatId == null) return

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(currentChatId!!, message)
            } catch (e: Exception) {
                // Handle error (e.g., show toast in activity)
            }
        }
    }
}