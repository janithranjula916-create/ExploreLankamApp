package com.example.explorelanka.data

import androidx.compose.runtime.mutableStateListOf

/** Temporary catalogue. In Step 3, Room will become the source of truth. */
data class Destination(
    val id: String,
    val name: String,
    val district: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val photoCreator: String,
    val photoLicense: String,
    val photoSourceUrl: String
)

private fun commonsPhoto(filename: String): String =
    "https://commons.wikimedia.org/wiki/Special:FilePath/${filename.replace(' ', '_')}?width=1000"

private fun commonsSource(filename: String): String =
    "https://commons.wikimedia.org/wiki/File:${filename.replace(' ', '_')}"

object DestinationData {
    val places = mutableStateListOf(
        Destination(
            id = "sigiriya",
            name = "Sigiriya Rock Fortress",
            district = "Matale",
            category = "History",
            description = "Discover an ancient rock fortress known for its gardens, historic frescoes, and magnificent panoramic views.",
            imageUrl = commonsPhoto("Sigiriya Rock Fortress.jpg"),
            photoCreator = "Satheesha J.",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("Sigiriya Rock Fortress.jpg")
        ),
        Destination(
            id = "nine_arch_bridge",
            name = "Nine Arch Bridge",
            district = "Badulla",
            category = "Nature",
            description = "Visit the iconic stone railway bridge near Ella, surrounded by Sri Lanka's lush hill-country landscape.",
            imageUrl = commonsPhoto("Nine arches bridge.jpg"),
            photoCreator = "AntanO",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("Nine arches bridge.jpg")
        ),
        Destination(
            id = "galle_fort",
            name = "Galle Fort",
            district = "Galle",
            category = "History",
            description = "Explore historic streets, preserved fortifications, and ocean views in the heart of coastal Galle.",
            imageUrl = commonsPhoto("GALLE FORT.jpg"),
            photoCreator = "Vimukthi SL",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("GALLE FORT.jpg")
        ),
        Destination(
            id = "mirissa_beach",
            name = "Mirissa Beach",
            district = "Matara",
            category = "Beach",
            description = "Relax on a scenic stretch of Sri Lanka's southern coastline, known for its palms and sunsets.",
            imageUrl = commonsPhoto("Mirissa Beach.jpg"),
            photoCreator = "Dan arndt",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("Mirissa Beach.jpg")
        ),
        Destination(
            id = "peradeniya_gardens",
            name = "Royal Botanic Gardens",
            district = "Kandy",
            category = "Nature",
            description = "Walk through botanical collections, avenues of trees, and landscaped tropical gardens in Peradeniya.",
            imageUrl = commonsPhoto("Royal Botanic Gardens, Peradeniya.jpg"),
            photoCreator = "Dudva",
            photoLicense = "CC0 1.0",
            photoSourceUrl = commonsSource("Royal Botanic Gardens, Peradeniya.jpg")
        ),
        Destination(
            id = "yala_national_park",
            name = "Yala National Park",
            district = "Hambantota",
            category = "Wildlife",
            description = "Discover diverse natural habitats and wildlife in one of Sri Lanka's most celebrated national parks.",
            imageUrl = commonsPhoto("Yala National Park, Sri Lanka.jpg"),
            photoCreator = "Charles J. Sharp",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("Yala National Park, Sri Lanka.jpg")
        ),
        Destination(
            id = "temple_of_tooth",
            name = "Temple of the Sacred Tooth Relic",
            district = "Kandy",
            category = "History",
            description = "Learn about the heritage and architecture of an important cultural landmark in Kandy.",
            imageUrl = commonsPhoto("Temple of tooth sri lanka.jpg"),
            photoCreator = "Yasith bagya",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("Temple of tooth sri lanka.jpg")
        ),
        Destination(
            id = "unawatuna_beach",
            name = "Unawatuna Beach",
            district = "Galle",
            category = "Beach",
            description = "Visit a popular beach on the south coast, with sweeping sand and turquoise water.",
            imageUrl = commonsPhoto("Unawatuna beach.jpg"),
            photoCreator = "Randeepa Dhanarathna",
            photoLicense = "CC BY-SA 4.0",
            photoSourceUrl = commonsSource("Unawatuna beach.jpg")
        )
    )

    /** Merge server changes into the observable catalogue; existing seed places remain offline. */

    fun mergeRemote(remote: List<Destination>) {
        if (remote.isEmpty()) return

        val merged = (remote + places.toList())
            .distinctBy { it.id.trim().lowercase() }
            .distinctBy {
                it.name.trim()
                    .lowercase()
                    .replace(Regex("\\s+"), " ")
            }
            .sortedBy { it.name }

        places.clear()
        places.addAll(merged)
    }


    fun findById(id: String): Destination? = places.firstOrNull { it.id == id }
}
