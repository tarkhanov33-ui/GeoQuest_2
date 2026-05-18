package com.example.geoquest.profile

import com.example.geoquest.profile.LeaderboardUser
import com.example.geoquest.R


data class LeaderboardUser(
    val rank: Int = 0,
    val username: String = "",
    val score: Long = 0,
    val questsCompleted: Long = 0,
    val profileImageUrl: String? = null,
    val isCurrentUser: Boolean = false
) {
    val rankTitle: String
        get() = when {
            questsCompleted >= 50 -> "Legend"
            questsCompleted >= 20 -> "Master"
            questsCompleted >= 10 -> "Explorer"
            questsCompleted >= 1 -> "Adventurer"
            else -> "Novice"
        }
}


