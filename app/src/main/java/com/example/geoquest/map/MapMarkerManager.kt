package com.example.geoquest.map

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

class MapMarkerManager(private val mMap: GoogleMap, private val currentUserId: String?) {

    fun drawQuestsOnMap(quests: List<QuestLocation>, statusMap: Map<String, String>): MutableMap<Circle, QuestLocation> {
        mMap.clear()
        val questMap = mutableMapOf<Circle, QuestLocation>()

        for (quest in quests) {
            val coords = quest.coordinate ?: continue
            val latLng = LatLng(coords.latitude, coords.longitude)

            val hue = when {
                quest.creatorId == currentUserId -> BitmapDescriptorFactory.HUE_AZURE
                statusMap[quest.id] == "SOLVED" || quest.status == "SOLVED" -> BitmapDescriptorFactory.HUE_GREEN
                statusMap[quest.id] == "ACTIVE" -> BitmapDescriptorFactory.HUE_YELLOW
                else -> BitmapDescriptorFactory.HUE_RED
            }

            val circle = mMap.addCircle(CircleOptions()
                .center(latLng)
                .radius(quest.radius)
                .strokeColor(Color.HSVToColor(255, floatArrayOf(hue, 1f, 1f)))
                .fillColor(Color.HSVToColor(64, floatArrayOf(hue, 1f, 1f)))
                .strokeWidth(5f)
                .clickable(true))

            questMap[circle] = quest
        }
        return questMap
    }
}