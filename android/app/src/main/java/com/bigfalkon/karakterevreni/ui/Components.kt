package com.bigfalkon.karakterevreni.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bigfalkon.karakterevreni.data.AlternativeUniverse
import com.bigfalkon.karakterevreni.data.GalleryItem

/**
 * CSS `object-position` değerini ("center 25%", "50% 30%") Compose hizalamasına çevirir;
 * web'deki kırpma noktaları böylece uygulamada da korunur.
 */
fun parseImageAlignment(position: String?): Alignment {
    if (position.isNullOrBlank()) return Alignment.Center
    val parts = position.trim().split(Regex("\\s+"))
    fun bias(token: String?, isVertical: Boolean): Float = when {
        token == null -> 0f
        token.endsWith("%") -> (token.removeSuffix("%").toFloatOrNull()?.div(50f)?.minus(1f)) ?: 0f
        token == "center" -> 0f
        token == "top" && isVertical -> -1f
        token == "bottom" && isVertical -> 1f
        token == "left" && !isVertical -> -1f
        token == "right" && !isVertical -> 1f
        else -> 0f
    }
    return BiasAlignment(
        horizontalBias = bias(parts.getOrNull(0), isVertical = false).coerceIn(-1f, 1f),
        verticalBias = bias(parts.getOrNull(1), isVertical = true).coerceIn(-1f, 1f)
    )
}

@Composable
fun StarRow(star: Int, isDismissed: Boolean, modifier: Modifier = Modifier, size: Int = 14) {
    if (isDismissed) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = "Emekli",
            tint = DismissedColor,
            modifier = modifier.size(size.dp + 2.dp)
        )
        return
    }
    val tint = StarColors[star.coerceIn(1, 4)] ?: Primary
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(star.coerceIn(1, 4)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
        }
    }
}

@Composable
fun CharacterCard(
    item: GalleryItem,
    universes: List<AlternativeUniverse>,
    activeAu: AlternativeUniverse?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "cardPress")

    val accent = when {
        activeAu != null -> parseHexColor(activeAu.color)
        item.character.isFusion -> FusionColor
        item.character.isDismissed -> DismissedColor
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (accent == Color.Transparent) Modifier
                else Modifier.border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.character.name,
                contentScale = ContentScale.Crop,
                alignment = parseImageAlignment(item.imagePosition),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
            )

            // Alt kısımdaki isim/yıldız için okunabilirlik gradyanı
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            // Sağ üstte, karakterin bulunduğu evrenleri gösteren noktalar
            if (activeAu == null) {
                val dots = universes.filter { item.character.hasAuEntry(it.id) }
                if (dots.isNotEmpty()) {
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        dots.take(5).forEach { au ->
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(au.color))
                            )
                        }
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    item.character.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StarRow(
                    star = item.displayStar,
                    isDismissed = item.character.isDismissed,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun FullBleedImage(url: String?, position: String?, modifier: Modifier = Modifier) {
    Box(modifier.background(BackgroundDark)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = parseImageAlignment(position),
            modifier = Modifier.fillMaxSize()
        )
    }
}
