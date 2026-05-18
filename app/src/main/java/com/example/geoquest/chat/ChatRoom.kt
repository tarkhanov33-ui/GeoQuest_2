package com.example.geoquest.chat

import com.example.geoquest.chat.ChatRoom
import com.example.geoquest.R


import com.google.firebase.Timestamp

data class ChatRoom(
    val id: String = "",
    val questId: String = "",
    val questTitle: String = "",
    val creatorId: String = "",
    val seekerIds: List<String> = emptyList(), 
    val invitedIds: List<String> = emptyList(), 
    val status: String = "ACTIVE", 
    val updatedAt: Timestamp = Timestamp.now()
)


