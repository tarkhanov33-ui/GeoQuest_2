package com.example.geoquest.chat

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geoquest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ChatFragment : Fragment() {

    companion object { var currentRoomId: String? = null }

    private val chatHelper = FirestoreChatHelper()
    private val aiHelper = GeminiAIHelper()
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private var roomListener: ListenerRegistration? = null
    private var msgListener: ListenerRegistration? = null
    private var currentChatRoom: ChatRoom? = null

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var tvHeader: TextView
    private lateinit var layoutCreator: View
    private lateinit var chatInput: View

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            chatHelper.uploadAndSendImage(currentRoomId!!, it, etMessage.text.toString()) {
                etMessage.text.clear()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        initViews(view)
        setupRecyclerView(view)
        return view
    }

    private fun initViews(v: View) {
        rvChat = v.findViewById(R.id.rvChat)
        etMessage = v.findViewById(R.id.etChatMessage)
        tvHeader = v.findViewById(R.id.tvChatHeaderName)
        layoutCreator = v.findViewById(R.id.layoutCreatorApproval)
        chatInput = v.findViewById(R.id.chat_input_layout)

        // Скрываем блок инвайтов, так как он больше не нужен
        v.findViewById<View>(R.id.layoutInviteApproval)?.visibility = View.GONE

        v.findViewById<View>(R.id.btnChatSend).setOnClickListener { sendMessage() }
        v.findViewById<View>(R.id.btnChatAttach).setOnClickListener { pickImage.launch("image/*") }
        v.findViewById<View>(R.id.btnApprove).setOnClickListener { approveQuest() }
        v.findViewById<View>(R.id.btnDecline).setOnClickListener {
            chatHelper.sendSystemMessage(currentRoomId!!, "Creator declined the solution.")
        }

        v.findViewById<View>(R.id.btnHintAI).setOnClickListener { askAI() }
        v.findViewById<View>(R.id.btnAddFriend).setOnClickListener { showFriendsDialog() }
    }

    override fun onResume() {
        super.onResume()
        currentRoomId?.let { startChat(it) }
    }

    private fun startChat(roomId: String) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid

        roomListener = chatHelper.observeRoom(roomId) { room ->
            currentChatRoom = room
            room?.let {
                tvHeader.text = if (it.status == "SOLVED") "Quest Solved! 🎉" else it.questTitle

                // Если квест активен — показываем поле ввода всем
                chatInput.visibility = if (it.status == "ACTIVE") View.VISIBLE else View.GONE

                // Кнопки управления квестом — только для создателя
                val isCreator = it.creatorId == myUid
                layoutCreator.visibility = if (isCreator && it.status == "ACTIVE") View.VISIBLE else View.GONE
            }
        }

        msgListener = chatHelper.observeMessages(roomId) { changes ->
            changes.forEach { dc ->
                if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    messages.add(dc.document.toObject(ChatMessage::class.java))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                }
            }
            if (messages.isNotEmpty()) rvChat.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            chatHelper.sendMessage(currentRoomId!!, text)
            etMessage.text.clear()
        }
    }

    private fun approveQuest() {
        val room = currentChatRoom ?: return
        chatHelper.approveQuest(currentRoomId!!, room) {
            chatHelper.sendSystemMessage(currentRoomId!!, "Quest APPROVED! Rewards issued!")
        }
    }

    private fun askAI() {
        currentChatRoom?.questId?.let { qId ->
            Toast.makeText(context, "AI is thinking...", Toast.LENGTH_SHORT).show()
            aiHelper.askAIForHint(qId, { hint ->
                chatHelper.sendSystemMessage(currentRoomId!!, "AI Guide: $hint")
            }, { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun setupRecyclerView(v: View) {
        chatAdapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        rvChat.adapter = chatAdapter
    }

    override fun onPause() {
        super.onPause()
        roomListener?.remove()
        msgListener?.remove()
    }

    private fun showFriendsDialog() {
        val input = EditText(context)
        AlertDialog.Builder(context)
            .setTitle("Add Friend to Quest")
            .setMessage("Enter friend's username:")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty()) {
                    chatHelper.inviteFriend(currentRoomId!!, username) { friendUid ->
                        if (friendUid != null) {
                            Toast.makeText(context, "$username added to the team!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}