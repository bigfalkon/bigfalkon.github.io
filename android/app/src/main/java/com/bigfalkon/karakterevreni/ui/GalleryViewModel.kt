package com.bigfalkon.karakterevreni.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bigfalkon.karakterevreni.data.AlternativeUniverse
import com.bigfalkon.karakterevreni.data.Character
import com.bigfalkon.karakterevreni.data.Firestore
import com.bigfalkon.karakterevreni.data.GalleryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

enum class GalleryMode(val label: String) {
    All("Tümü"),
    Star1("1 Yıldız"),
    Star2("2 Yıldız"),
    Star3("3 Yıldız"),
    Fusion("Füzyonlar"),
    Dismissed("Emekliler")
}

enum class SortMode(val label: String) {
    Name("İsim (A-Z)"),
    Newest("En yeni"),
    Oldest("En eski")
}

data class UiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val characters: List<Character> = emptyList(),
    val dismissed: List<Character> = emptyList(),
    val universes: List<AlternativeUniverse> = emptyList(),
    val mode: GalleryMode = GalleryMode.All,
    val sort: SortMode = SortMode.Name,
    val race: String? = null,
    val activeAuId: String? = null,
    val query: String = ""
) {
    val activeAu: AlternativeUniverse? get() = universes.firstOrNull { it.id == activeAuId }
    val races: List<String>
        get() = (characters + dismissed).mapNotNull { it.irk }.distinct()
            .sortedWith(Collator.getInstance(Locale("tr")))
    /** Kilitli evrenler yalnızca kilidi açıldığında listelenir (sitedeki davranış). */
    val visibleUniverses: List<AlternativeUniverse> get() = universes.filterNot { it.locked }
    val filterCount: Int
        get() = listOfNotNull(
            race,
            mode.takeIf { it != GalleryMode.All },
            sort.takeIf { it != SortMode.Name }
        ).size
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Firestore.readCache(getApplication())?.let { cached ->
                if (!cached.isEmpty) {
                    _state.update {
                        it.copy(
                            loading = false,
                            characters = cached.characters,
                            dismissed = cached.dismissed,
                            universes = cached.universes
                        )
                    }
                }
            }
            refresh(initial = true)
        }
    }

    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = !initial || !it.loading, error = null) }
            runCatching { Firestore.load(getApplication()) }
                .onSuccess { snap ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = null,
                            characters = snap.characters,
                            dismissed = snap.dismissed,
                            universes = snap.universes
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = if (it.characters.isEmpty()) {
                                "Karakterler yüklenemedi. Bağlantını kontrol et."
                            } else null
                        )
                    }
                }
        }
    }

    fun setMode(mode: GalleryMode) = _state.update { it.copy(mode = mode) }
    fun setSort(sort: SortMode) = _state.update { it.copy(sort = sort) }
    fun setRace(race: String?) = _state.update { it.copy(race = race) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun toggleAu(id: String?) =
        _state.update { it.copy(activeAuId = if (it.activeAuId == id) null else id) }

    fun clearFilters() =
        _state.update { it.copy(mode = GalleryMode.All, sort = SortMode.Name, race = null) }

    fun character(id: String): Character? =
        _state.value.characters.firstOrNull { it.id == id }
            ?: _state.value.dismissed.firstOrNull { it.id == id }
}

// ─── Galeri listesinin türetilmesi (sitedeki _doRender mantığının karşılığı) ───

private fun comparatorFor(sort: SortMode): Comparator<Character> = when (sort) {
    SortMode.Newest -> compareByDescending { it.addedAt }
    SortMode.Oldest -> compareBy { it.addedAt }
    SortMode.Name -> compareBy(Collator.getInstance(Locale.ENGLISH)) { it.name }
}

fun buildGalleryItems(state: UiState): List<GalleryItem> {
    val sorted = comparatorFor(state.sort)
    val source = if (state.mode == GalleryMode.Dismissed) {
        state.dismissed.filter { it.type != "easterEgg" }
    } else {
        state.characters
    }

    var items = mutableListOf<GalleryItem>()

    fun addAllStars(list: List<Character>) {
        list.filterNot { it.isFusion }.sortedWith(sorted).forEach { c ->
            items.add(GalleryItem(c, 1, c.imageUrl, c.imagePosition))
            c.evolutions.forEach { evo ->
                items.add(GalleryItem(c, evo.star, evo.imageUrl, evo.imagePosition))
            }
        }
        list.filter { it.isFusion }.sortedWith(sorted).forEach { c ->
            items.add(GalleryItem(c, 4, c.previewUrl ?: c.imageUrl, c.previewPosition))
        }
    }

    when (state.mode) {
        GalleryMode.All, GalleryMode.Dismissed -> addAllStars(source)
        GalleryMode.Fusion -> source.filter { it.isFusion }.sortedWith(sorted).forEach { c ->
            items.add(GalleryItem(c, 4, c.previewUrl ?: c.imageUrl, c.previewPosition))
        }
        GalleryMode.Star1 -> source.filterNot { it.isFusion }.sortedWith(sorted).forEach { c ->
            items.add(GalleryItem(c, 1, c.imageUrl, c.imagePosition))
        }
        GalleryMode.Star2, GalleryMode.Star3 -> {
            val star = if (state.mode == GalleryMode.Star2) 2 else 3
            source.filterNot { it.isFusion }.sortedWith(sorted).forEach { c ->
                c.evolution(star)?.let { evo ->
                    items.add(GalleryItem(c, star, evo.imageUrl, evo.imagePosition))
                }
            }
        }
    }

    state.race?.let { race -> items = items.filter { it.character.irk == race }.toMutableList() }

    // AU modu: yalnızca o evrende karşılığı olanlar, o evrenin görselleriyle.
    val auId = state.activeAuId
    if (auId != null) {
        val seenFusions = mutableSetOf<String>()
        val auItems = mutableListOf<GalleryItem>()
        items.filter { it.character.hasAuEntry(auId) }.forEach { item ->
            val entry = item.character.auData[auId] ?: return@forEach
            if (item.character.isFusion) {
                if (seenFusions.add(item.character.id)) {
                    auItems.add(
                        item.copy(
                            imageUrl = entry.fusionImageUrl ?: item.imageUrl,
                            imagePosition = entry.fusionImagePosition,
                            auId = auId
                        )
                    )
                }
            } else if (item.displayStar == 3) {
                auItems.add(
                    item.copy(
                        imageUrl = entry.star3ImageUrl ?: item.imageUrl,
                        imagePosition = entry.star3ImagePosition,
                        auId = auId
                    )
                )
            }
        }
        items = auItems
    }

    val query = state.query.trim()
    if (query.isNotEmpty()) {
        items = items.filter {
            it.character.name.contains(query, ignoreCase = true) ||
                it.character.id.contains(query, ignoreCase = true) ||
                it.character.irk?.contains(query, ignoreCase = true) == true
        }.toMutableList()
    }

    return items
}
