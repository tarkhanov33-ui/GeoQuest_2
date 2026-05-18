package com.example.geoquest.map

import com.google.firebase.firestore.GeoPoint

data class QuestLocation(
    val id: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: String = "Normal",
    val reward: String = "",
    val hint: String = "",
    val duration: String = "",
    val radius: Double = 200.0,
    val imageUrl: String? = null,
    val averageRating: Double = 0.0,
    val ratingCount: Long = 0,
    val status: String = "ACTIVE",
    val coordinate: GeoPoint? = null
)