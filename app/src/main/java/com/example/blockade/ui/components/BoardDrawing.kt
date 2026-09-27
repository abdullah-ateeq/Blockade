package com.example.blockade.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import com.example.blockade.game.Barrier
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.Player
import com.example.blockade.ui.theme.BoardPalette
import com.example.blockade.ui.theme.Bronze
import com.example.blockade.ui.theme.BronzeDark
import com.example.blockade.ui.theme.BronzeLight
import com.example.blockade.ui.theme.Warning
import com.example.blockade.ui.theme.colors
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Maps logical board coordinates to pixels. Cells sit on a regular pitch with a groove between
 * them; barriers are drawn inside those grooves, never on top of cells.
 */
class BoardGeometry(val size: Float, val cells: Int, padFraction: Float = 0.045f) {
    val pad = size * padFraction
    val pitch = (size - 2 * pad) / cells
    val gap = pitch * 0.17f
    val cellSize = pitch - gap
    val tokenRadius = cellSize * 0.34f

    fun cellRect(row: Int, col: Int) = Rect(
        offset = Offset(pad + col * pitch + gap / 2, pad + row * pitch + gap / 2),
        size = Size(cellSize, cellSize)
    )

    /** Center of a (possibly fractional) logical cell position. */
    fun center(row: Float, col: Float) = Offset(pad + (col + 0.5f) * pitch, pad + (row + 0.5f) * pitch)

    fun barrierRect(barrier: Barrier, lengthScale: Float = 1f): Rect {
        val thickness = gap * 0.92f
        val inset = gap * 0.1f
        val fullLength = pitch - 2 * inset
        val length = fullLength * lengthScale
        return when (barrier.orientation) {
            BarrierOrientation.HORIZONTAL -> {
                val y = pad + (barrier.row + 1) * pitch
                val cx = pad + (barrier.col + 0.5f) * pitch
                Rect(cx - length / 2, y - thickness / 2, cx + length / 2, y + thickness / 2)
            }
            BarrierOrientation.VERTICAL -> {
                val x = pad + (barrier.col + 1) * pitch
                val cy = pad + (barrier.row + 0.5f) * pitch
                Rect(x - thickness / 2, cy - length / 2, x + thickness / 2, cy + length / 2)
            }
        }
    }

    /** Logical cell under a pixel offset, or null if outside the playing area. */
    fun cellAt(offset: Offset): Pair<Int, Int>? {
        val fx = (offset.x - pad) / pitch
        val fy = (offset.y - pad) / pitch
        if (fx < 0 || fy < 0 || fx >= cells || fy >= cells) return null
        return floor(fy).toInt() to floor(fx).toInt()
    }

    /**
     * Nearest barrier slot of the chosen orientation to a touch point. Because orientation is
     * selected up front, any touch resolves to exactly one groove, which keeps targets large.
     */
    fun barrierAt(offset: Offset, orientation: BarrierOrientation): Barrier? {
        val fx = (offset.x - pad) / pitch
        val fy = (offset.y - pad) / pitch
        val slack = 0.35f
        if (fx < -slack || fy < -slack || fx > cells + slack || fy > cells + slack) return null
        return when (orientation) {
            BarrierOrientation.HORIZONTAL -> {
                val boundary = fy.roundToInt().coerceIn(1, cells - 1)
                Barrier(boundary - 1, floor(fx).toInt().coerceIn(0, cells - 1), orientation)
            }
            BarrierOrientation.VERTICAL -> {
                val boundary = fx.roundToInt().coerceIn(1, cells - 1)
                Barrier(floor(fy).toInt().coerceIn(0, cells - 1), boundary - 1, orientation)
            }
        }
    }
}

fun DrawScope.drawBoardBase(geo: BoardGeometry, palette: BoardPalette, goalAccents: Boolean = true) {
    val corner = CornerRadius(geo.pad * 0.9f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(palette.frameEdge, palette.frame)),
        cornerRadius = corner
    )
    // Recessed playing field: the grooves where barriers live.
    val inner = Rect(geo.pad, geo.pad, size.width - geo.pad, size.height - geo.pad)
    drawRoundRect(
        color = palette.groove,
        topLeft = inner.topLeft - Offset(geo.gap * 0.25f, geo.gap * 0.25f),
        size = Size(inner.width + geo.gap * 0.5f, inner.height + geo.gap * 0.5f),
        cornerRadius = CornerRadius(geo.gap)
    )
    if (goalAccents) {
        val stroke = geo.pad * 0.22f
        val inset = geo.pad * 1.6f
        drawLine(
            Player.PLAYER_ONE.colors().base.copy(alpha = 0.9f),
            Offset(inset, geo.pad * 0.45f), Offset(size.width - inset, geo.pad * 0.45f),
            strokeWidth = stroke, cap = StrokeCap.Round
        )
        drawLine(
            Player.PLAYER_TWO.colors().base.copy(alpha = 0.9f),
            Offset(inset, size.height - geo.pad * 0.45f),
            Offset(size.width - inset, size.height - geo.pad * 0.45f),
            strokeWidth = stroke, cap = StrokeCap.Round
        )
    }
    val cellCorner = CornerRadius(geo.cellSize * 0.1f)
    for (r in 0 until geo.cells) for (c in 0 until geo.cells) {
        val rect = geo.cellRect(r, c)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(palette.cellHighlight, palette.cell),
                start = rect.topLeft, end = rect.bottomRight
            ),
            topLeft = rect.topLeft, size = rect.size, cornerRadius = cellCorner
        )
        if (goalAccents && (r == 0 || r == geo.cells - 1)) {
            val tint = if (r == 0) Player.PLAYER_ONE.colors().base else Player.PLAYER_TWO.colors().base
            drawRoundRect(
                color = tint.copy(alpha = 0.09f),
                topLeft = rect.topLeft, size = rect.size, cornerRadius = cellCorner
            )
        }
    }
}

private fun roundedPolygon(points: List<Offset>, t: Float): Path {
    val path = Path()
    val n = points.size
    fun lerpO(a: Offset, b: Offset, f: Float) = Offset(lerp(a.x, b.x, f), lerp(a.y, b.y, f))
    val start = lerpO(points[0], points[1], t)
    path.moveTo(start.x, start.y)
    for (i in 0 until n) {
        val a = points[i]
        val b = points[(i + 1) % n]
        val c = points[(i + 2) % n]
        val lineEnd = lerpO(a, b, 1 - t)
        val curveEnd = lerpO(b, c, t)
        path.lineTo(lineEnd.x, lineEnd.y)
        path.quadraticTo(b.x, b.y, curveEnd.x, curveEnd.y)
    }
    path.close()
    return path
}

private fun diamond(center: Offset, radius: Float, rounding: Float = 0.18f) = roundedPolygon(
    listOf(
        Offset(center.x, center.y - radius),
        Offset(center.x + radius, center.y),
        Offset(center.x, center.y + radius),
        Offset(center.x - radius, center.y)
    ),
    rounding
)

/**
 * Bevelled abstract token. Player 1 is a diamond, Player 2 a disc, each engraved with a matching
 * inner symbol so tokens remain distinguishable without colour.
 */
fun DrawScope.drawToken(
    player: Player,
    center: Offset,
    radius: Float,
    selection: Float = 0f,
    glow: Float = 0f,
    alpha: Float = 1f
) {
    // Radial gradients reject a zero radius (e.g. while a token scales in from nothing).
    if (radius < 0.5f || alpha <= 0f) return
    val colors = player.colors()
    val isDiamond = player == Player.PLAYER_ONE
    val shapeRadius = if (isDiamond) radius * 1.22f else radius

    if (glow > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colors.light.copy(alpha = 0.45f * glow * alpha), Color.Transparent),
                center = center, radius = radius * 2.2f
            ),
            radius = radius * 2.2f, center = center
        )
    }
    if (selection > 0f) {
        drawCircle(
            color = BronzeLight.copy(alpha = selection * alpha),
            radius = radius * 1.42f, center = center,
            style = Stroke(width = radius * 0.08f)
        )
    }
    // Soft contact shadow.
    val shadowCenter = center + Offset(0f, radius * 0.22f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.55f * alpha), Color.Transparent),
            center = shadowCenter, radius = radius * 1.35f
        ),
        radius = radius * 1.35f, center = shadowCenter
    )
    val bevel = Brush.linearGradient(
        listOf(colors.light, colors.base, colors.dark),
        start = center - Offset(radius, radius), end = center + Offset(radius, radius)
    )
    val face = Brush.radialGradient(
        listOf(colors.base, lerpColor(colors.base, colors.dark, 0.35f)),
        center = center - Offset(radius * 0.3f, radius * 0.35f), radius = radius * 1.3f
    )
    val symbolColor = colors.light.copy(alpha = 0.9f * alpha)
    val engraveColor = colors.dark.copy(alpha = 0.8f * alpha)
    val engraveOffset = Offset(0f, radius * 0.05f)

    if (isDiamond) {
        drawPath(diamond(center, shapeRadius), bevel, alpha = alpha)
        drawPath(diamond(center, shapeRadius * 0.8f), face, alpha = alpha)
        drawPath(diamond(center + engraveOffset, radius * 0.3f, 0.1f), engraveColor)
        drawPath(diamond(center, radius * 0.3f, 0.1f), symbolColor)
    } else {
        drawCircle(bevel, radius = shapeRadius, center = center, alpha = alpha)
        drawCircle(face, radius = shapeRadius * 0.8f, center = center, alpha = alpha)
        val ringStroke = Stroke(width = radius * 0.11f)
        drawCircle(engraveColor, radius = radius * 0.3f, center = center + engraveOffset, style = ringStroke)
        drawCircle(symbolColor, radius = radius * 0.3f, center = center, style = ringStroke)
    }
    // Specular sheen.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.22f * alpha), Color.Transparent),
            center = center - Offset(radius * 0.35f, radius * 0.4f), radius = radius * 0.6f
        ),
        radius = radius * 0.6f, center = center - Offset(radius * 0.35f, radius * 0.4f)
    )
}

enum class BarrierStyle { SOLID, PREVIEW, INVALID }

fun DrawScope.drawBarrier(rect: Rect, style: BarrierStyle = BarrierStyle.SOLID, highlight: Float = 0f) {
    val thickness = minOf(rect.width, rect.height)
    val corner = CornerRadius(thickness / 2)
    when (style) {
        BarrierStyle.INVALID -> {
            drawRoundRect(
                color = Warning.copy(alpha = 0.18f),
                topLeft = rect.topLeft, size = rect.size, cornerRadius = corner
            )
            drawRoundRect(
                color = Warning,
                topLeft = rect.topLeft, size = rect.size, cornerRadius = corner,
                style = Stroke(
                    width = thickness * 0.22f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(thickness * 0.7f, thickness * 0.45f))
                )
            )
            return
        }
        BarrierStyle.PREVIEW, BarrierStyle.SOLID -> Unit
    }
    val alpha = if (style == BarrierStyle.PREVIEW) 0.6f else 1f
    if (highlight > 0f || style == BarrierStyle.PREVIEW) {
        val g = if (style == BarrierStyle.PREVIEW) 1f else highlight
        val grow = thickness * 0.9f
        drawRoundRect(
            color = BronzeLight.copy(alpha = 0.22f * g),
            topLeft = rect.topLeft - Offset(grow, grow),
            size = Size(rect.width + 2 * grow, rect.height + 2 * grow),
            cornerRadius = CornerRadius(thickness)
        )
    }
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f * alpha),
        topLeft = rect.topLeft + Offset(thickness * 0.1f, thickness * 0.35f),
        size = rect.size, cornerRadius = corner
    )
    val horizontal = rect.width >= rect.height
    val brush = if (horizontal) {
        Brush.verticalGradient(listOf(BronzeLight, Bronze, BronzeDark), startY = rect.top, endY = rect.bottom)
    } else {
        Brush.horizontalGradient(listOf(BronzeLight, Bronze, BronzeDark), startX = rect.left, endX = rect.right)
    }
    drawRoundRect(brush = brush, topLeft = rect.topLeft, size = rect.size, cornerRadius = corner, alpha = alpha)
}

private fun lerpColor(a: Color, b: Color, f: Float) = Color(
    red = lerp(a.red, b.red, f),
    green = lerp(a.green, b.green, f),
    blue = lerp(a.blue, b.blue, f),
    alpha = lerp(a.alpha, b.alpha, f)
)
