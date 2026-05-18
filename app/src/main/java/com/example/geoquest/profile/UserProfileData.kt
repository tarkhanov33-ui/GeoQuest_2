package com.example.geoquest.profile

data class UserProfileData(
    val username: String = "",
    val city: String = "",
    val score: Long = 0L,
    val questsCompleted: Long = 0L,
    val level: Long = 1L,
    val xp: Long = 0L,
    val profileImageUrl: String? = null,
    val friends: List<String> = emptyList()
)