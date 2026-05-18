package com.example.geoquest.profile
import com.example.geoquest.R
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
class LeaderboardAdapter(private var users: List<LeaderboardUser>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    fun updateData(newUsers: List<LeaderboardUser>) {
        this.users = newUsers
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tvLeaderboardRank)
        val tvName: TextView = itemView.findViewById(R.id.tvLeaderboardName)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvLeaderboardSubtitle)
        val tvScore: TextView = itemView.findViewById(R.id.tvLeaderboardScore)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivLeaderboardAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        
        holder.tvRank.text = "#${user.rank}"
        holder.tvName.text = user.username
        if (user.isCurrentUser) {
            holder.tvName.text = "${user.username} (You)"
            holder.tvName.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.tvName.setTextColor(Color.parseColor("#E0E0E0")) 
        }
        
        holder.tvSubtitle.text = "Rank: ${user.rankTitle} | Quests: ${user.questsCompleted}"
        holder.tvScore.text = "${user.score} pts"
        
        when (user.rank) {
            1 -> holder.tvRank.setTextColor(Color.parseColor("#FFD700")) 
            2 -> holder.tvRank.setTextColor(Color.parseColor("#C0C0C0")) 
            3 -> holder.tvRank.setTextColor(Color.parseColor("#CD7F32")) 
            else -> holder.tvRank.setTextColor(Color.parseColor("#757575")) 
        }
        
        if (user.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
            holder.ivAvatar.imageTintList = null
            holder.ivAvatar.setPadding(0, 0, 0, 0)
            Glide.with(holder.itemView.context)
                .load(user.profileImageUrl)
                .transform(CircleCrop())
                .into(holder.ivAvatar)
        } else {
            
            holder.ivAvatar.setPadding(8, 8, 8, 8)
            holder.ivAvatar.setColorFilter(Color.parseColor("#757575")) 
            holder.ivAvatar.setImageResource(R.drawable.user)
        }
    }

    override fun getItemCount(): Int = users.size
}


