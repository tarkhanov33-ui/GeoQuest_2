package com.example.geoquest.map

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.geoquest.R
import com.example.geoquest.chat.ChatFragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Circle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val mapHelper = FirestoreMapHelper()
    private var markerManager: MapMarkerManager? = null

    private lateinit var bsViewQuest: BottomSheetBehavior<LinearLayout>
    private lateinit var bsCreateQuest: BottomSheetBehavior<LinearLayout>
    private lateinit var tvQuestTitle: TextView
    private lateinit var tvQuestDescription: TextView
    private lateinit var tvQuestSnippet: TextView
    private lateinit var tvQuestRating: TextView
    private lateinit var btnQuestAction: Button
    private lateinit var ivQuestViewPhoto: ImageView
    private lateinit var ivMapTarget: ImageView

    private lateinit var etCreateTitle: EditText
    private lateinit var etCreateDescription: EditText
    private lateinit var etCreateHint: EditText
    private lateinit var etCreateReward: EditText
    private lateinit var etCreateDuration: EditText
    private lateinit var etCreateRadius: EditText
    private lateinit var tvQuestPhotoStatus: TextView

    private val questMap = mutableMapOf<Circle, QuestLocation>()
    private val fetchedQuests = mutableListOf<QuestLocation>()
    private val chatStatusMap = mutableMapOf<String, String>()
    private var uploadedPhotoUrl: String? = null
    private var editingQuestId: String? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            tvQuestPhotoStatus.text = "Uploading..."
            mapHelper.uploadPhoto(it, { url ->
                uploadedPhotoUrl = url
                tvQuestPhotoStatus.text = "Photo Uploaded ✓"
                tvQuestPhotoStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }, {
                tvQuestPhotoStatus.text = "Error"
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        initViews(view)
        val mapFrag = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
        return view
    }

    private fun initViews(v: View) {
        bsViewQuest = BottomSheetBehavior.from(v.findViewById(R.id.bottom_sheet_view_quest))
        bsCreateQuest = BottomSheetBehavior.from(v.findViewById(R.id.bottom_sheet_create_quest))
        tvQuestTitle = v.findViewById(R.id.tvQuestTitle)
        tvQuestDescription = v.findViewById(R.id.tvQuestDescription)
        tvQuestSnippet = v.findViewById(R.id.tvQuestSnippet)
        tvQuestRating = v.findViewById(R.id.tvQuestRating)
        btnQuestAction = v.findViewById(R.id.btnQuestAction)
        ivQuestViewPhoto = v.findViewById(R.id.ivQuestViewPhoto)
        ivMapTarget = v.findViewById(R.id.ivMapTarget)

        etCreateTitle = v.findViewById(R.id.etCreateTitle)
        etCreateDescription = v.findViewById(R.id.etCreateDescription)
        etCreateHint = v.findViewById(R.id.etCreateHint)
        etCreateReward = v.findViewById(R.id.etCreateReward)
        etCreateDuration = v.findViewById(R.id.etCreateDuration)
        etCreateRadius = v.findViewById(R.id.etCreateRadius)
        tvQuestPhotoStatus = v.findViewById(R.id.tvQuestPhotoStatus)

        v.findViewById<View>(R.id.fabAddQuest).setOnClickListener {
            resetCreateUI()
            bsCreateQuest.state = BottomSheetBehavior.STATE_COLLAPSED
            ivMapTarget.visibility = View.VISIBLE
            bsViewQuest.state = BottomSheetBehavior.STATE_HIDDEN
        }

        v.findViewById<Button>(R.id.btnSubmitNewQuest).setOnClickListener { submitQuest() }
        v.findViewById<View>(R.id.btnAttachQuestPhoto).setOnClickListener { pickPhoto.launch("image/*") }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        markerManager = MapMarkerManager(mMap, FirebaseAuth.getInstance().currentUser?.uid)
        mMap.setOnCircleClickListener { circle -> questMap[circle]?.let { showDetails(it) } }

        mapHelper.observeQuests { quests ->
            fetchedQuests.clear()
            fetchedQuests.addAll(quests)
            updateUI()
        }
        mapHelper.observeChatStatuses { statuses ->
            chatStatusMap.clear()
            chatStatusMap.putAll(statuses)
            updateUI()
        }
    }

    private fun updateUI() {
        if (!isAdded || markerManager == null) return
        val newCircles = markerManager!!.drawQuestsOnMap(fetchedQuests, chatStatusMap)
        questMap.clear()
        questMap.putAll(newCircles)
    }

    private fun showDetails(quest: QuestLocation) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        tvQuestTitle.text = quest.title
        tvQuestDescription.text = quest.description

        if (!quest.imageUrl.isNullOrEmpty()) {
            ivQuestViewPhoto.visibility = View.VISIBLE
            Glide.with(this).load(quest.imageUrl).into(ivQuestViewPhoto)
        } else { ivQuestViewPhoto.visibility = View.GONE }

        if (quest.creatorId == uid) {
            tvQuestSnippet.text = "Your Quest"
            tvQuestSnippet.setTextColor(android.graphics.Color.BLUE)
            btnQuestAction.text = "Edit Quest"
            btnQuestAction.setOnClickListener { startEditing(quest) }
        } else {
            val status = chatStatusMap[quest.id]
            tvQuestSnippet.text = "Difficulty: ${quest.difficulty}"
            tvQuestSnippet.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            when (status) {
                "SOLVED" -> {
                    btnQuestAction.text = "Rate"
                    btnQuestAction.setOnClickListener { showRatingDialog(quest) }
                }
                "ACTIVE" -> {
                    btnQuestAction.text = "Continue"
                    btnQuestAction.setOnClickListener { openChat(quest) }
                }
                else -> {
                    btnQuestAction.text = "Accept"
                    btnQuestAction.setOnClickListener { openChat(quest) }
                }
            }
        }

        tvQuestRating.text = "Rating: ★ ${String.format(java.util.Locale.US, "%.1f", quest.averageRating)}"
        bsViewQuest.state = BottomSheetBehavior.STATE_EXPANDED
        bsCreateQuest.state = BottomSheetBehavior.STATE_HIDDEN
        ivMapTarget.visibility = View.GONE
    }

    private fun openChat(quest: QuestLocation) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        mapHelper.prepareChatRoom(quest, uid) { roomId ->
            if (roomId != null) {
                ChatFragment.currentRoomId = roomId
                parentFragmentManager.beginTransaction().replace(R.id.fragment_container, ChatFragment()).addToBackStack(null).commit()
            }
        }
    }

    private fun submitQuest() {
        val target = mMap.cameraPosition.target
        val quest = QuestLocation(
            id = editingQuestId ?: "",
            title = etCreateTitle.text.toString(),
            description = etCreateDescription.text.toString(),
            hint = etCreateHint.text.toString(),
            reward = etCreateReward.text.toString(),
            duration = etCreateDuration.text.toString(),
            radius = etCreateRadius.text.toString().toDoubleOrNull() ?: 200.0,
            coordinate = GeoPoint(target.latitude, target.longitude),
            imageUrl = uploadedPhotoUrl,
            creatorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        )
        mapHelper.saveQuest(quest, editingQuestId != null) {
            bsCreateQuest.state = BottomSheetBehavior.STATE_HIDDEN
            ivMapTarget.visibility = View.GONE
        }
    }

    private fun startEditing(quest: QuestLocation) {
        editingQuestId = quest.id
        uploadedPhotoUrl = quest.imageUrl
        etCreateTitle.setText(quest.title)
        etCreateDescription.setText(quest.description)
        etCreateHint.setText(quest.hint)
        etCreateReward.setText(quest.reward)
        etCreateDuration.setText(quest.duration)
        etCreateRadius.setText(quest.radius.toString())
        bsViewQuest.state = BottomSheetBehavior.STATE_HIDDEN
        bsCreateQuest.state = BottomSheetBehavior.STATE_COLLAPSED
        ivMapTarget.visibility = View.VISIBLE
    }

    private fun showRatingDialog(quest: QuestLocation) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_rate_quest, null)
        val rb = view.findViewById<RatingBar>(R.id.rbQuestRating)
        val et = view.findViewById<EditText>(R.id.etReviewComment)

        AlertDialog.Builder(requireContext())
            .setTitle("Rate Quest")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                mapHelper.submitRating(quest.id, rb.rating, et.text.toString()) {
                    Toast.makeText(context, "Rated!", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun resetCreateUI() {
        etCreateTitle.text.clear()
        etCreateDescription.text.clear()
        etCreateHint.text.clear()
        etCreateReward.text.clear()
        etCreateDuration.text.clear()
        etCreateRadius.text.clear()
        uploadedPhotoUrl = null
        editingQuestId = null
        tvQuestPhotoStatus.text = "Attach Photo"
    }
}