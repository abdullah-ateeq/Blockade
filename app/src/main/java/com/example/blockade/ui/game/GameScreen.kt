package com.example.blockade.ui.game

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blockade.data.Settings
import com.example.blockade.feedback.GameSound
import com.example.blockade.feedback.SoundPlayer
import com.example.blockade.game.ActionMode
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.BlockadeState
import com.example.blockade.game.Player
import com.example.blockade.game.STARTING_BARRIERS
import com.example.blockade.game.barriersRemaining
import com.example.blockade.game.positionOf
import com.example.blockade.game.shortestPathLength
import com.example.blockade.ui.components.QuietButton
import com.example.blockade.ui.components.SegmentedSelector
import com.example.blockade.ui.components.TokenIcon
import com.example.blockade.ui.theme.Bronze
import com.example.blockade.ui.theme.colors
import com.example.blockade.ui.theme.glyph
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settings: Settings,
    soundPlayer: SoundPlayer,
    onGameOver: () -> Unit,
    onHome: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val game = ui.game
    var confirmRestart by rememberSaveable { mutableStateOf(false) }

    GameFeedback(viewModel, settings, soundPlayer)

    val latestGameOver by rememberUpdatedState(onGameOver)
    LaunchedEffect(game.winner) {
        if (game.winner != null) {
            delay(1200)
            latestGameOver()
        }
    }

    val board: @Composable (Modifier) -> Unit = { modifier ->
        GameBoard(
            state = game,
            movingTo = ui.movingTo,
            rejected = ui.rejected?.barrier,
            showLegalMoves = settings.showLegalMoves,
            showLastAction = settings.showLastAction,
            onCellTap = { pos ->
                if (pos == game.positionOf(game.currentPlayer)) viewModel.selectToken(game.currentPlayer)
                else viewModel.moveToken(pos)
            },
            onBarrierTap = { viewModel.placeBarrier(it) },
            modifier = modifier
        )
    }
    val controls: @Composable () -> Unit = {
        ActionControls(
            game = game,
            onMode = viewModel::setActionMode,
            onOrientation = viewModel::setOrientation
        )
    }
    val footer: @Composable () -> Unit = {
        GameFooter(
            canUndo = ui.canUndo && !game.isAnimating && game.isActive,
            active = game.isActive,
            onUndo = viewModel::undo,
            onRestart = { confirmRestart = true },
            onHome = onHome,
            onHowToPlay = onHowToPlay,
            onSettings = onSettings
        )
    }
    val status: @Composable () -> Unit = { TurnStatus(game, ui.rejected?.message) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (maxWidth > maxHeight) {
                // Landscape / tablet: board left, status and controls right.
                Row(
                    Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        board(Modifier.fillMaxHeight())
                    }
                    Column(
                        Modifier.widthIn(max = 380.dp).weight(0.8f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        PlayerPanel(game, Player.PLAYER_TWO)
                        status()
                        PlayerPanel(game, Player.PLAYER_ONE)
                        controls()
                        footer()
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlayerPanel(game, Player.PLAYER_TWO, Modifier.widthIn(max = 560.dp))
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        board(Modifier.widthIn(max = 560.dp))
                    }
                    PlayerPanel(game, Player.PLAYER_ONE, Modifier.widthIn(max = 560.dp))
                    Column(
                        Modifier.widthIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        status()
                        controls()
                        footer()
                    }
                }
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Restart this match?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "The current board and barrier counts will be cleared.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRestart = false; viewModel.newGame() }) {
                    Text("RESTART", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun PlayerPanel(game: BlockadeState, player: Player, modifier: Modifier = Modifier) {
    val active = game.isActive && game.currentPlayer == player || game.winner == player
    val colors = player.colors()
    val remaining = game.barriersRemaining(player)
    val distance = game.shortestPathLength(player)
    val border by animateColorAsState(
        if (active) colors.base.copy(alpha = 0.9f) else MaterialTheme.colorScheme.outlineVariant,
        label = "panelBorder"
    )
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append("${player.label}. ")
                    append("${player.label.lowercase().replaceFirstChar { it.uppercase() }} has $remaining barriers remaining. ")
                    if (distance != null) append("Distance to goal $distance. ")
                    if (active && game.isActive) append("Current turn.")
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        TokenIcon(player, Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                player.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (active) colors.light else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            BarrierPips(remaining)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$remaining / $STARTING_BARRIERS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "BARRIERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(18.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 52.dp)) {
            Text(
                distance?.toString() ?: "–",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "TO GOAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BarrierPips(remaining: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(STARTING_BARRIERS) { i ->
            Box(
                Modifier
                    .size(width = 14.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i < remaining) Bronze else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun TurnStatus(game: BlockadeState, message: String?) {
    val player = game.winner ?: game.currentPlayer
    val text = when {
        message != null -> message
        game.winner != null -> "${player.label} WINS"
        else -> "${player.label}'S TURN"
    }
    val color = when {
        message != null -> MaterialTheme.colorScheme.error
        else -> player.colors().light
    }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = text to color,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "turn"
        ) { (label, tint) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (message == null) {
                    Text(
                        player.glyph,
                        color = tint,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(label, style = MaterialTheme.typography.titleMedium, color = tint, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ActionControls(
    game: BlockadeState,
    onMode: (ActionMode) -> Unit,
    onOrientation: (BarrierOrientation) -> Unit
) {
    val hasBarriers = game.barriersRemaining(game.currentPlayer) > 0
    val enabled = game.isActive && !game.isAnimating
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedSelector(
            options = ActionMode.entries,
            selected = game.actionMode,
            label = { it.name },
            onSelect = onMode,
            enabled = { enabled && (it == ActionMode.MOVE || hasBarriers) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
            if (game.actionMode == ActionMode.BARRIER && game.isActive) {
                SegmentedSelector(
                    options = BarrierOrientation.entries,
                    selected = game.barrierOrientation,
                    label = { it.name },
                    onSelect = onOrientation,
                    enabled = { enabled },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    when {
                        !game.isActive -> "Match complete"
                        !hasBarriers -> "No barriers left — move one cell"
                        else -> "Tap a highlighted cell to move"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GameFooter(
    canUndo: Boolean,
    active: Boolean,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuietButton("UNDO", onUndo, enabled = canUndo)
        QuietButton("RESTART", onRestart, enabled = active)
        Box {
            QuietButton("MENU", { menuOpen = true })
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MenuItem("How to Play") { menuOpen = false; onHowToPlay() }
                MenuItem("Settings") { menuOpen = false; onSettings() }
                MenuItem("Exit to Home") { menuOpen = false; onHome() }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick
    )
}

/** Routes game events to subtle sounds and haptics, respecting user settings. */
@Composable
private fun GameFeedback(viewModel: GameViewModel, settings: Settings, soundPlayer: SoundPlayer) {
    val view = LocalView.current
    val latestSettings by rememberUpdatedState(settings)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val s = latestSettings
            if (s.soundEnabled) {
                soundPlayer.play(
                    when (event) {
                        GameEvent.SELECT -> GameSound.SELECT
                        GameEvent.MOVE -> GameSound.MOVE
                        GameEvent.BARRIER_PLACED -> GameSound.BARRIER
                        GameEvent.INVALID_BARRIER -> GameSound.INVALID
                        GameEvent.WIN -> GameSound.WIN
                    }
                )
            }
            if (s.hapticsEnabled) {
                val constant = when (event) {
                    GameEvent.SELECT, GameEvent.MOVE -> HapticFeedbackConstants.CLOCK_TICK
                    GameEvent.BARRIER_PLACED -> HapticFeedbackConstants.VIRTUAL_KEY
                    GameEvent.INVALID_BARRIER ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
                        else HapticFeedbackConstants.LONG_PRESS
                    GameEvent.WIN ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                        else HapticFeedbackConstants.LONG_PRESS
                }
                view.performHapticFeedback(constant)
            }
        }
    }
}
