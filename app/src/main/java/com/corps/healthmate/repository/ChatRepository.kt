package com.corps.healthmate.repository

import com.corps.healthmate.models.Chat
import com.corps.healthmate.models.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getActiveChats(): Flow<List<Chat>>
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(chatId: String, message: String)


}