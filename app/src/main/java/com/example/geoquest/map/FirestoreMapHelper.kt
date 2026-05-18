package com.example.geoquest.map

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class FirestoreMapHelper {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    fun observeQuests(onUpdate: (List<QuestLocation>) -> Unit): ListenerRegistration {
        return db.collection("quests").addSnapshotListener { snapshot, _ ->
            val quests = snapshot?.documents?.mapNotNull {
                it.toObject(QuestLocation::class.java)?.copy(id = it.id)
            } ?: emptyList()
            onUpdate(quests)
        }
    }

    fun observeChatStatuses(onUpdate: (Map<String, String>) -> Unit): ListenerRegistration? {
        val uid = currentUser?.uid ?: return null
        return db.collection("chat_rooms")
            .whereArrayContains("seekerIds", uid)
            .addSnapshotListener { snapshot, _ ->
                val statusMap = snapshot?.documents?.associate {
                    (it.getString("questId") ?: "") to (it.getString("status") ?: "")
                } ?: emptyMap()
                onUpdate(statusMap)
            }
    }

    fun uploadPhoto(uri: Uri, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val uid = currentUser?.uid ?: return
        val ref = storage.reference.child("quest_images/$uid/${UUID.randomUUID()}")
        ref.putFile(uri).continueWithTask { task -> ref.downloadUrl }
            .addOnSuccessListener { onSuccess(it.toString()) }
            .addOnFailureListener { onError(it) }
    }

    fun prepareChatRoom(quest: QuestLocation, uid: String, onComplete: (String?) -> Unit) {
        db.collection("chat_rooms")
            .whereEqualTo("questId", quest.id)
            .whereArrayContains("seekerIds", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    onComplete(documents.documents[0].id)
                } else {
                    val newRoom = hashMapOf(
                        "questId" to quest.id,
                        "questTitle" to quest.title,
                        "creatorId" to quest.creatorId,
                        "seekerIds" to listOf(uid),
                        "status" to "ACTIVE",
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("chat_rooms").add(newRoom)
                        .addOnSuccessListener { onComplete(it.id) }
                        .addOnFailureListener { onComplete(null) }
                }
            }
    }

    fun submitRating(questId: String, rating: Float, comment: String, onComplete: (Boolean) -> Unit) {
        val uid = currentUser?.uid ?: return
        val questRef = db.collection("quests").document(questId)
        val reviewRef = db.collection("reviews").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(questRef)
            val count = snapshot.getLong("ratingCount") ?: 0L
            val avg = snapshot.getDouble("averageRating") ?: 0.0
            val newAvg = ((count * avg) + rating) / (count + 1)

            transaction.set(reviewRef, hashMapOf(
                "userId" to uid, "questId" to questId, "rating" to rating,
                "comment" to comment, "timestamp" to Timestamp.now()
            ))
            transaction.update(questRef, "ratingCount", count + 1, "averageRating", newAvg)
        }.addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun saveQuest(quest: QuestLocation, isEditing: Boolean, onComplete: (Boolean) -> Unit) {
        val ref = if (isEditing) db.collection("quests").document(quest.id) else db.collection("quests").document()
        ref.set(quest, SetOptions.merge()).addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}