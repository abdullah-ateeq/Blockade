package com.example.blockade.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.blockade.data.Statistics
import com.example.blockade.game.Player
import com.example.blockade.ui.components.PrimaryButton
import com.example.blockade.ui.components.SecondaryButton
import com.example.blockade.ui.components.StatBlock
import com.example.blockade.ui.components.VerticalRule
import com.example.blockade.ui.components.drawBarrier
import com.example.blockade.ui.components.drawToken

@Composable
fun HomeScreen(
    statistics: Statistics,
    onPlay: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Emblem()
                Spacer(Modifier.height(28.dp))
                Text(
                    "BLOCKADE",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Race forward. Block wisely.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(44.dp))
                PrimaryButton("PLAY", onPlay, Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    "LOCAL 2 PLAYER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("HOW TO PLAY", onHowToPlay, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("SETTINGS", onSettings, Modifier.weight(1f))
                        SecondaryButton("ABOUT", onAbout, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(36.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBlock(statistics.gamesPlayed.toString(), "Games")
                    VerticalRule()
                    StatBlock(statistics.player1Wins.toString(), "P1 Wins")
                    VerticalRule()
                    StatBlock(statistics.player2Wins.toString(), "P2 Wins")
                }
            }
        }
    }
}

@Composable
private fun Emblem() {
    Canvas(
        Modifier
            .size(width = 120.dp, height = 72.dp)
            .clearAndSetSemantics { }
    ) {
        val r = size.height * 0.2f
        val midY = size.height / 2
        val thickness = size.height * 0.07f
        drawBarrier(
            Rect(size.width * 0.44f, midY - size.height * 0.36f, size.width * 0.44f + thickness, midY + size.height * 0.36f)
        )
        drawToken(Player.PLAYER_ONE, Offset(size.width * 0.22f, midY), r)
        drawToken(Player.PLAYER_TWO, Offset(size.width * 0.74f, midY), r)
    }
}
