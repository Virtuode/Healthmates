package com.corps.healthmate.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corps.healthmate.models.Chat
import com.corps.healthmate.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                chatRepository.getActiveChats().collect { chatList ->
                    _chats.value = chatList.sortedByDescending { it.appointmentTime }
                    if (_isLoading.value) {
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load chats: ${e.message}"
                _chats.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun refreshChats() {
        _isLoading.value = true
        loadChats()
    }

    fun isChatTimeValid(chat: Chat): Boolean {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
                isLenient = false
            }
            val appointmentTime = sdf.parse(chat.appointmentTime) ?: run {
                return false
            }
            val currentTime = Date()
            val isTimeValid = currentTime >= appointmentTime
            val isStatusValid = chat.status in listOf("confirmed", "pending")
            val isValid = isTimeValid && isStatusValid

            return isValid
        } catch (e: Exception) {
            return false
        }
    }

    fun clearError() {
        _error.value = null
    }

}