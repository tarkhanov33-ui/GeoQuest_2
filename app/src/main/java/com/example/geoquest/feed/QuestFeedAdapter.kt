package com.example.geoquest.feed

import com.example.geoquest.R
import com.example.geoquest.feed.QuestFeedAdapter
import com.example.geoquest.map.QuestLocation


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class QuestFeedAdapter(
    private val quests: List<QuestLocation>,
    private val onAcceptClick: (QuestLocation) -> Unit
) : RecyclerView.Adapter<QuestFeedAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivQuestFeedImage)
        val tvTitle: TextView = itemView.findViewById(R.id.tvQuestFeedTitle)
        val tvReward: TextView = itemView.findViewById(R.id.tvQuestFeedReward)
        val tvDesc: TextView = itemView.findViewById(R.id.tvQuestFeedDesc)
        val tvDifficulty: TextView = itemView.findViewById(R.id.tvQuestFeedDifficulty)
        val btnAccept: Button = itemView.findViewById(R.id.btnQuestFeedAccept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest_feed, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val quest = quests[position]

        holder.tvTitle.text = quest.title
        holder.tvDesc.text = quest.description
        holder.tvDifficulty.text = "Difficulty: ${quest.difficulty}"

        if (quest.reward != null && quest.reward.toString().toIntOrNull() ?: 0 > 0) {
            holder.tvReward.text = "+${quest.reward} pts"
            holder.tvReward.visibility = View.VISIBLE
        } else {
            holder.tvReward.visibility = View.GONE
        }

        if (quest.imageUrl != null && quest.imageUrl.isNotEmpty()) {
            holder.ivImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(quest.imageUrl)
                .transform(CenterCrop(), RoundedCorners(24))
                .into(holder.ivImage)
        } else {
            holder.ivImage.visibility = View.GONE
        }

        holder.btnAccept.setOnClickListener {
            onAcceptClick(quest)
        }
    }

    override fun getItemCount(): Int = quests.size
}


