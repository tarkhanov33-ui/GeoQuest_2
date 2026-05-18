package com.example.geoquest.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.geoquest.R
import com.example.geoquest.core.MainActivity
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private val profileHelper = FirestoreProfileHelper()

    private var userDataListener: ListenerRegistration? = null
    private var globalLeaderboardListener: ListenerRegistration? = null

    private lateinit var tlProfileTabs: TabLayout
    private lateinit var layoutProfileInfo: LinearLayout
    private lateinit var layoutFriendsLeaderboard: LinearLayout
    private lateinit var layoutGlobalLeaderboard: LinearLayout
    private lateinit var etUsername: EditText
    private lateinit var etCity: EditText
    private lateinit var btnSave: Button
    private lateinit var tvProfileScore: TextView
    private lateinit var tvProfileRank: TextView
    private lateinit var tvProfileLevel: TextView
    private lateinit var tvProfileXP: TextView
    private lateinit var pbProfileXP: ProgressBar
    private lateinit var etAddFriend: EditText
    private lateinit var btnAddFriend: Button
    private lateinit var rvFriendsList: RecyclerView
    private lateinit var rvGlobalLeaderboard: RecyclerView
    private lateinit var ivProfileAvatar: ImageView

    private lateinit var friendsAdapter: LeaderboardAdapter
    private lateinit var globalAdapter: LeaderboardAdapter

    private var currentUsername = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            profileHelper.uploadProfileImage(uri) { success ->
                if (success) {
                    Toast.makeText(context, "Profile image updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)
        setupTabs()
        setupRecyclerViews()
        setupListeners(view)

        startObservingData()

        return view
    }

    private fun startObservingData() {
        userDataListener = profileHelper.observeUserData { data ->
            if (data == null) return@observeUserData

            currentUsername = data.username

            if (etUsername.text.isEmpty()) etUsername.setText(data.username)
            if (etCity.text.isEmpty()) etCity.setText(data.city)
            val fullList = mutableListOf<LeaderboardUser>()
            fullList.add(LeaderboardUser(0, data.username, data.score, data.questsCompleted, data.profileImageUrl, true))
            profileHelper.fetchFriendsData(data.friends) { friendsData ->
                fullList.addAll(friendsData)

                fullList.sortByDescending { it.score }
                val rankedList = fullList.mapIndexed { index, user ->
                    user.copy(rank = index + 1)
                }

                friendsAdapter.updateData(rankedList)
            }
            tvProfileScore.text = data.score.toString()
            tvProfileLevel.text = "Level ${data.level}"
            tvProfileXP.text = "${data.xp} / 500 XP"
            pbProfileXP.progress = data.xp.toInt()
            tvProfileRank.text = getRankTitle(data.questsCompleted)

            if (!data.profileImageUrl.isNullOrEmpty()) {
                ivProfileAvatar.imageTintList = null
                ivProfileAvatar.setPadding(0, 0, 0, 0)
                Glide.with(requireContext())
                    .load(data.profileImageUrl)
                    .transform(CircleCrop())
                    .into(ivProfileAvatar)
            }

            val myRankEntry = LeaderboardUser(1, data.username, data.score, data.questsCompleted, data.profileImageUrl, true)
            friendsAdapter.updateData(listOf(myRankEntry))
        }

        globalLeaderboardListener = profileHelper.observeGlobalLeaderboard { list ->
            val markedList = list.map { item ->
                item.copy(isCurrentUser = (item.username == currentUsername && currentUsername.isNotEmpty()))
            }
            globalAdapter.updateData(markedList)
        }
    }

    private fun setupListeners(view: View) {
        btnSave.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val city = etCity.text.toString().trim()
            if (username.isNotEmpty() && city.isNotEmpty()) {
                profileHelper.updateProfile(username, city) { success ->
                    if (success) Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddFriend.setOnClickListener {
            val friendName = etAddFriend.text.toString().trim()
            if (friendName == currentUsername) {
                Toast.makeText(context, "You cannot add yourself!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (friendName.isNotEmpty()) {
                profileHelper.addFriend(friendName) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) etAddFriend.text.clear()
                }
            }
        }

        view.findViewById<View>(R.id.profile_image_container).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menu.add("Logout")
            popup.setOnMenuItemClickListener { item ->
                if (item.title == "Logout") {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                true
            }
            popup.show()
        }
    }

    private fun initViews(view: View) {
        tlProfileTabs = view.findViewById(R.id.tlProfileTabs)
        layoutProfileInfo = view.findViewById(R.id.layoutProfileInfo)
        layoutFriendsLeaderboard = view.findViewById(R.id.layoutFriendsLeaderboard)
        layoutGlobalLeaderboard = view.findViewById(R.id.layoutGlobalLeaderboard)
        etUsername = view.findViewById(R.id.etProfileUsername)
        etCity = view.findViewById(R.id.etProfileCity)
        btnSave = view.findViewById(R.id.btnProfileSave)
        tvProfileScore = view.findViewById(R.id.tvProfileScore)
        tvProfileRank = view.findViewById(R.id.tvProfileRank)
        tvProfileLevel = view.findViewById(R.id.tvProfileLevel)
        tvProfileXP = view.findViewById(R.id.tvProfileXP)
        pbProfileXP = view.findViewById(R.id.pbProfileXP)
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar)
        etAddFriend = view.findViewById(R.id.etAddFriend)
        btnAddFriend = view.findViewById(R.id.btnAddFriend)
        rvFriendsList = view.findViewById(R.id.rvFriendsList)
        rvGlobalLeaderboard = view.findViewById(R.id.rvGlobalLeaderboard)
    }

    private fun setupTabs() {
        tlProfileTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                layoutProfileInfo.visibility = View.GONE
                layoutFriendsLeaderboard.visibility = View.GONE
                layoutGlobalLeaderboard.visibility = View.GONE

                when (tab?.position) {
                    0 -> layoutProfileInfo.visibility = View.VISIBLE
                    1 -> layoutFriendsLeaderboard.visibility = View.VISIBLE
                    2 -> layoutGlobalLeaderboard.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerViews() {
        friendsAdapter = LeaderboardAdapter(mutableListOf())
        rvFriendsList.layoutManager = LinearLayoutManager(context)
        rvFriendsList.adapter = friendsAdapter

        globalAdapter = LeaderboardAdapter(mutableListOf())
        rvGlobalLeaderboard.layoutManager = LinearLayoutManager(context)
        rvGlobalLeaderboard.adapter = globalAdapter
    }

    private fun getRankTitle(questsCompleted: Long): String {
        return when {
            questsCompleted >= 50 -> "Legend"
            questsCompleted >= 20 -> "Master"
            questsCompleted >= 10 -> "Explorer"
            questsCompleted >= 1 -> "Adventurer"
            else -> "Novice"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userDataListener?.remove()
        globalLeaderboardListener?.remove()
    }
}