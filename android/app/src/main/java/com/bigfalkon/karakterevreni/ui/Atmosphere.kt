package com.bigfalkon.karakterevreni.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Sitedeki `aurora` arka planının karşılığı: yavaşça sürüklenen iki radyal ışıma.
 * Bir evren seçiliyken tonu o evrenin rengine döner.
 */
@Composable
fun AuroraBackground(accent: Color?, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val first = accent ?: Primary
    val second = if (accent != null) Primary else FusionColor

    Canvas(modifier.fillMaxSize()) {
        drawRect(BackgroundDark)
        radial(
            color = first.copy(alpha = 0.16f),
            center = Offset(size.width * (0.15f + 0.10f * drift), size.height * (0.25f - 0.05f * drift)),
            radius = size.minDimension * 0.95f
        )
        radial(
            color = second.copy(alpha = 0.12f),
            center = Offset(size.width * (0.85f - 0.08f * drift), size.height * (0.65f + 0.05f * drift)),
            radius = size.minDimension * 1.05f
        )
    }
}

private fun DrawScope.radial(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** Füzyon kartlarının arkasındaki prizma parıltısı (sitedeki `prism` animasyonu). */
@Composable
fun PrismGlow(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "prism")
    val scale by transition.animateFloat(
        initialValue = 1.5f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(5_000), RepeatMode.Reverse),
        label = "prismScale"
    )
    Canvas(modifier) {
        val radius = size.minDimension * 0.5f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(size.width / 2f, size.height),
                radius = radius
            ),
            radius = radius,
            center = Offset(size.width / 2f, size.height)
        )
    }
}
