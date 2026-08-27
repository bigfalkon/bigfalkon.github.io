package com.bigfalkon.karakterevreni.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bigfalkon.karakterevreni.data.AlternativeUniverse
import com.bigfalkon.karakterevreni.data.Character

private data class Variant(
    val label: String,
    val imageUrl: String?,
    val imagePosition: String?,
    val accent: Color
)

/** Karakter/füzyon detay ekranı: tüm yıldız seviyeleri, AU karşılıkları ve füzyonlar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    character: Character,
    universes: List<AlternativeUniverse>,
    allCharacters: List<Character>,
    onBack: () -> Unit,
    onOpenCharacter: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val variants = remember(character, universes) {
        buildList {
            if (character.isFusion) {
                add(
                    Variant(
                        "Füzyon",
                        character.previewUrl ?: character.imageUrl,
                        character.previewPosition,
                        FusionColor
                    )
                )
            } else {
                add(Variant("1★", character.imageUrl, character.imagePosition, StarColors[1]!!))
                character.evolutions.forEach { evo ->
                    add(
                        Variant(
                            "${evo.star}★",
                            evo.imageUrl,
                            evo.imagePosition,
                            StarColors[evo.star.coerceIn(1, 4)] ?: Primary
                        )
                    )
                }
            }
            universes.forEach { au ->
                val entry = character.auData[au.id] ?: return@forEach
                val url = entry.imageFor(character.isFusion) ?: return@forEach
                add(
                    Variant(
                        au.name,
                        url,
                        entry.positionFor(character.isFusion),
                        parseHexColor(au.color)
                    )
                )
            }
        }.filter { it.imageUrl != null }
    }

    var selected by remember(character.id) { mutableStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { variants.size.coerceAtLeast(1) })

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        selected = pagerState.currentPage
    }
    androidx.compose.runtime.LaunchedEffect(selected) {
        if (pagerState.currentPage != selected) pagerState.animateScrollToPage(selected)
    }

    val fusions = remember(character, allCharacters) {
        if (character.isFusion) emptyList()
        else allCharacters.filter { it.isFusion && it.fusionPartners.contains(character.id) }
    }
    val partners = remember(character, allCharacters) {
        if (!character.isFusion) emptyList()
        else character.fusionPartners.mapNotNull { id -> allCharacters.firstOrNull { it.id == id } }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    character.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = if (character.isFusion || character.isDismissed) Fantastical else Manrope,
                    fontWeight = if (character.isFusion || character.isDismissed) FontWeight.Normal else FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp)
        ) {
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.86f)
                ) { page ->
                    val variant = variants.getOrNull(page)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { variant?.imageUrl?.let(onOpenImage) }
                    ) {
                        if (character.isFusion) {
                            PrismGlow(
                                color = FusionColor,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        BlurredBackdropImage(
                            url = variant?.imageUrl,
                            position = variant?.imagePosition,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (variants.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(variants.size) { index ->
                            val variant = variants[index]
                            FilterChip(
                                selected = selected == index,
                                onClick = { selected = index },
                                label = { Text(variant.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = variant.accent.copy(alpha = 0.22f),
                                    selectedLabelColor = variant.accent
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StarRow(
                        star = if (character.isFusion) 4 else (character.evolutions.maxOfOrNull { it.star } ?: 1),
                        isDismissed = character.isDismissed,
                        size = 18
                    )
                    character.irk?.let { irk ->
                        AssistChip(
                            onClick = {},
                            label = { Text(irk) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Diversity3,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(labelColor = Primary)
                        )
                    }
                }
            }

            if (partners.isNotEmpty()) {
                item { SectionTitle("Füzyon bileşenleri") }
                item { CharacterStrip(partners, onOpenCharacter) }
            }

            if (fusions.isNotEmpty()) {
                item { SectionTitle("Bu karakterin füzyonları") }
                item { CharacterStrip(fusions, onOpenCharacter) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun CharacterStrip(list: List<Character>, onOpen: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list, key = { it.id }) { c ->
            Column(
                Modifier.width(112.dp).clickable { onOpen(c.id) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    modifier = Modifier.size(112.dp)
                ) {
                    AsyncImage(
                        model = if (c.isFusion) (c.previewUrl ?: c.imageUrl) else c.star3ImageUrl(),
                        contentDescription = c.name,
                        contentScale = ContentScale.Crop,
                        alignment = parseImageAlignment(
                            if (c.isFusion) c.previewPosition else c.imagePosition
                        ),
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                }
                Text(
                    c.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** Tam ekran görsel önizleme. */
@Composable
fun ImageViewer(url: String, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Kapat",
                tint = Color.White
            )
        }
    }
}
