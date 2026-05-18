package com.example.geoquest.chat

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class FirestoreChatHelper {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    fun observeRoom(roomId: String, onUpdate: (ChatRoom?) -> Unit): ListenerRegistration {
        return db.collection("chat_rooms").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                onUpdate(snapshot?.toObject(ChatRoom::class.java))
            }
    }

    fun observeMessages(roomId: String, onNewMessages: (List<DocumentChange>) -> Unit): ListenerRegistration {
        return db.collection("chat_rooms").document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { onNewMessages(it.documentChanges) }
            }
    }

    fun sendMessage(roomId: String, text: String) {
        val currentUid = uid ?: return
        val message = ChatMessage(senderId = currentUid, text = text, timestamp = Timestamp.now())
        db.collection("chat_rooms").document(roomId).collection("messages").add(message)
        db.collection("chat_rooms").document(roomId).update("updatedAt", Timestamp.now())
    }

    fun uploadAndSendImage(roomId: String, uri: Uri, text: String, onComplete: (Boolean) -> Unit) {
        val currentUid = uid ?: return
        val ref = storage.reference.child("chat_images/$roomId/${UUID.randomUUID()}")

        ref.putFile(uri).continueWithTask { ref.downloadUrl }.addOnSuccessListener { url ->
            val message = ChatMessage(
                senderId = currentUid,
                text = text,
                imageUrl = url.toString(),
                timestamp = Timestamp.now()
            )
            db.collection("chat_rooms").document(roomId).collection("messages").add(message)
            onComplete(true)
        }.addOnFailureListener { onComplete(false) }
    }

    fun sendSystemMessage(roomId: String, text: String) {
        val message = ChatMessage(senderId = "SYSTEM", text = text, timestamp = Timestamp.now())
        db.collection("chat_rooms").document(roomId).collection("messages").add(message)
    }

    fun approveQuest(roomId: String, room: ChatRoom, onComplete: (Boolean) -> Unit) {
        db.collection("chat_rooms").document(roomId).update("status", "SOLVED")
        db.collection("quests").document(room.questId).update("status", "SOLVED")

        db.collection("quests").document(room.questId).get().addOnSuccessListener { questDoc ->
            val rewardPts = (questDoc.getString("reward")?.toIntOrNull() ?: 0).coerceIn(0, 50)

            db.runTransaction { transaction ->
                val usersData = room.seekerIds.map { seekerId ->
                    val userRef = db.collection("users").document(seekerId)
                    val snapshot = transaction.get(userRef)
                    Pair(userRef, snapshot)
                }

                for ((userRef, userSnapshot) in usersData) {
                    val score = userSnapshot.getLong("score") ?: 0L
                    val quests = userSnapshot.getLong("questsCompleted") ?: 0L
                    val xp = userSnapshot.getLong("xp") ?: 0L

                    val newXp = xp + rewardPts

                    transaction.update(
                        userRef,
                        "score", score + rewardPts,
                        "questsCompleted", quests + 1,
                        "xp", newXp,
                        "level", (newXp / 500) + 1
                    )
                }

                null
            }.addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }


    fun inviteFriend(roomId: String, friendUsername: String, onComplete: (String?) -> Unit) {
        db.collection("users").whereEqualTo("username", friendUsername).get().addOnSuccessListener { docs ->
            if (docs.isEmpty) {
                onComplete(null) // Юзер не найден
                return@addOnSuccessListener
            }

            val friendUid = docs.documents[0].id

            db.collection("chat_rooms").document(roomId)
                .update("seekerIds", FieldValue.arrayUnion(friendUid))
                .addOnSuccessListener {
                    onComplete(friendUid)
                }
        }
    }
}