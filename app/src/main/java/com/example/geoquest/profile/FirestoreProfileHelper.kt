package com.example.geoquest.profile

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class FirestoreProfileHelper {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    fun observeUserData(onUpdate: (UserProfileData?) -> Unit): ListenerRegistration? {
        val uid = currentUser?.uid ?: return null

        return db.collection("users").document(uid).addSnapshotListener { document, e ->
            if (e != null || document == null || !document.exists()) {
                onUpdate(null)
                return@addSnapshotListener
            }

            val data = UserProfileData(
                username = document.getString("username") ?: "",
                city = document.getString("city") ?: "",
                score = document.getLong("score") ?: 0L,
                questsCompleted = document.getLong("questsCompleted") ?: 0L,
                level = document.getLong("level") ?: 1L,
                xp = document.getLong("xp") ?: 0L,
                profileImageUrl = document.getString("profileImageUrl"),
                friends = document.get("friends") as? List<String> ?: emptyList()
            )
            onUpdate(data)
        }
    }

    fun updateProfile(username: String, city: String, onComplete: (Boolean) -> Unit) {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf("username" to username, "city" to city))
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun addFriend(friendUsername: String, onComplete: (isSuccess: Boolean, message: String) -> Unit) {
        val uid = currentUser?.uid ?: return

        db.collection("users").whereEqualTo("username", friendUsername).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    db.collection("users").document(uid)
                        .update("friends", FieldValue.arrayUnion(friendUsername))
                        .addOnSuccessListener { onComplete(true, "Frend added!") }
                        .addOnFailureListener { onComplete(false, "Server error") }
                } else {
                    onComplete(false, "Choice other nickname")
                }
            }
            .addOnFailureListener { onComplete(false, "Net error") }
    }

    fun observeGlobalLeaderboard(onUpdate: (List<LeaderboardUser>) -> Unit): ListenerRegistration? {
        return db.collection("users")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val topList = snapshot.documents.mapIndexed { index, doc ->
                    LeaderboardUser(
                        rank = index + 1,
                        username = doc.getString("username") ?: "",
                        score = doc.getLong("score") ?: 0L,
                        questsCompleted = doc.getLong("questsCompleted") ?: 0L,
                        profileImageUrl = doc.getString("profileImageUrl"),
                        isCurrentUser = false // Будет вычисляться во Фрагменте
                    )
                }
                onUpdate(topList)
            }
    }

    fun uploadProfileImage(uri: Uri, onComplete: (isSuccess: Boolean) -> Unit) {
        val uid = currentUser?.uid ?: return
        val ref = storage.reference.child("profile_images/$uid")

        ref.putFile(uri).continueWithTask { ref.downloadUrl }
            .addOnSuccessListener { downloadUrl ->
                db.collection("users").document(uid).update("profileImageUrl", downloadUrl.toString())
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }
    fun fetchFriendsData(friendsNames: List<String>, onComplete: (List<LeaderboardUser>) -> Unit) {
        if (friendsNames.isEmpty()) {
            onComplete(emptyList())
            return
        }

        db.collection("users")
            .whereIn("username", friendsNames)
            .get()
            .addOnSuccessListener { snapshot ->
                val friendsList = snapshot.documents.map { doc ->
                    LeaderboardUser(
                        username = doc.getString("username") ?: "",
                        score = doc.getLong("score") ?: 0L,
                        questsCompleted = doc.getLong("questsCompleted") ?: 0L,
                        profileImageUrl = doc.getString("profileImageUrl")
                    )
                }
                onComplete(friendsList)
            }
    }
}