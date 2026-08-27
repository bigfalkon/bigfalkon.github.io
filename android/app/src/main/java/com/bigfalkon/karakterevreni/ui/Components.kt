package com.bigfalkon.karakterevreni.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bigfalkon.karakterevreni.data.AlternativeUniverse
import com.bigfalkon.karakterevreni.data.GalleryItem
import kotlinx.coroutines.delay

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
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

/**
 * Galeri kartı: sitedeki `.character-card` görünümü — 2/3 oran, tür rengine göre
 * kenar ışıması, sıraya göre gecikmeli giriş animasyonu, füzyon/emekli isimlerinde
 * fantastik yazı tipi.
 */
@Composable
fun CharacterCard(
    item: GalleryItem,
    universes: List<AlternativeUniverse>,
    activeAu: AlternativeUniverse?,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "cardPress")

    // Kartlar sitedeki gibi sırayla süzülerek gelsin.
    var appeared by remember(item.key) { mutableStateOf(false) }
    LaunchedEffect(item.key) {
        delay((minOf(index, 16) * 35).toLong())
        appeared = true
    }
    val enter by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(420),
        label = "cardEnter"
    )

    val isFusion = item.character.isFusion
    val isDismissed = item.character.isDismissed
    val accent = when {
        activeAu != null -> parseHexColor(activeAu.color)
        isFusion -> FusionColor
        isDismissed -> DismissedColor
        item.character.auData.isNotEmpty() -> Primary.copy(alpha = 0.5f)
        else -> OutlineVar
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                alpha = enter
                translationY = (1f - enter) * 30.dp.toPx()
                scaleX = pressScale * (0.98f + 0.02f * enter)
                scaleY = pressScale * (0.98f + 0.02f * enter)
            }
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = if (activeAu != null || isFusion) 0.55f else 0.35f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        color = SurfaceDark,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Box {
            if (isFusion) {
                PrismGlow(
                    color = activeAu?.let { parseHexColor(it.color) } ?: FusionColor,
                    modifier = Modifier.matchParentSize()
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(220)
                    .build(),
                contentDescription = item.character.name,
                contentScale = ContentScale.Crop,
                alignment = parseImageAlignment(item.imagePosition),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )

            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )

            if (activeAu != null) {
                val auColor = parseHexColor(activeAu.color)
                Text(
                    activeAu.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = auColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, auColor, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            } else {
                val dots = universes.filter { item.character.hasAuEntry(it.id) }
                if (dots.isNotEmpty()) {
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        dots.take(6).forEach { au ->
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(au.color))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
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
                    fontFamily = if (isFusion || isDismissed) Fantastical else Manrope,
                    fontWeight = if (isFusion || isDismissed) FontWeight.Normal else FontWeight.Bold,
                    color = when {
                        isFusion -> Color(0xFFF4D9FF)
                        isDismissed -> Color(0xFFC8C4D0)
                        else -> Color.White
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StarRow(
                    star = item.displayStar,
                    isDismissed = isDismissed,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

/** Detayda sitedeki gibi: arkada bulanık dolgu, önde tam görünen sharp görsel. */
@Composable
fun BlurredBackdropImage(
    url: String?,
    position: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    Box(modifier.background(Color(0xFF17161C))) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .scale(1.15f)
                .blur(20.dp)
                .alpha(0.5f)
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = contentScale,
            alignment = parseImageAlignment(position),
            modifier = Modifier.fillMaxSize()
        )
    }
}
