package com.example.geoquest.chat

import com.example.geoquest.R
import com.example.geoquest.chat.ChatAdapter
import com.example.geoquest.chat.ChatMessage


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.tvMessageSent)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivMessageSentImage)
        
        fun bind(message: ChatMessage) {
            if (!message.imageUrl.isNullOrEmpty()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context).load(message.imageUrl).into(ivImage)
                if (message.text.isNotEmpty()) {
                    textMessage.visibility = View.VISIBLE
                    textMessage.text = message.text
                } else {
                    textMessage.visibility = View.GONE
                }
            } else {
                ivImage.visibility = View.GONE
                textMessage.visibility = View.VISIBLE
                textMessage.text = message.text
            }
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.tvMessageReceived)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivMessageReceivedImage)
        
        fun bind(message: ChatMessage) {
            if (!message.imageUrl.isNullOrEmpty()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context).load(message.imageUrl).into(ivImage)
                if (message.text.isNotEmpty()) {
                    textMessage.visibility = View.VISIBLE
                    textMessage.text = message.text
                } else {
                    textMessage.visibility = View.GONE
                }
            } else {
                ivImage.visibility = View.GONE
                textMessage.visibility = View.VISIBLE
                textMessage.text = message.text
            }
        }
    }
}


