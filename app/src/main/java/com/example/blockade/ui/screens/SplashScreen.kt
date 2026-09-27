package com.example.blockade.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.blockade.game.Barrier
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.Player
import com.example.blockade.ui.components.BoardGeometry
import com.example.blockade.ui.components.drawBarrier
import com.example.blockade.ui.components.drawBoardBase
import com.example.blockade.ui.components.drawToken
import com.example.blockade.ui.theme.BronzeLight
import com.example.blockade.ui.theme.LocalBoardPalette
import kotlinx.coroutines.delay

private const val SPLASH_CELLS = 5

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val latestFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1350, easing = LinearEasing))
        delay(200)
        latestFinished()
    }
    val palette = LocalBoardPalette.current
    val t = progress.value

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(
                Modifier
                    .size(180.dp)
                    .alpha(phase(t, 0f, 0.22f))
            ) {
                val geo = BoardGeometry(size.width, SPLASH_CELLS, padFraction = 0.06f)
                drawBoardBase(geo, palette)

                // Barrier slides in from the right, directly in front of Player 1.
                val barrier = Barrier(3, 2, BarrierOrientation.HORIZONTAL)
                val slide = easeOut(phase(t, 0.3f, 0.52f))
                if (slide > 0f) {
                    val rect = geo.barrierRect(barrier)
                    val dx = (1f - slide) * geo.pitch * 2.2f
                    translate(left = dx) { drawBarrier(rect, highlight = 1f - slide) }
                }

                // Planned route: straight up, then redrawn around the barrier.
                val straight = listOf(4 to 2, 0 to 2)
                val detour = listOf(4 to 2, 4 to 3, 0 to 3)
                val route = if (t < 0.55f) straight else detour
                val draw = if (t < 0.55f) phase(t, 0.18f, 0.32f) * (1f - phase(t, 0.45f, 0.55f))
                else easeOut(phase(t, 0.55f, 0.75f))
                if (draw > 0f) {
                    val path = Path()
                    route.forEachIndexed { i, (r, c) ->
                        val p = geo.center(r.toFloat(), c.toFloat())
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    val dash = geo.pitch * 0.18f
                    drawPath(
                        path,
                        color = BronzeLight.copy(alpha = 0.55f * draw),
                        style = Stroke(
                            width = geo.gap * 0.35f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.8f))
                        )
                    )
                }

                val tokensIn = phase(t, 0.12f, 0.3f)
                val advance = easeOut(phase(t, 0.72f, 0.88f))
                val p1 = Offset(lerp(2f, 3f, advance), 4f)
                drawToken(
                    Player.PLAYER_ONE,
                    geo.center(p1.y, p1.x),
                    geo.tokenRadius * tokensIn,
                    alpha = tokensIn
                )
                drawToken(Player.PLAYER_TWO, geo.center(0f, 2f), geo.tokenRadius * tokensIn, alpha = tokensIn)
            }
            Spacer(Modifier.height(36.dp))
            val titleIn = phase(t, 0.78f, 1f)
            Text(
                "BLOCKADE",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = titleIn
                    translationY = (1f - titleIn) * 12.dp.toPx()
                }
            )
        }
    }
}

private fun phase(t: Float, start: Float, end: Float) = ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x)
