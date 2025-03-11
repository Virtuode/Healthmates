package com.corps.healthmate.models

class Notification {
    // Getters and Setters
    var title: String? = null
    var message: String? = null
    var type: String? = null
    var referenceId: String? = null
    var timestamp: Long = 0
    var isRead: Boolean = false

    // Required empty constructor for Firebase
    constructor()

    constructor(
        title: String?,
        message: String?,
        type: String?,
        referenceId: String?,
        timestamp: Long
    ) {
        this.title = title
        this.message = message
        this.type = type
        this.referenceId = referenceId
        this.timestamp = timestamp
        this.isRead = false
    }
}