package com.example.blockade.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.example.blockade.game.ActionMode
import com.example.blockade.game.BOARD_SIZE
import com.example.blockade.game.Barrier
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.BarrierPlacementResult
import com.example.blockade.game.BlockadeState
import com.example.blockade.game.GridPosition
import com.example.blockade.game.LastAction
import com.example.blockade.game.Player
import com.example.blockade.game.barriersRemaining
import com.example.blockade.game.getLegalMoves
import com.example.blockade.game.isEdgeBlocked
import com.example.blockade.game.openBarrierSlots
import com.example.blockade.game.positionOf
import com.example.blockade.game.validateBarrier
import com.example.blockade.ui.components.BarrierStyle
import com.example.blockade.ui.components.BoardGeometry
import com.example.blockade.ui.components.drawBarrier
import com.example.blockade.ui.components.drawBoardBase
import com.example.blockade.ui.components.drawToken
import com.example.blockade.ui.theme.BronzeLight
import com.example.blockade.ui.theme.LocalBoardPalette
import com.example.blockade.ui.theme.colors

@Composable
fun GameBoard(
    state: BlockadeState,
    modifier: Modifier = Modifier,
    movingTo: GridPosition? = null,
    rejected: Barrier? = null,
    showLegalMoves: Boolean = true,
    showLastAction: Boolean = true,
    interactive: Boolean = true,
    onCellTap: (GridPosition) -> Unit = {},
    onBarrierTap: (Barrier) -> Unit = {}
) {
    val palette = LocalBoardPalette.current
    val latestState by rememberUpdatedState(state)
    val latestCellTap by rememberUpdatedState(onCellTap)
    val latestBarrierTap by rememberUpdatedState(onBarrierTap)
    var preview by remember { mutableStateOf<Barrier?>(null) }

    fun target(player: Player): GridPosition =
        if (movingTo != null && state.isAnimating && state.currentPlayer == player) movingTo
        else state.positionOf(player)

    val motion = tween<Offset>(durationMillis = GameViewModel.MOVE_ANIMATION_MS.toInt(), easing = FastOutSlowInEasing)
    val p1 by animateOffsetAsState(target(Player.PLAYER_ONE).toOffset(), motion, label = "p1")
    val p2 by animateOffsetAsState(target(Player.PLAYER_TWO).toOffset(), motion, label = "p2")

    val lastBarrier = (state.lastAction as? LastAction.BarrierPlaced)?.barrier
    val barrierGrow = remember(lastBarrier) { Animatable(if (lastBarrier == null) 1f else 0f) }
    LaunchedEffect(lastBarrier) {
        barrierGrow.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse"
    )
    val winGlow = remember(state.winner) { Animatable(0f) }
    LaunchedEffect(state.winner) {
        if (state.winner != null) winGlow.animateTo(1f, tween(600))
    }

    val inputEnabled = interactive && state.isActive && !state.isAnimating
    val legalMoves = if (state.isActive && !state.isAnimating && state.actionMode == ActionMode.MOVE) {
        state.getLegalMoves(state.currentPlayer)
    } else emptyList()
    val barrierMode = state.isActive && state.actionMode == ActionMode.BARRIER &&
        state.barriersRemaining(state.currentPlayer) > 0

    BoxWithConstraints(modifier.aspectRatio(1f)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(inputEnabled) {
                    if (!inputEnabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val s = latestState
                            if (s.actionMode != ActionMode.BARRIER) return@detectTapGestures
                            val geo = BoardGeometry(size.width.toFloat(), BOARD_SIZE)
                            val candidate = geo.barrierAt(offset, s.barrierOrientation)
                                ?: return@detectTapGestures
                            preview = candidate
                            val released = tryAwaitRelease()
                            preview = null
                            if (released) latestBarrierTap(candidate)
                        },
                        onTap = { offset ->
                            val s = latestState
                            if (s.actionMode != ActionMode.MOVE) return@detectTapGestures
                            val geo = BoardGeometry(size.width.toFloat(), BOARD_SIZE)
                            val (r, c) = geo.cellAt(offset) ?: return@detectTapGestures
                            latestCellTap(GridPosition(r, c))
                        }
                    )
                }
        ) {
            val geo = BoardGeometry(size.width, BOARD_SIZE)
            drawBoardBase(geo, palette)

            val mover = state.currentPlayer.colors()

            // Last action marker.
            if (showLastAction && !state.isAnimating) {
                val last = state.lastAction
                if (last is LastAction.Moved) {
                    val rect = geo.cellRect(last.to.row, last.to.col)
                    drawRoundRect(
                        color = last.player.colors().light.copy(alpha = 0.5f),
                        topLeft = rect.topLeft, size = rect.size,
                        cornerRadius = CornerRadius(geo.cellSize * 0.1f),
                        style = Stroke(width = geo.gap * 0.28f)
                    )
                    drawCircle(
                        color = last.player.colors().base.copy(alpha = 0.45f),
                        radius = geo.cellSize * 0.07f,
                        center = geo.center(last.from.row.toFloat(), last.from.col.toFloat())
                    )
                }
            }

            // Legal destinations.
            if (showLegalMoves) {
                legalMoves.forEach { pos ->
                    val center = geo.center(pos.row.toFloat(), pos.col.toFloat())
                    drawCircle(mover.light.copy(alpha = 0.16f), radius = geo.cellSize * 0.3f, center = center)
                    drawCircle(
                        mover.light.copy(alpha = 0.55f), radius = geo.cellSize * 0.3f, center = center,
                        style = Stroke(width = geo.gap * 0.18f)
                    )
                }
            }

            // Available barrier slots.
            if (barrierMode && interactive) {
                state.openBarrierSlots(state.barrierOrientation).forEach { slot ->
                    drawCircle(
                        palette.dot.copy(alpha = 0.75f),
                        radius = geo.gap * 0.26f,
                        center = geo.barrierRect(slot).center
                    )
                }
            }

            // Placed barriers.
            state.barriers.forEach { barrier ->
                val isLast = barrier == lastBarrier
                drawBarrier(
                    geo.barrierRect(barrier, if (isLast) barrierGrow.value else 1f),
                    BarrierStyle.SOLID,
                    highlight = if (isLast && showLastAction) 1f else 0f
                )
            }

            // Barrier preview under the finger, or a just-rejected placement.
            preview?.let { candidate ->
                val valid = state.validateBarrier(candidate) == BarrierPlacementResult.VALID
                drawBarrier(geo.barrierRect(candidate), if (valid) BarrierStyle.PREVIEW else BarrierStyle.INVALID)
            }
            if (preview == null && rejected != null) {
                drawBarrier(geo.barrierRect(rejected), BarrierStyle.INVALID)
            }

            // Tokens.
            val selectable = state.isActive && state.actionMode == ActionMode.MOVE && !state.isAnimating
            Player.entries.forEach { player ->
                val pos = if (player == Player.PLAYER_ONE) p1 else p2
                drawToken(
                    player = player,
                    center = geo.center(pos.y, pos.x),
                    radius = geo.tokenRadius,
                    selection = if (selectable && interactive && player == state.currentPlayer) pulse else 0f,
                    glow = if (state.winner == player) winGlow.value else 0f
                )
            }

            if (!state.isActive) {
                val winner = state.winner
                if (winner != null) {
                    val goalRect = geo.cellRect(winner.goalRow, 0)
                    drawRect(
                        color = BronzeLight.copy(alpha = 0.06f * winGlow.value),
                        topLeft = Offset(geo.pad, goalRect.top),
                        size = androidx.compose.ui.geometry.Size(size.width - 2 * geo.pad, geo.cellSize)
                    )
                }
            }
        }

        if (interactive) {
            BoardSemanticsOverlay(
                state = state,
                legalMoves = legalMoves,
                padding = maxWidth * 0.045f,
                onCellTap = onCellTap,
                onBarrierTap = onBarrierTap
            )
        }
    }
}

private fun GridPosition.toOffset() = Offset(col.toFloat(), row.toFloat())

/**
 * Invisible accessibility layer: one node per cell with a spoken description and actions.
 * It has no pointer handling, so touches pass through to the canvas.
 */
@Composable
private fun BoardSemanticsOverlay(
    state: BlockadeState,
    legalMoves: List<GridPosition>,
    padding: androidx.compose.ui.unit.Dp,
    onCellTap: (GridPosition) -> Unit,
    onBarrierTap: (Barrier) -> Unit
) {
    val barrierMode = state.isActive && state.actionMode == ActionMode.BARRIER
    Column(Modifier.fillMaxSize().padding(padding)) {
        for (r in 0 until BOARD_SIZE) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until BOARD_SIZE) {
                    val pos = GridPosition(r, c)
                    val legal = pos in legalMoves
                    val description = describeCell(state, pos, legal)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clearAndSetSemantics {
                                contentDescription = description
                                if (legal) {
                                    onClick(label = "Move here") { onCellTap(pos); true }
                                }
                                if (barrierMode) {
                                    val actions = mutableListOf<CustomAccessibilityAction>()
                                    if (r < BOARD_SIZE - 1) {
                                        actions += CustomAccessibilityAction(
                                            "Place horizontal barrier between row ${r + 1} and row ${r + 2}"
                                        ) { onBarrierTap(Barrier(r, c, BarrierOrientation.HORIZONTAL)); true }
                                    }
                                    if (c < BOARD_SIZE - 1) {
                                        actions += CustomAccessibilityAction(
                                            "Place vertical barrier between column ${c + 1} and column ${c + 2}"
                                        ) { onBarrierTap(Barrier(r, c, BarrierOrientation.VERTICAL)); true }
                                    }
                                    customActions = actions
                                }
                            }
                    )
                }
            }
        }
    }
}

private fun describeCell(state: BlockadeState, pos: GridPosition, legal: Boolean): String = buildString {
    when (pos) {
        state.player1Position -> append("Player 1 token, ")
        state.player2Position -> append("Player 2 token, ")
    }
    append("row ${pos.row + 1} column ${pos.col + 1}")
    if (pos.row == Player.PLAYER_ONE.goalRow) append(", Player 1 goal row")
    if (pos.row == Player.PLAYER_TWO.goalRow) append(", Player 2 goal row")
    if (legal) append(", legal move")
    val sides = listOf(
        "above" to GridPosition(pos.row - 1, pos.col),
        "below" to GridPosition(pos.row + 1, pos.col),
        "left" to GridPosition(pos.row, pos.col - 1),
        "right" to GridPosition(pos.row, pos.col + 1)
    ).filter { (_, n) -> n.isInsideBoard && state.isEdgeBlocked(pos, n) }.map { it.first }
    if (sides.isNotEmpty()) append(", barrier ${sides.joinToString(" and ")}")
}
