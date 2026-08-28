package com.bigfalkon.karakterevreni.data

/** Bir karakterin belirli yıldız seviyesindeki görseli. */
data class Evolution(
    val star: Int,
    val imageUrl: String?,
    val imagePosition: String?
)

/** Bir karakterin belirli bir alternatif evrendeki (AU) karşılığı. */
data class AuEntry(
    val star3ImageUrl: String?,
    val star3ImagePosition: String?,
    val fusionImageUrl: String?,
    val fusionImagePosition: String?,
    val prompt: String? = null
) {
    fun imageFor(isFusion: Boolean): String? = if (isFusion) fusionImageUrl else star3ImageUrl
    fun positionFor(isFusion: Boolean): String? =
        if (isFusion) fusionImagePosition else star3ImagePosition
}

data class Character(
    val id: String,
    val name: String,
    val irk: String?,
    val star: Int,
    val imageUrl: String?,
    val imagePosition: String?,
    val previewUrl: String?,
    val previewPosition: String?,
    val isFusion: Boolean,
    val isDismissed: Boolean,
    val type: String?,
    val fusionPartners: List<String>,
    val evolutions: List<Evolution>,
    val auData: Map<String, AuEntry>,
    val addedAt: Long
) {
    fun evolution(star: Int): Evolution? = evolutions.firstOrNull { it.star == star }

    /** Sitedeki mantık: 3★ görseli yoksa 1★ görseline düş. */
    fun star3ImageUrl(): String? = evolution(3)?.imageUrl ?: imageUrl

    fun hasAuEntry(auId: String): Boolean = auData[auId]?.imageFor(isFusion) != null
}

data class AlternativeUniverse(
    val id: String,
    val name: String,
    val color: String?,
    val icon: String?,
    val locked: Boolean,
    val description: String?
)

/** Galeride gösterilen tek bir kart: karakter + hangi yıldız seviyesinin gösterildiği. */
data class GalleryItem(
    val character: Character,
    val displayStar: Int,
    val imageUrl: String?,
    val imagePosition: String?,
    val auId: String? = null
) {
    val key: String get() = "${character.id}#$displayStar#${auId ?: "-"}"
}
