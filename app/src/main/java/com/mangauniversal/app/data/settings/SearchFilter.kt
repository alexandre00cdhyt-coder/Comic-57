package com.mangauniversal.app.data.settings

import com.mangauniversal.app.data.model.Comic
import com.mangauniversal.app.data.model.ComicType

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DATE_ASC,
    DATE_DESC,
}

data class SearchFilters(
    val query: String = "",
    val typeFilter: ComicType? = null,
    val minSizeMB: Int = 0,
    val maxSizeMB: Int = 0,
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
)

fun List<Comic>.applyFilters(filters: SearchFilters): List<Comic> {
    var result = this

    if (filters.query.isNotBlank()) {
        result = result.filter { it.title.contains(filters.query, ignoreCase = true) }
    }

    if (filters.typeFilter != null) {
        result = result.filter { it.type == filters.typeFilter }
    }

    if (filters.minSizeMB > 0) {
        result = result.filter { it.sizeBytes >= filters.minSizeMB * 1024L * 1024L }
    }

    if (filters.maxSizeMB > 0) {
        result = result.filter { it.sizeBytes <= filters.maxSizeMB * 1024L * 1024L }
    }

    result = when (filters.sortOrder) {
        SortOrder.NAME_ASC -> result.sortedBy { it.title.lowercase() }
        SortOrder.NAME_DESC -> result.sortedByDescending { it.title.lowercase() }
        SortOrder.SIZE_ASC -> result.sortedBy { it.sizeBytes }
        SortOrder.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
        SortOrder.DATE_ASC -> result.sortedBy { it.lastModified }
        SortOrder.DATE_DESC -> result.sortedByDescending { it.lastModified }
    }

    return result
}
