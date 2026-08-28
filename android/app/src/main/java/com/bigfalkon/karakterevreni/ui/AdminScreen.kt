package com.bigfalkon.karakterevreni.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bigfalkon.karakterevreni.data.AlternativeUniverse
import com.bigfalkon.karakterevreni.data.AuEntry
import com.bigfalkon.karakterevreni.data.Character
import com.bigfalkon.karakterevreni.data.Evolution

private fun emptyCharacter() = Character(
    id = "", name = "", irk = null, star = 1, imageUrl = null, imagePosition = null,
    previewUrl = null, previewPosition = null, isFusion = false, isDismissed = false,
    type = null, fusionPartners = emptyList(), evolutions = emptyList(),
    auData = emptyMap(), addedAt = 0L
)

private fun emptyUniverse() = AlternativeUniverse(
    id = "", name = "", color = "#64dcb4", icon = "auto_fix_high",
    locked = false, description = ""
)

/** Native admin paneli: karakter ve evren CRUD'u, doğrudan Firestore'a yazar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(vm: GalleryViewModel, state: UiState, onClose: () -> Unit) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Character?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var editingAu by remember { mutableStateOf<AlternativeUniverse?>(null) }
    var auIsNew by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.adminMessage) {
        state.adminMessage?.let {
            snackbar.showSnackbar(it)
            vm.consumeAdminMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        when {
                            editing != null && isNew -> "Yeni kayıt"
                            editing != null -> editing?.name?.ifBlank { "Kayıt" } ?: ""
                            else -> "Admin Paneli"
                        },
                        fontFamily = if (editing == null) Fantastical else Manrope,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (editing != null) editing = null else onClose() }) {
                        Icon(
                            if (editing != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                            contentDescription = "Kapat"
                        )
                    }
                },
                actions = {
                    if (state.adminBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 14.dp).size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )

            val current = editing
            if (current == null) {
                TabRow(selectedTabIndex = tabIndex, containerColor = SurfaceDark) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Karakterler") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Evrenler") })
                }
                if (tabIndex == 0) {
                    CharacterAdminList(
                        state = state,
                        query = query,
                        onQuery = { query = it },
                        onEdit = { editing = it; isNew = false },
                        onNew = { editing = emptyCharacter(); isNew = true }
                    )
                } else {
                    UniverseAdminList(
                        state = state,
                        onEdit = { editingAu = it; auIsNew = false },
                        onNew = { editingAu = emptyUniverse(); auIsNew = true }
                    )
                }
            } else {
                CharacterEditor(vm, state, current, isNew, onDone = { editing = null })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    editingAu?.let { au ->
        UniverseEditorDialog(vm, au, auIsNew, onDismiss = { editingAu = null })
    }

    BackHandler {
        when {
            editingAu != null -> editingAu = null
            editing != null -> editing = null
            else -> onClose()
        }
    }
}

// ─── Listeler ─────────────────────────────────────────────────────────────────

@Composable
private fun CharacterAdminList(
    state: UiState,
    query: String,
    onQuery: (String) -> Unit,
    onEdit: (Character) -> Unit,
    onNew: () -> Unit
) {
    val q = query.trim()
    fun List<Character>.filtered() = filter {
        q.isEmpty() || it.name.contains(q, true) || it.id.contains(q, true)
    }.sortedBy { it.name.lowercase() }

    val actives = state.characters.filtered()
    val retired = state.dismissed.filtered()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text("Ara…") },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            "Yeni kayıt ekle",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onNew)
                )
            }
            item { AdminSectionHeader("Aktif (${actives.size})") }
            items(actives, key = { "c-" + it.id }) { c -> AdminCharacterRow(c) { onEdit(c) } }
            item { AdminSectionHeader("Emekliler (${retired.size})") }
            items(retired, key = { "d-" + it.id }) { c -> AdminCharacterRow(c) { onEdit(c) } }
        }
    }
}

@Composable
private fun AdminSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp)
    )
}

@Composable
private fun AdminCharacterRow(c: Character, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                c.name,
                fontFamily = if (c.isFusion) Fantastical else Manrope,
                fontWeight = if (c.isFusion) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val kind = if (c.isFusion) "Füzyon" else "${1 + c.evolutions.size}★'a kadar"
            Text(listOfNotNull(c.irk, kind).joinToString(" · "))
        },
        leadingContent = {
            AsyncImage(
                model = if (c.isFusion) (c.previewUrl ?: c.imageUrl) else c.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceHigh)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun UniverseAdminList(
    state: UiState,
    onEdit: (AlternativeUniverse) -> Unit,
    onNew: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = {
                    Text(
                        "Yeni evren ekle",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingContent = {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onNew)
            )
        }
        items(state.universes, key = { it.id }) { au ->
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(au.name, fontWeight = FontWeight.Bold)
                        if (au.locked) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Gizli",
                                tint = parseHexColor(au.color),
                                modifier = Modifier.padding(start = 6.dp).size(14.dp)
                            )
                        }
                    }
                },
                supportingContent = { Text(au.id) },
                leadingContent = {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(parseHexColor(au.color))
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onEdit(au) }
            )
        }
    }
}

// ─── Karakter editörü ─────────────────────────────────────────────────────────

private class AuFieldHolder(url: String, pos: String, prompt: String) {
    val url: MutableState<String> = mutableStateOf(url)
    val pos: MutableState<String> = mutableStateOf(pos)
    val prompt: MutableState<String> = mutableStateOf(prompt)
}

@Composable
private fun CharacterEditor(
    vm: GalleryViewModel,
    state: UiState,
    c: Character,
    isNew: Boolean,
    onDone: () -> Unit
) {
    var id by remember(c.id, isNew) { mutableStateOf(c.id) }
    var name by remember(c.id, isNew) { mutableStateOf(c.name) }
    var irk by remember(c.id, isNew) { mutableStateOf(c.irk.orEmpty()) }
    var fusion by remember(c.id, isNew) { mutableStateOf(c.isFusion) }
    var imageUrl by remember(c.id, isNew) { mutableStateOf(c.imageUrl.orEmpty()) }
    var imagePos by remember(c.id, isNew) { mutableStateOf(c.imagePosition.orEmpty()) }
    var previewUrl by remember(c.id, isNew) { mutableStateOf(c.previewUrl.orEmpty()) }
    var previewPos by remember(c.id, isNew) { mutableStateOf(c.previewPosition.orEmpty()) }
    var partner1 by remember(c.id, isNew) { mutableStateOf(c.fusionPartners.getOrNull(0).orEmpty()) }
    var partner2 by remember(c.id, isNew) { mutableStateOf(c.fusionPartners.getOrNull(1).orEmpty()) }
    var evo2Url by remember(c.id, isNew) { mutableStateOf(c.evolution(2)?.imageUrl.orEmpty()) }
    var evo2Pos by remember(c.id, isNew) { mutableStateOf(c.evolution(2)?.imagePosition.orEmpty()) }
    var evo3Url by remember(c.id, isNew) { mutableStateOf(c.evolution(3)?.imageUrl.orEmpty()) }
    var evo3Pos by remember(c.id, isNew) { mutableStateOf(c.evolution(3)?.imagePosition.orEmpty()) }
    val auFields = remember(c.id, isNew) {
        state.universes.associate { au ->
            val e = c.auData[au.id]
            val url = if (c.isFusion) e?.fusionImageUrl else e?.star3ImageUrl
            val pos = if (c.isFusion) e?.fusionImagePosition else e?.star3ImagePosition
            au.id to AuFieldHolder(url.orEmpty(), pos.orEmpty(), e?.prompt.orEmpty())
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }

    fun buildCharacter(): Character {
        val evolutions = if (fusion) emptyList() else buildList {
            if (evo2Url.isNotBlank()) add(Evolution(2, evo2Url.trim(), evo2Pos.trim().ifBlank { null }))
            if (evo3Url.isNotBlank()) add(Evolution(3, evo3Url.trim(), evo3Pos.trim().ifBlank { null }))
        }
        val au = buildMap {
            state.universes.forEach { u ->
                val h = auFields.getValue(u.id)
                val url = h.url.value.trim()
                val pos = h.pos.value.trim()
                val prompt = h.prompt.value.trim()
                val orig = c.auData[u.id]
                if (url.isNotEmpty() || prompt.isNotEmpty()) {
                    put(
                        u.id,
                        AuEntry(
                            star3ImageUrl = if (!fusion) url.ifEmpty { null } else orig?.star3ImageUrl,
                            star3ImagePosition = if (!fusion) pos.ifEmpty { null } else orig?.star3ImagePosition,
                            fusionImageUrl = if (fusion) url.ifEmpty { null } else orig?.fusionImageUrl,
                            fusionImagePosition = if (fusion) pos.ifEmpty { null } else orig?.fusionImagePosition,
                            prompt = prompt.ifEmpty { null }
                        )
                    )
                }
            }
        }
        return c.copy(
            id = id.trim(),
            name = name.trim(),
            irk = irk.trim().ifBlank { null },
            star = if (fusion) 4 else 1,
            isFusion = fusion,
            imageUrl = imageUrl.trim().ifBlank { null },
            imagePosition = imagePos.trim().ifBlank { null },
            previewUrl = previewUrl.trim().ifBlank { null },
            previewPosition = previewPos.trim().ifBlank { null },
            fusionPartners = if (fusion) {
                listOf(partner1, partner2).map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            },
            evolutions = evolutions,
            auData = au
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isNew) {
            Field("ID (benzersiz, örn. karakter adı)", id) { id = it }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Füzyon", modifier = Modifier.weight(1f))
                Switch(checked = fusion, onCheckedChange = { fusion = it })
            }
        }
        Field("İsim", name) { name = it }
        Field("Irk", irk) { irk = it }
        Field(if (fusion) "Tam görsel URL" else "1★ görsel URL", imageUrl) { imageUrl = it }
        Field("Görsel konumu (örn. center 25%)", imagePos) { imagePos = it }

        if (fusion) {
            EditorHeader("Füzyon")
            Field("Önizleme (kart) URL", previewUrl) { previewUrl = it }
            Field("Önizleme konumu", previewPos) { previewPos = it }
            Field("Bileşen 1 (karakter ID)", partner1) { partner1 = it }
            Field("Bileşen 2 (karakter ID)", partner2) { partner2 = it }
        } else {
            EditorHeader("Evrimler")
            Field("2★ görsel URL", evo2Url) { evo2Url = it }
            Field("2★ konum", evo2Pos) { evo2Pos = it }
            Field("3★ görsel URL", evo3Url) { evo3Url = it }
            Field("3★ konum", evo3Pos) { evo3Pos = it }
        }

        EditorHeader("Alternatif evrenler")
        state.universes.forEach { au ->
            val h = auFields.getValue(au.id)
            Text(
                au.name + if (au.locked) " (gizli)" else "",
                color = parseHexColor(au.color),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
            )
            Field("Görsel URL", h.url.value) { h.url.value = it }
            Field("Konum", h.pos.value) { h.pos.value = it }
            Field("Prompt", h.prompt.value, singleLine = false) { h.prompt.value = it }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.saveCharacter(buildCharacter(), isNew) { ok -> if (ok) onDone() } },
            enabled = !state.adminBusy && id.isNotBlank() && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kaydet", fontWeight = FontWeight.Bold)
        }

        if (!isNew) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!c.isDismissed) {
                    OutlinedButton(
                        onClick = { vm.retireCharacter(c) { ok -> if (ok) onDone() } },
                        enabled = !state.adminBusy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Emekliye ayır") }
                } else {
                    OutlinedButton(
                        onClick = { vm.restoreCharacter(c) { ok -> if (ok) onDone() } },
                        enabled = !state.adminBusy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Geri getir") }
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.adminBusy,
                    modifier = Modifier.weight(1f)
                ) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SurfaceDark,
            title = { Text("Silinsin mi?") },
            text = { Text("\"${c.name}\" kalıcı olarak silinecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCharacter(c) { ok ->
                        confirmDelete = false
                        if (ok) onDone()
                    }
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun EditorHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

// ─── Evren editörü ────────────────────────────────────────────────────────────

@Composable
private fun UniverseEditorDialog(
    vm: GalleryViewModel,
    au: AlternativeUniverse,
    isNew: Boolean,
    onDismiss: () -> Unit
) {
    var id by remember(au.id, isNew) { mutableStateOf(au.id) }
    var name by remember(au.id, isNew) { mutableStateOf(au.name) }
    var color by remember(au.id, isNew) { mutableStateOf(au.color.orEmpty()) }
    var icon by remember(au.id, isNew) { mutableStateOf(au.icon.orEmpty()) }
    var locked by remember(au.id, isNew) { mutableStateOf(au.locked) }
    var description by remember(au.id, isNew) { mutableStateOf(au.description.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(if (isNew) "Yeni evren" else au.name, fontFamily = Fantastical) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isNew) Field("ID (örn. steampunk)", id) { id = it }
                Field("İsim", name) { name = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(color))
                    )
                    Box(Modifier.weight(1f)) {
                        Field("Renk (#64dcb4)", color) { color = it }
                    }
                }
                Field("İkon (material symbol adı)", icon) { icon = it }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gizli (kilitli)", modifier = Modifier.weight(1f))
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }
                Field("Açıklama", description, singleLine = false) { description = it }
                if (!isNew) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Evreni sil", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.saveUniverse(
                        AlternativeUniverse(
                            id = id.trim(),
                            name = name.trim(),
                            color = color.trim().ifBlank { null },
                            icon = icon.trim().ifBlank { null },
                            locked = locked,
                            description = description.trim().ifBlank { null }
                        )
                    ) { ok -> if (ok) onDismiss() }
                },
                enabled = id.isNotBlank() && name.isNotBlank()
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SurfaceDark,
            title = { Text("Evren silinsin mi?") },
            text = { Text("\"${au.name}\" evreni silinecek. Karakterlerdeki AU verileri kalır ama görünmez olur.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteUniverse(au.id) { ok ->
                        confirmDelete = false
                        if (ok) onDismiss()
                    }
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") }
            }
        )
    }
}
