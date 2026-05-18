package com.example.geoquest.feed

import com.example.geoquest.map.QuestLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FirestoreFeedHelper {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var questSnapshotListener: ListenerRegistration? = null

    fun startListeningQuests(onQuestsUpdated: (List<QuestLocation>) -> Unit, onError: (Exception) -> Unit) {
        val uid = currentUser?.uid ?: return

        questSnapshotListener?.remove()
        questSnapshotListener = db.collection("quests")
            .whereNotEqualTo("status", "SOLVED")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                val newQuests = mutableListOf<QuestLocation>()
                snapshot?.documents?.forEach { doc ->
                    val quest = doc.toObject(QuestLocation::class.java)?.copy(id = doc.id)
                    if (quest != null && quest.creatorId != uid) {
                        newQuests.add(quest)
                    }
                }
                onQuestsUpdated(newQuests)
            }
    }

    fun acceptQuest(quest: QuestLocation, onResult: (isSuccess: Boolean, roomId: String?, message: String?) -> Unit) {
        val uid = currentUser?.uid ?: return

        db.collection("chat_rooms")
            .whereEqualTo("questId", quest.id)
            .whereArrayContains("seekerIds", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    onResult(true, documents.documents[0].id, null)
                } else {
                    val newRoom: Map<String, Any> = hashMapOf(
                        "questId" to (quest.id ?: ""),
                        "questTitle" to quest.title,
                        "creatorId" to quest.creatorId,
                        "seekerIds" to listOf(uid),
                        "invitedIds" to emptyList<String>(),
                        "status" to "ACTIVE",
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("chat_rooms").add(newRoom)
                        .addOnSuccessListener { docRef -> onResult(true, docRef.id, null) }
                        .addOnFailureListener { e -> onResult(false, null, e.message) }
                }
            }
            .addOnFailureListener { e -> onResult(false, null, e.message) }
    }

    fun stopListening() {
        questSnapshotListener?.remove()
    }
}