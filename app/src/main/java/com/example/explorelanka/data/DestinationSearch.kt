package com.example.explorelanka.data

import java.util.Locale

/** Pure filtering logic so that Explore search can be unit-tested. */
object DestinationSearch {
    fun filter(
        destinations: List<Destination>,
        query: String,
        category: String
    ): List<Destination> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return destinations.filter { destination ->
            val matchesCategory = category == "All" ||
                destination.category.equals(category, ignoreCase = true)
            val matchesSearch = normalizedQuery.isEmpty() ||
                destination.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                destination.district.lowercase(Locale.ROOT).contains(normalizedQuery)
            matchesCategory && matchesSearch
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
