package com.bigfalkon.karakterevreni.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bigfalkon.karakterevreni.data.GalleryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: UiState,
    items: List<GalleryItem>,
    onOpenFilters: () -> Unit,
    onClearAu: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    contentPadding: PaddingValues
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val gridState = rememberLazyGridState()

    Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        state.activeAu?.name ?: "Karakter Evreni",
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${items.size} kart",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Yenile")
                }
                BadgedBox(
                    badge = {
                        if (state.filterCount > 0) Badge { Text(state.filterCount.toString()) }
                    }
                ) {
                    IconButton(onClick = onOpenFilters) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtreler")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundDark,
                scrolledContainerColor = SurfaceDark
            ),
            scrollBehavior = scrollBehavior
        )

        AnimatedVisibility(visible = state.activeAu != null) {
            val au = state.activeAu
            val accent = parseHexColor(au?.color)
            Surface(
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${au?.name} evrenini görüntülüyorsun",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                    IconButton(onClick = onClearAu, modifier = Modifier.size(28.dp)) {
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
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.loading && items.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }

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
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 158.dp),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = contentPadding.calculateBottomPadding() + 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.key }) { item ->
                        CharacterCard(
                            item = item,
                            universes = remember(state.universes) { state.visibleUniverses },
                            activeAu = state.activeAu,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}
