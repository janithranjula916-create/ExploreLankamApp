package com.example.explorelanka.data.remote

import android.util.Log
import com.example.explorelanka.data.Destination
import com.example.explorelanka.data.DestinationData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/** Public curated destinations; local seed catalogue is retained as offline fallback. */
object DestinationCloudListener {
    fun start(): ListenerRegistration =
        FirebaseFirestore.getInstance().collection("places")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ExploreLankaPlaces", "Remote destinations unavailable", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val url = doc.getString("imageUrl")?.takeIf { it.startsWith("https://") }.orEmpty()
                    Destination(
                        id = doc.id, name = name,
                        district = doc.getString("district").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        imageUrl = url,
                        photoCreator = doc.getString("photoCreator") ?: "Unknown",
                        photoLicense = doc.getString("photoLicense") ?: "Check licence",
                        photoSourceUrl = doc.getString("photoSourceUrl")?.takeIf { it.startsWith("https://") } ?: "https://commons.wikimedia.org"
                    )
                }.orEmpty()
                DestinationData.mergeRemote(items)
            }
}
