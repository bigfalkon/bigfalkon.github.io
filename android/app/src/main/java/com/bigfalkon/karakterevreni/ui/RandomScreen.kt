package com.bigfalkon.karakterevreni.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bigfalkon.karakterevreni.data.Character
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SpinItem(
    val character: Character,
    val star: Int,
    val imageUrl: String?,
    val imagePosition: String?
)

/**
 * Sitedeki rastgele seçicinin native karşılığı: yıldız filtreli havuzdan
 * 2 rastgele karakter, slot makinesi hissiyatlı bir dönüşle çekilir.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(
    state: UiState,
    onClose: () -> Unit,
    onOpenCharacter: (String) -> Unit
) {
    var include1 by rememberSaveable { mutableStateOf(true) }
    var include2 by rememberSaveable { mutableStateOf(true) }
    var include3 by rememberSaveable { mutableStateOf(true) }
    var includeFusion by rememberSaveable { mutableStateOf(true) }

    val pool = remember(state.characters, include1, include2, include3, includeFusion) {
        buildList {
            state.characters.forEach { c ->
                if (c.isFusion) {
                    if (includeFusion) add(SpinItem(c, 4, c.previewUrl ?: c.imageUrl, c.previewPosition))
                } else {
                    if (include1) add(SpinItem(c, 1, c.imageUrl, c.imagePosition))
                    if (include2) c.evolution(2)?.let { add(SpinItem(c, 2, it.imageUrl, it.imagePosition)) }
                    if (include3) c.evolution(3)?.let { add(SpinItem(c, 3, it.imageUrl, it.imagePosition)) }
                }
            }
        }
    }

    var left by remember { mutableStateOf<SpinItem?>(null) }
    var right by remember { mutableStateOf<SpinItem?>(null) }
    var spinning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun draw() {
        if (pool.isEmpty() || spinning) return
        scope.launch {
            spinning = true
            var interval = 55L
            val deadline = System.currentTimeMillis() + 1500
            while (System.currentTimeMillis() < deadline) {
                left = pool.random()
                right = pool.random()
                delay(interval)
                interval = (interval * 1.18f).toLong().coerceAtMost(220L)
            }
            // Sonuç: mümkünse iki farklı karakter
            val first = pool.random()
            val second = pool.filter { it.character.id != first.character.id }
                .randomOrNull() ?: first
            left = first
            right = second
            spinning = false
        }
    }

    LaunchedEffect(Unit) { draw() }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = { Text("Rastgele Karakter", fontFamily = Fantastical) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Kapat")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(include1, { include1 = !include1 }, label = { Text("1★") })
                FilterChip(include2, { include2 = !include2 }, label = { Text("2★") })
                FilterChip(include3, { include3 = !include3 }, label = { Text("3★") })
                FilterChip(includeFusion, { includeFusion = !includeFusion }, label = { Text("Füzyon") })
            }

            Text(
                "Havuz: ${pool.size} kart",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SpinCard(left, spinning) { left?.let { onOpenCharacter(it.character.id) } }
                SpinCard(right, spinning) { right?.let { onOpenCharacter(it.character.id) } }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = { draw() },
                enabled = pool.isNotEmpty() && !spinning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                Text("  Karakter Çek", fontWeight = FontWeight.Bold)
            }

            Text(
                "Sonuca dokunarak karakter detayını açabilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }

    BackHandler { onClose() }
}

@Composable
private fun RowScope.SpinCard(item: SpinItem?, spinning: Boolean, onClick: () -> Unit) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.5.dp,
                    if (spinning) FusionColor.copy(alpha = 0.6f) else OutlineVar,
                    RoundedCornerShape(20.dp)
                )
                .background(SurfaceDark)
                .clickable(enabled = !spinning && item != null, onClick = onClick)
        ) {
            if (spinning) {
                PrismGlow(FusionColor, Modifier.matchParentSize())
            }
            if (item != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.character.name,
                    contentScale = ContentScale.Crop,
                    alignment = parseImageAlignment(item.imagePosition),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("?", fontFamily = Fantastical, fontSize = 44.sp, color = OnSurfaceVar)
                }
            }
        }
        Text(
            item?.character?.name ?: "—",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (item != null) {
            StarRow(item.star, isDismissed = false, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
