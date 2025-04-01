package com.corps.healthmate.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text", // text, image, etc.
    val status: String = "sent" // sent, delivered, read
)