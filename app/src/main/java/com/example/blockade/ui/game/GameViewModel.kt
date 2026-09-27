package com.example.blockade.ui.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blockade.game.ActionMode
import com.example.blockade.game.Barrier
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.BarrierPlacementResult
import com.example.blockade.game.BlockadeState
import com.example.blockade.game.GridPosition
import com.example.blockade.game.Player
import com.example.blockade.game.barriersRemaining
import com.example.blockade.game.canMove
import com.example.blockade.game.shortestPathLength
import com.example.blockade.game.validateBarrier
import com.example.blockade.game.withBarrier
import com.example.blockade.game.withMove
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameEvent { SELECT, MOVE, BARRIER_PLACED, INVALID_BARRIER, WIN }

data class RejectedBarrier(val barrier: Barrier, val message: String, val id: Long)

data class MatchSummary(
    val winner: Player,
    val turns: Int,
    val movesMade: Int,
    val barriersUsed: Int,
    val durationMillis: Long,
    val runnerUpDistance: Int?,
    val finalState: BlockadeState
)

data class GameUiState(
    val game: BlockadeState = BlockadeState(),
    /** Destination of the token currently animating, or null when idle. */
    val movingTo: GridPosition? = null,
    val canUndo: Boolean = false,
    val rejected: RejectedBarrier? = null
)

class GameViewModel(
    private val recordResult: (winner: Player, turns: Int) -> Unit,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val moveAnimationMillis: Long = MOVE_ANIMATION_MS
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events

    private val history = ArrayDeque<BlockadeState>()
    private var moveJob: Job? = null
    private var messageJob: Job? = null
    private var startedAt = clock()
    private var endedAt: Long? = null
    private var rejectionCounter = 0L

    var summary: MatchSummary? = null
        private set

    fun newGame() {
        moveJob?.cancel()
        messageJob?.cancel()
        history.clear()
        startedAt = clock()
        endedAt = null
        summary = null
        _uiState.value = GameUiState()
    }

    fun setActionMode(mode: ActionMode) {
        val game = _uiState.value.game
        if (!game.isActive || game.isAnimating) return
        if (mode == ActionMode.BARRIER && game.barriersRemaining(game.currentPlayer) <= 0) return
        _uiState.update { it.copy(game = game.copy(actionMode = mode)) }
    }

    fun setOrientation(orientation: BarrierOrientation) {
        _uiState.update { it.copy(game = it.game.copy(barrierOrientation = orientation)) }
    }

    fun selectToken(player: Player) {
        val game = _uiState.value.game
        if (game.isActive && !game.isAnimating && player == game.currentPlayer &&
            game.actionMode == ActionMode.MOVE
        ) {
            _events.tryEmit(GameEvent.SELECT)
        }
    }

    fun moveToken(destination: GridPosition) {
        val game = _uiState.value.game
        if (!game.isActive || game.isAnimating) return
        if (game.actionMode != ActionMode.MOVE) return
        if (!game.canMove(game.currentPlayer, destination)) return

        history.addLast(game)
        _uiState.update {
            it.copy(game = game.copy(isAnimating = true), movingTo = destination, canUndo = false)
        }
        _events.tryEmit(GameEvent.MOVE)

        moveJob = viewModelScope.launch {
            delay(moveAnimationMillis)
            val next = game.withMove(destination)
            _uiState.update {
                it.copy(game = next, movingTo = null, canUndo = next.isActive && history.isNotEmpty())
            }
            if (!next.isActive) finishMatch(next)
        }
    }

    /** Returns the validation result so callers (and tests) can react to rejections. */
    fun placeBarrier(barrier: Barrier): BarrierPlacementResult {
        val game = _uiState.value.game
        if (!game.isActive) return BarrierPlacementResult.GAME_OVER
        if (game.isAnimating || game.actionMode != ActionMode.BARRIER) {
            return BarrierPlacementResult.GAME_OVER
        }
        val result = game.validateBarrier(barrier)
        if (result != BarrierPlacementResult.VALID) {
            reject(barrier, result)
            return result
        }
        history.addLast(game)
        _uiState.update {
            it.copy(game = game.withBarrier(barrier), canUndo = true, rejected = null)
        }
        _events.tryEmit(GameEvent.BARRIER_PLACED)
        return result
    }

    fun undo() {
        val game = _uiState.value.game
        if (game.isAnimating || !game.isActive || history.isEmpty()) return
        val previous = history.removeLast()
        _uiState.update {
            it.copy(
                game = previous.copy(actionMode = ActionMode.MOVE, isAnimating = false),
                canUndo = history.isNotEmpty(),
                rejected = null
            )
        }
    }

    private fun reject(barrier: Barrier, result: BarrierPlacementResult) {
        val message = when (result) {
            BarrierPlacementResult.BLOCKS_PATH -> "PATH CANNOT BE BLOCKED"
            BarrierPlacementResult.DUPLICATE -> "EDGE ALREADY BLOCKED"
            BarrierPlacementResult.NO_BARRIERS_LEFT -> "NO BARRIERS REMAINING"
            BarrierPlacementResult.OUT_OF_BOUNDS -> "OUTSIDE THE BOARD"
            else -> return
        }
        val rejection = RejectedBarrier(barrier, message, ++rejectionCounter)
        _uiState.update { it.copy(rejected = rejection) }
        _events.tryEmit(GameEvent.INVALID_BARRIER)
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            delay(MESSAGE_DURATION_MS)
            _uiState.update { if (it.rejected?.id == rejection.id) it.copy(rejected = null) else it }
        }
    }

    private fun finishMatch(final: BlockadeState) {
        val winner = final.winner ?: return
        if (summary != null) return
        val end = clock()
        endedAt = end
        summary = MatchSummary(
            winner = winner,
            turns = final.turns,
            movesMade = final.movesMade,
            barriersUsed = final.barriersUsed,
            durationMillis = end - startedAt,
            runnerUpDistance = final.shortestPathLength(winner.opponent),
            finalState = final
        )
        recordResult(winner, final.turns)
        _events.tryEmit(GameEvent.WIN)
    }

    companion object {
        const val MOVE_ANIMATION_MS = 190L
        const val MESSAGE_DURATION_MS = 1600L
    }
}
