package com.example.geoquest.chat

import com.example.geoquest.R
import com.example.geoquest.chat.ChatListAdapter
import com.example.geoquest.chat.ChatRoom


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val chats: List<ChatRoom>,
    private val currentUserId: String,
    private val onChatClick: (ChatRoom) -> Unit,
    private val onDeleteClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvChatTitle)
        val tvStatus: TextView = itemView.findViewById(R.id.tvChatStatus)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteChat)

        fun bind(chat: ChatRoom) {
            
            if (chat.questTitle.isNotEmpty()) {
                tvTitle.text = chat.questTitle
            } else if (chat.questId.isNotEmpty()) {
                tvTitle.text = "Quest #${chat.questId.take(5)} Discussion"
            } else {
                tvTitle.text = "Direct Message"
            }

            if (chat.invitedIds.contains(currentUserId)) {
                tvStatus.text = "Status: INVITED (Pending)"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#FFA500"))
            } else {
                tvStatus.text = "Status: ${chat.status}"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#E0E0E0"))
            }

            itemView.setOnClickListener { onChatClick(chat) }

            if (chat.creatorId == currentUserId) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDeleteClick(chat) }
            } else {
                btnDelete.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size
}


