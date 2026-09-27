package com.example.blockade.ui.screens

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.blockade.ui.components.PrimaryButton
import com.example.blockade.ui.components.SecondaryButton
import com.example.blockade.ui.components.StatBlock
import com.example.blockade.ui.components.TokenIcon
import com.example.blockade.ui.components.VerticalRule
import com.example.blockade.ui.game.GameBoard
import com.example.blockade.ui.game.MatchSummary
import com.example.blockade.ui.theme.colors

@Composable
fun ResultScreen(
    summary: MatchSummary?,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (summary != null) {
                    val winner = summary.winner
                    TokenIcon(winner, Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${winner.label} WINS",
                        style = MaterialTheme.typography.displaySmall,
                        color = winner.colors().light,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        summary.runnerUpDistance?.let { d ->
                            "${winner.opponent.label.lowercase().replaceFirstChar { it.uppercase() }} " +
                                "finished $d ${if (d == 1) "step" else "steps"} from goal"
                        } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    GameBoard(
                        state = summary.finalState,
                        interactive = false,
                        showLegalMoves = false,
                        showLastAction = true,
                        modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth()
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBlock(summary.turns.toString(), "Turns")
                        VerticalRule()
                        StatBlock(summary.movesMade.toString(), "Moves")
                        VerticalRule()
                        StatBlock(summary.barriersUsed.toString(), "Barriers")
                        VerticalRule()
                        StatBlock(formatDuration(summary.durationMillis), "Time")
                    }
                } else {
                    Text(
                        "MATCH ENDED",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.height(36.dp))
                PrimaryButton("PLAY AGAIN", onPlayAgain, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                SecondaryButton("HOME", onHome, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
