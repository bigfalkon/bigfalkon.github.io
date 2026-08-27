package com.bigfalkon.karakterevreni.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bigfalkon.karakterevreni.data.GalleryItem

@Composable
fun SearchScreen(
    state: UiState,
    items: List<GalleryItem>,
    onQueryChange: (String) -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    contentPadding: PaddingValues
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("İsim, ID veya ırk ara") },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
        )

        if (state.query.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aramak için yazmaya başla",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = CardSizes[state.cardSize.coerceIn(0, 4)]),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp
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
                        born = 0L,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun UniversesScreen(
    state: UiState,
    onSelect: (String?) -> Unit,
    onSignOut: () -> Unit,
    contentPadding: PaddingValues
) {
    val universes = state.visibleUniverses
    LazyColumn(
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            Text(
                "Alternatif Evrenler",
                fontFamily = Fantastical,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
            )
        }
        item {
            Text(
                "Bir evren seçtiğinde galeri yalnızca o evrendeki kartları gösterir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )
        }
        item {
            UniverseRow(
                name = "Normal evren",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                count = state.characters.size,
                selected = state.activeAuId == null,
                onClick = { onSelect(null) }
            )
        }
        items(universes, key = { it.id }) { au ->
            val count = state.characters.count { it.hasAuEntry(au.id) } +
                state.dismissed.count { it.hasAuEntry(au.id) }
            UniverseRow(
                name = au.name,
                color = parseHexColor(au.color),
                count = count,
                selected = state.activeAuId == au.id,
                secret = au.locked,
                onClick = { onSelect(au.id) }
            )
        }

        if (state.signedIn) {
            item {
                ListItem(
                    headlineContent = { Text("Çıkış yap") },
                    supportingContent = { Text("Gizli evrenler tekrar kilitlenir") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onSignOut)
                )
            }
        } else if (state.hasLockedUniverses) {
            item {
                Text(
                    "Galeri başlığına arka arkaya 5 kez dokunarak gizli evrenlere erişebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun UniverseRow(
    name: String,
    color: Color,
    count: Int,
    selected: Boolean,
    secret: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Bold)
                if (secret) {
                    Icon(
                        Icons.Filled.LockOpen,
                        contentDescription = "Gizli evren",
                        tint = color,
                        modifier = Modifier.padding(start = 6.dp).size(14.dp)
                    )
                }
            }
        },
        supportingContent = { Text("$count kart") },
        leadingContent = {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            }
        },
        trailingContent = {
            if (selected) Icon(Icons.Filled.Check, contentDescription = "Seçili", tint = color)
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) SurfaceHigh else Color.Transparent
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ─── Panel ────────────────────────────────────────────────────────────────────

private data class WebTool(
    val title: String,
    val subtitle: String,
    val url: String,
    val icon: ImageVector
)

private val panelTools = listOf(
    WebTool(
        "Admin Paneli", "Karakter ekle, düzenle, evren yönet",
        "https://bigfalkon.github.io/index2.html", Icons.Filled.AdminPanelSettings
    ),
    WebTool(
        "Rastgele Karakter", "Rastgele karakter seçici",
        "https://bigfalkon.github.io/rastgelekarakter.html", Icons.Filled.Casino
    )
)

private val otherTools = listOf(
    WebTool(
        "Turnuva", "Karakter turnuvası",
        "https://bigfalkon.github.io/oyun.html", Icons.Filled.EmojiEvents
    ),
    WebTool(
        "AU Viewer", "Evren görüntüleyici",
        "https://bigfalkon.github.io/au-viewer.html", Icons.Filled.AutoFixHigh
    ),
    WebTool(
        "Yedekleme", "Yedek al / geri yükle",
        "https://bigfalkon.github.io/character-backup-tool.html", Icons.Filled.Backup
    ),
    WebTool(
        "Linkler", "Bağlantılar sayfası",
        "https://bigfalkon.github.io/links.html", Icons.Filled.Link
    )
)

/** Panel sekmesi: admin paneli ve diğer web araçları, uygulamanın içinde açılır. */
@Composable
fun ToolsScreen(onOpenTool: (String, String) -> Unit, contentPadding: PaddingValues) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            Text(
                "Panel",
                fontFamily = Fantastical,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
            )
        }
        item {
            Text(
                "Araçlar uygulamanın içinde açılır.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            )
        }
        item { SectionLabel("Yönetim") }
        items(panelTools) { tool -> ToolRow(tool, onOpenTool) }
        item { SectionLabel("Diğer") }
        items(otherTools) { tool -> ToolRow(tool, onOpenTool) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun ToolRow(tool: WebTool, onOpenTool: (String, String) -> Unit) {
    ListItem(
        headlineContent = { Text(tool.title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(tool.subtitle) },
        leadingContent = {
            Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onOpenTool(tool.url, tool.title) }
    )
}
