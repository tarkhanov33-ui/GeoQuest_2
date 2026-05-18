package com.example.geoquest.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRegisterHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun registerUser(
        userMap:  Map<String, Any>,
        password: String,
        onComplete: (isSuccess: Boolean, message: String?) -> Unit
    )
    {
        val email = userMap["email"] as String

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    saveToFirestore(uid, userMap, onComplete)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }

    private fun saveToFirestore(
        uid: String,
        userMap: Map<String, Any>,
        onComplete: (isSuccess: Boolean, message: String?) -> Unit
    ) {
        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }
}