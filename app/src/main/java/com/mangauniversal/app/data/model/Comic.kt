package com.mangauniversal.app.data.model

/**
 * Representa un cómic, manga, libro o carpeta de imágenes
 * que puede ser leído por la app.
 */
data class Comic(
    val id: String,
    val title: String,
    val path: String,
    val type: ComicType,
    val coverPath: String? = null,
    val pageCount: Int = 0,
    val lastModified: Long = 0L,
    val sizeBytes: Long = 0L,
    val folder: String = "",
    val isFavorite: Boolean = false,
    val lastPageRead: Int = 0,
    val lastReadAt: Long = 0L,
)

enum class ComicType {
    CBZ, CBR, ZIP, RAR, SEVEN_ZIP, PDF, FOLDER, UNKNOWN
}

val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "gif", "webp",
    "bmp", "avif", "heic", "heif", "tiff", "tif"
)

val COMIC_EXTENSIONS = setOf(
    "cbz", "cbr", "zip", "rar", "7z", "pdf"
)

fun String.isImageFile(): Boolean {
    val ext = substringAfterLast('.', "").lowercase()
    return ext in IMAGE_EXTENSIONS
}

fun String.isComicFile(): Boolean {
    val ext = substringAfterLast('.', "").lowercase()
    return ext in COMIC_EXTENSIONS
}

fun detectComicType(fileName: String): ComicType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "cbz" -> ComicType.CBZ
        "cbr" -> ComicType.CBR
        "zip" -> ComicType.ZIP
        "rar" -> ComicType.RAR
        "7z" -> ComicType.SEVEN_ZIP
        "pdf" -> ComicType.PDF
        else -> ComicType.UNKNOWN
    }
}
