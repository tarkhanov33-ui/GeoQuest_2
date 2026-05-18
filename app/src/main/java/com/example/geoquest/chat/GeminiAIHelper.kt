package com.example.geoquest.chat

import android.util.Log
import com.example.geoquest.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeminiAIHelper {

    private val db = FirebaseFirestore.getInstance()

    private val generativeModel by lazy {
        val key = BuildConfig.GEMINI_API_KEY.replace("\"", "").replace("'", "").trim()
        
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            requestOptions = RequestOptions(apiVersion = "v1")
        )
    }

    fun askAIForHint(
        questId: String,
        onHintGenerated: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (questId.isEmpty()) return

        db.collection("quests").document(questId).get()
            .addOnSuccessListener { document ->
                val title = document.getString("title") ?: "Unknown"
                val desc = document.getString("description") ?: "No description"
                
                generateHint(title, desc, onHintGenerated, onError)
            }
            .addOnFailureListener { e ->
                onError("Database error: ${e.localizedMessage}")
            }
    }

    private fun generateHint(
        title: String,
        desc: String,
        onHintGenerated: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val prompt = "You are a mysterious guide. Provide a short 1-sentence hint in English " +
                "for a quest titled '$title' with description '$desc'. " +
                "Use plain text only, NO markdown, NO bolding with asterisks."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val hintText = response.text?.replace("*", "")

                withContext(Dispatchers.Main) {
                    if (!hintText.isNullOrEmpty()) {
                        onHintGenerated(hintText.trim())
                    } else {
                        onError("AI returned an empty hint.")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAI", "API Error", e)
                withContext(Dispatchers.Main) {
                    val msg = e.localizedMessage ?: "Unknown error"
                    if (msg.contains("Unexpected Response")) {
                        onError("Wait a moment and try again (Limit reached).")
                    } else {
                        onError("AI Error: $msg")
                    }
                }
            }
        }
    }
}
