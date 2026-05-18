package com.example.geoquest.chat

import com.example.geoquest.chat.ChatMessage
import com.example.geoquest.R


import com.google.firebase.Timestamp

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null
)


