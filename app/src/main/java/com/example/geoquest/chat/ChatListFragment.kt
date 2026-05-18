package com.example.geoquest.chat

import com.example.geoquest.R
import com.example.geoquest.chat.ChatFragment
import com.example.geoquest.chat.ChatListAdapter
import com.example.geoquest.chat.ChatListFragment
import com.example.geoquest.chat.ChatRoom


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks

class ChatListFragment : Fragment() {

    private lateinit var rvChatList: RecyclerView
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var adapter: ChatListAdapter
    private val chatList = mutableListOf<ChatRoom>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        rvChatList = view.findViewById(R.id.rvChatList)
        fabNewChat = view.findViewById(R.id.fabNewChat)

        rvChatList.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChatListAdapter(
            chatList, 
            auth.currentUser?.uid ?: "",
            onChatClick = { chat -> openChat(chat) },
            onDeleteClick = { chat -> deleteChat(chat) }
        )
        rvChatList.adapter = adapter

        fabNewChat.setOnClickListener {
            
            createNewGenericChat()
        }

        loadChats()

        return view
    }

    private fun loadChats() {
        val uid = auth.currentUser?.uid ?: return

        val creatorQuery = db.collection("chat_rooms").whereEqualTo("creatorId", uid).get()

        val seekerQuery = db.collection("chat_rooms").whereArrayContains("seekerIds", uid).get()

        val invitedQuery = db.collection("chat_rooms").whereArrayContains("invitedIds", uid).get()

        Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(creatorQuery, seekerQuery, invitedQuery)
            .addOnSuccessListener { results ->
                if (!isAdded) return@addOnSuccessListener
                chatList.clear()
                val uniqueChats = mutableMapOf<String, ChatRoom>()
                
                for (snapshot in results) {
                    for (doc in snapshot.documents) {
                        try {
                            val room = doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                            if (room != null) {
                                uniqueChats[doc.id] = room
                            }
                        } catch (e: Exception) {
                            
                        }
                    }
                }
                
                chatList.addAll(uniqueChats.values.sortedByDescending { it.updatedAt })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                val ctx = context ?: return@addOnFailureListener
                Toast.makeText(ctx, "Error loading chats", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChat(chat: ChatRoom) {
        ChatFragment.currentRoomId = chat.id
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ChatFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun deleteChat(chat: ChatRoom) {
        db.collection("chat_rooms").document(chat.id).delete()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                val ctx = context ?: return@addOnSuccessListener
                Toast.makeText(ctx, "Chat room deleted", Toast.LENGTH_SHORT).show()
                loadChats()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                val ctx = context ?: return@addOnFailureListener
                Toast.makeText(ctx, "Error deleting chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewGenericChat() {
        val uid = auth.currentUser?.uid ?: return
        val newRoom = ChatRoom(
            id = "", 
            questId = "",
            questTitle = "Direct Message",
            creatorId = uid,
            seekerIds = listOf(), 
            invitedIds = emptyList(),
            status = "ACTIVE"
        )
        
        db.collection("chat_rooms").add(newRoom)
            .addOnSuccessListener { docRef ->
                if (!isAdded) return@addOnSuccessListener
                val ctx = context ?: return@addOnSuccessListener
                Toast.makeText(ctx, "New Chat Created", Toast.LENGTH_SHORT).show()
                openChat(newRoom.copy(id = docRef.id))
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                val ctx = context ?: return@addOnFailureListener
                Toast.makeText(ctx, "Failed to create chat", Toast.LENGTH_SHORT).show()
            }
    }
}


