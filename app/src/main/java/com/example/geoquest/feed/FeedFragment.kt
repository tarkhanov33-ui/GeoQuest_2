package com.example.geoquest.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geoquest.R
import com.example.geoquest.chat.ChatFragment
import com.example.geoquest.map.QuestLocation
import com.google.android.material.bottomnavigation.BottomNavigationView

class FeedFragment : Fragment() {

    private lateinit var rvQuestFeed: RecyclerView
    private lateinit var adapter: QuestFeedAdapter
    private val questList = mutableListOf<QuestLocation>()

    private val repository = FirestoreFeedHelper()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        setupRecyclerView(view)

        repository.startListeningQuests(
            onQuestsUpdated = { newQuests ->
                questList.clear()
                questList.addAll(newQuests)
                adapter.notifyDataSetChanged()
            },
            onError = { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )

        return view
    }

    private fun setupRecyclerView(view: View) {
        rvQuestFeed = view.findViewById(R.id.rvQuestFeed)
        rvQuestFeed.layoutManager = LinearLayoutManager(context)

        adapter = QuestFeedAdapter(questList) { quest ->
            handleAcceptQuest(quest)
        }
        rvQuestFeed.adapter = adapter
    }

    private fun handleAcceptQuest(quest: QuestLocation) {
        repository.acceptQuest(quest) { success, roomId, error ->
            if (success && roomId != null) {
                openChatRoom(roomId)
            } else {
                Toast.makeText(context, "Failed: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openChatRoom(roomId: String) {
        ChatFragment.currentRoomId = roomId

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ChatFragment())
            .addToBackStack(null)
            .commit()

        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_chat
    }

    override fun onDestroyView() {
        super.onDestroyView()
        repository.stopListening()
    }
}