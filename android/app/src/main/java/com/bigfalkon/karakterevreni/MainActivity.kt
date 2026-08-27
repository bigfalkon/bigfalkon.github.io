package com.bigfalkon.karakterevreni

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bigfalkon.karakterevreni.ui.BackgroundDark
import com.bigfalkon.karakterevreni.ui.DetailScreen
import com.bigfalkon.karakterevreni.ui.FilterSheet
import com.bigfalkon.karakterevreni.ui.GalleryScreen
import com.bigfalkon.karakterevreni.ui.GalleryViewModel
import com.bigfalkon.karakterevreni.ui.ImageViewer
import com.bigfalkon.karakterevreni.ui.KarakterEvreniTheme
import com.bigfalkon.karakterevreni.ui.SearchScreen
import com.bigfalkon.karakterevreni.ui.ToolsScreen
import com.bigfalkon.karakterevreni.ui.UniversesScreen
import com.bigfalkon.karakterevreni.ui.buildGalleryItems

private enum class Tab(val label: String, val icon: ImageVector) {
    Gallery("Galeri", Icons.Filled.GridView),
    Search("Ara", Icons.Filled.Search),
    Universes("Evrenler", Icons.Filled.AutoFixHigh),
    Tools("Araçlar", Icons.Filled.Build)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { KarakterEvreniTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot() {
    val vm: GalleryViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Gallery) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val items = remember(state) { buildGalleryItems(state) }
    val searchItems = remember(state) {
        if (state.query.isBlank()) emptyList()
        else buildGalleryItems(state.copy(mode = com.bigfalkon.karakterevreni.ui.GalleryMode.All))
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (detailId == null) {
                NavigationBar(containerColor = BackgroundDark) {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                            label = { Text(entry.label) },
                            colors = NavigationBarItemDefaults.colors()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            AnimatedContent(
                targetState = detailId,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)))
                            .togetherWith(fadeOut(tween(160)))
                    } else {
                        fadeIn(tween(180))
                            .togetherWith(slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
                    }
                },
                label = "detail"
            ) { openId ->
                if (openId != null) {
                    val character = vm.character(openId)
                    if (character == null) {
                        detailId = null
                    } else {
                        DetailScreen(
                            character = character,
                            universes = state.visibleUniverses,
                            allCharacters = state.characters + state.dismissed,
                            onBack = { detailId = null },
                            onOpenCharacter = { detailId = it },
                            onOpenImage = { viewerUrl = it },
                            contentPadding = padding
                        )
                    }
                } else {
                    when (tab) {
                        Tab.Gallery -> GalleryScreen(
                            state = state,
                            items = items,
                            onOpenFilters = { showFilters = true },
                            onClearAu = { vm.toggleAu(state.activeAuId) },
                            onRefresh = { vm.refresh() },
                            onItemClick = { detailId = it.character.id },
                            contentPadding = padding
                        )

                        Tab.Search -> SearchScreen(
                            state = state,
                            items = searchItems,
                            onQueryChange = vm::setQuery,
                            onItemClick = { detailId = it.character.id },
                            contentPadding = padding
                        )

                        Tab.Universes -> UniversesScreen(
                            state = state,
                            onSelect = { id ->
                                vm.toggleAu(id)
                                tab = Tab.Gallery
                            },
                            contentPadding = padding
                        )

                        Tab.Tools -> ToolsScreen(contentPadding = padding)
                    }
                }
            }

            viewerUrl?.let { url ->
                ImageViewer(url = url, onDismiss = { viewerUrl = null })
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = state,
            onMode = vm::setMode,
            onSort = vm::setSort,
            onRace = vm::setRace,
            onClear = vm::clearFilters,
            onDismiss = { showFilters = false }
        )
    }

    BackHandler(enabled = viewerUrl != null || detailId != null || tab != Tab.Gallery) {
        when {
            viewerUrl != null -> viewerUrl = null
            detailId != null -> detailId = null
            else -> tab = Tab.Gallery
        }
    }
}
