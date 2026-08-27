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
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.bigfalkon.karakterevreni.ui.AuroraBackground
import com.bigfalkon.karakterevreni.ui.BackgroundDark
import com.bigfalkon.karakterevreni.ui.DetailScreen
import com.bigfalkon.karakterevreni.ui.FilterSheet
import com.bigfalkon.karakterevreni.ui.GalleryScreen
import com.bigfalkon.karakterevreni.ui.GalleryViewModel
import com.bigfalkon.karakterevreni.ui.ImageViewer
import com.bigfalkon.karakterevreni.ui.KarakterEvreniTheme
import com.bigfalkon.karakterevreni.ui.SearchScreen
import com.bigfalkon.karakterevreni.ui.SignInDialog
import com.bigfalkon.karakterevreni.ui.parseHexColor
import com.bigfalkon.karakterevreni.ui.ToolsScreen
import com.bigfalkon.karakterevreni.ui.UniversesScreen
import com.bigfalkon.karakterevreni.ui.WebToolScreen
import com.bigfalkon.karakterevreni.ui.buildGalleryItems

private enum class Tab(val label: String, val icon: ImageVector) {
    Gallery("Galeri", Icons.Filled.GridView),
    Search("Ara", Icons.Filled.Search),
    Universes("Evrenler", Icons.Filled.AutoFixHigh),
    Tools("Panel", Icons.Filled.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val vm: GalleryViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val accent = state.activeAu?.let { parseHexColor(it.color) }

    KarakterEvreniTheme(accent = accent) {
        AppContent(vm = vm, state = state, accent = accent)
    }
}

@Composable
private fun AppContent(
    vm: GalleryViewModel,
    state: com.bigfalkon.karakterevreni.ui.UiState,
    accent: androidx.compose.ui.graphics.Color?
) {

    var tab by rememberSaveable { mutableStateOf(Tab.Gallery) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSignIn by rememberSaveable { mutableStateOf(false) }
    var toolUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var toolTitle by rememberSaveable { mutableStateOf("") }
    var titleTaps by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.unlockMessage) {
        state.unlockMessage?.let {
            snackbarHost.showSnackbar(it)
            vm.consumeUnlockMessage()
        }
    }

    // Galeri, Ara sekmesindeki sorgudan etkilenmemeli.
    val items = remember(state) { buildGalleryItems(state.copy(query = "")) }
    val searchItems = remember(state) {
        if (state.query.isBlank()) emptyList()
        else buildGalleryItems(state.copy(mode = com.bigfalkon.karakterevreni.ui.GalleryMode.All))
    }

    Box(Modifier.fillMaxSize()) {
    AuroraBackground(accent = accent)
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            if (detailId == null && toolUrl == null) {
                NavigationBar(containerColor = BackgroundDark.copy(alpha = 0.92f)) {
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
                            onTitleTap = {
                                val now = System.currentTimeMillis()
                                titleTaps = if (now - lastTapAt > 1500L) 1 else titleTaps + 1
                                lastTapAt = now
                                if (titleTaps >= 5) {
                                    titleTaps = 0
                                    if (vm.secretUnlockTapped()) showSignIn = true
                                }
                            },
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
                            onSignOut = vm::signOut,
                            contentPadding = padding
                        )

                        Tab.Tools -> ToolsScreen(
                            onOpenTool = { url, title ->
                                toolUrl = url
                                toolTitle = title
                            },
                            contentPadding = padding
                        )
                    }
                }
            }

            toolUrl?.let { url ->
                WebToolScreen(
                    url = url,
                    title = toolTitle,
                    onClose = { toolUrl = null }
                )
            }

            viewerUrl?.let { url ->
                ImageViewer(url = url, onDismiss = { viewerUrl = null })
            }
        }
    }

    }

    if (showFilters) {
        FilterSheet(
            state = state,
            onMode = vm::setMode,
            onSort = vm::setSort,
            onRace = vm::setRace,
            onCardSize = vm::setCardSize,
            onClear = vm::clearFilters,
            onDismiss = { showFilters = false }
        )
    }

    if (showSignIn) {
        SignInDialog(
            signingIn = state.signingIn,
            error = state.signInError,
            onSubmit = vm::signIn,
            onDismiss = {
                showSignIn = false
                vm.clearSignInError()
            }
        )
    }

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) showSignIn = false
    }

    BackHandler(
        enabled = toolUrl == null &&
            (viewerUrl != null || detailId != null || tab != Tab.Gallery)
    ) {
        when {
            viewerUrl != null -> viewerUrl = null
            detailId != null -> detailId = null
            else -> tab = Tab.Gallery
        }
    }
}
