package com.bigfalkon.karakterevreni.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigfalkon.karakterevreni.data.GalleryItem

/** Kart boyutu adımları (sitedeki XS…XL zoom seviyelerinin karşılığı). */
val CardSizes = listOf(118.dp, 138.dp, 162.dp, 196.dp, 240.dp)
val CardSizeLabels = listOf("XS", "S", "M", "L", "XL")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: UiState,
    items: List<GalleryItem>,
    onOpenFilters: () -> Unit,
    onClearAu: () -> Unit,
    onRefresh: () -> Unit,
    onTitleTap: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    contentPadding: PaddingValues
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val gridState = rememberLazyGridState()
    val accent = state.activeAu?.let { parseHexColor(it.color) } ?: Primary
    // Görünüm değişince kartlar sitedeki gibi yeniden sırayla süzülsün.
    val born = remember(state.mode, state.sort, state.race, state.activeAuId) {
        System.currentTimeMillis()
    }
    val ptrState = rememberPullToRefreshState()

    Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        TopAppBar(
            title = {
                Column(
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTitleTap
                    )
                ) {
                    Text(
                        state.activeAu?.name ?: "Karakter Evreni",
                        fontFamily = Fantastical,
                        fontSize = 22.sp,
                        color = accent
                    )
                    Text(
                        if (state.activeAu != null) {
                            "${items.size} kart · alternatif evren"
                        } else {
                            "${items.size} kart"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                BadgedBox(
                    badge = {
                        if (state.filterCount > 0) {
                            Badge(containerColor = accent) { Text(state.filterCount.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = onOpenFilters) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtreler")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = BackgroundDark.copy(alpha = 0.9f)
            ),
            scrollBehavior = scrollBehavior
        )

        AnimatedVisibility(visible = state.activeAu != null) {
            val au = state.activeAu
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${au?.name} evrenindesin",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                    IconButton(onClick = onClearAu) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Evrenden çık",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            state = ptrState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = ptrState,
                    isRefreshing = state.refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = SurfaceHigh,
                    color = accent
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.loading && items.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = accent) }

                state.error != null && items.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Tekrar dene")
                    }
                }

                items.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "Bu filtreye uyan karakter yok.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = CardSizes[state.cardSize.coerceIn(0, 4)]),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                        CharacterCard(
                            item = item,
                            universes = state.visibleUniverses,
                            activeAu = state.activeAu,
                            index = index,
                            born = born,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}
