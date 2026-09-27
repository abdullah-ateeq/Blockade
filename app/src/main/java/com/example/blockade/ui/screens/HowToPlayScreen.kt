package com.example.blockade.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.blockade.game.Barrier
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.Player
import com.example.blockade.ui.components.BoardGeometry
import com.example.blockade.ui.components.ScreenScaffold
import com.example.blockade.ui.components.SectionLabel
import com.example.blockade.ui.components.drawBarrier
import com.example.blockade.ui.components.drawBoardBase
import com.example.blockade.ui.components.drawToken
import com.example.blockade.ui.theme.BronzeLight
import com.example.blockade.ui.theme.LocalBoardPalette
import com.example.blockade.ui.theme.colors

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "How to Play", onBack = onBack) {
        SectionLabel("Objective")
        Spacer(Modifier.height(8.dp))
        Body(
            "Race your token to the opposite side of the 7×7 board. Player 1 (◆) starts at the " +
                "bottom and aims for the top row. Player 2 (●) starts at the top and aims for the bottom row."
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("Each turn, choose one")
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MoveDiagram()
                Spacer(Modifier.height(10.dp))
                Text("MOVE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Caption("One cell up, down, left or right. No diagonals, no jumping.")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                BarrierDiagram()
                Spacer(Modifier.height(10.dp))
                Text("BARRIER", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Caption("Block the edge between two cells to force a longer route.")
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Rules")
        Spacer(Modifier.height(10.dp))
        Rule("Each player has 6 barriers. Placing one uses your turn.")
        Rule("Barriers block movement between cells — never the cells themselves.")
        Rule("You cannot completely block either player. Every barrier must leave both players a route to their goal.")
        Rule("You cannot move onto your opponent's cell.")
        Rule("Reach any cell on your goal row first to win.")

        Spacer(Modifier.height(28.dp))
        SectionLabel("Placing a barrier")
        Spacer(Modifier.height(10.dp))
        Rule("Tap BARRIER, then choose HORIZONTAL or VERTICAL.")
        Rule("Press between two cells to preview. Release to place.")
        Rule("A dashed amber outline means the placement is not allowed.")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun Rule(text: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Text("—", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

private const val DIAGRAM_CELLS = 3

@Composable
private fun MoveDiagram() {
    val palette = LocalBoardPalette.current
    Canvas(
        Modifier
            .size(128.dp)
            .semantics { contentDescription = "Diagram: a token in the centre can move to the four orthogonal neighbours" }
    ) {
        val geo = BoardGeometry(size.width, DIAGRAM_CELLS, padFraction = 0.06f)
        drawBoardBase(geo, palette, goalAccents = false)
        val light = Player.PLAYER_ONE.colors().light
        listOf(0 to 1, 1 to 0, 1 to 2, 2 to 1).forEach { (r, c) ->
            val center = geo.center(r.toFloat(), c.toFloat())
            drawCircle(light.copy(alpha = 0.16f), geo.cellSize * 0.3f, center)
            drawCircle(light.copy(alpha = 0.6f), geo.cellSize * 0.3f, center, style = Stroke(geo.gap * 0.2f))
        }
        drawToken(Player.PLAYER_ONE, geo.center(1f, 1f), geo.tokenRadius)
    }
}

@Composable
private fun BarrierDiagram() {
    val palette = LocalBoardPalette.current
    Canvas(
        Modifier
            .size(128.dp)
            .semantics { contentDescription = "Diagram: a barrier above the token forces a detour to the side" }
    ) {
        val geo = BoardGeometry(size.width, DIAGRAM_CELLS, padFraction = 0.06f)
        drawBoardBase(geo, palette, goalAccents = false)
        val route = listOf(2 to 1, 2 to 2, 0 to 2)
        val path = Path()
        route.forEachIndexed { i, (r, c) ->
            val p = geo.center(r.toFloat(), c.toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        val dash = geo.pitch * 0.14f
        drawPath(
            path, BronzeLight.copy(alpha = 0.6f),
            style = Stroke(
                width = geo.gap * 0.3f, cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
            )
        )
        drawBarrier(geo.barrierRect(Barrier(1, 1, BarrierOrientation.HORIZONTAL)))
        drawToken(Player.PLAYER_ONE, geo.center(2f, 1f), geo.tokenRadius)
    }
}
