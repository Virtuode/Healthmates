package com.corps.healthmate.repository

import com.corps.healthmate.models.Chat
import com.corps.healthmate.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import jakarta.inject.Inject

class ChatRepositoryImpl @Inject constructor() : ChatRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun getActiveChats(): Flow<List<Chat>> = callbackFlow {
        val currentUser = auth.currentUser ?: run {
            Log.e("ChatRepository", "No authenticated user")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val chatsRef = database.reference.child("chats")
            .orderByChild("patientId")
            .equalTo(currentUser.uid)

        val listener = chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val chats = snapshot.children.mapNotNull { it.getValue(Chat::class.java) }
                        // Group by doctorId and take the most recent chat for each doctor
                        .groupBy { it.doctorId }
                        .map { (_, doctorChats) -> doctorChats.maxByOrNull { it.lastMessageTime }!! }
                        .sortedByDescending { it.lastMessageTime }
                    trySend(chats)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error processing chats", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error fetching chats: ${error.message}", error.toException())
                trySend(emptyList())
            }
        })

        awaitClose {
            chatsRef.removeEventListener(listener)
        }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val messagesRef = database.reference.child("messages").child(chatId)
            .orderByChild("timestamp")

        val listener = messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error fetching messages: ${error.message}")
                trySend(emptyList())
            }
        })

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    override suspend fun sendMessage(chatId: String, message: String) {
        val currentUser = auth.currentUser ?: run {
            Log.e("ChatRepository", "No authenticated user")
            return
        }

        val messageRef = database.reference.child("messages").child(chatId).push()
        val newMessage = Message(
            id = messageRef.key ?: "",
            senderId = currentUser.uid,
            message = message
        )

        try {
            messageRef.setValue(newMessage).await()
            database.reference.child("chats").child(chatId)
                .updateChildren(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                ).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            throw e // Let the caller handle the error
        }
    }
}