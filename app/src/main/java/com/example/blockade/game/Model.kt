package com.example.blockade.game

const val BOARD_SIZE = 7
const val STARTING_BARRIERS = 6

enum class Player(val number: Int) {
    PLAYER_ONE(1),
    PLAYER_TWO(2);

    val opponent: Player
        get() = if (this == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE

    /** Player 1 races to the top row, Player 2 to the bottom row. */
    val goalRow: Int
        get() = if (this == PLAYER_ONE) 0 else BOARD_SIZE - 1

    val label: String
        get() = "PLAYER $number"
}

data class GridPosition(val row: Int, val col: Int) {
    val isInsideBoard: Boolean
        get() = row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
}

enum class BarrierOrientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * A barrier blocks exactly one edge between two orthogonally adjacent cells.
 *
 * - HORIZONTAL at (row, col) blocks movement between (row, col) and (row + 1, col).
 *   It is drawn as a horizontal bar in the groove below cell (row, col). Valid rows: 0..5.
 * - VERTICAL at (row, col) blocks movement between (row, col) and (row, col + 1).
 *   It is drawn as a vertical bar in the groove right of cell (row, col). Valid cols: 0..5.
 */
data class Barrier(
    val row: Int,
    val col: Int,
    val orientation: BarrierOrientation
) {
    val first: GridPosition
        get() = GridPosition(row, col)

    val second: GridPosition
        get() = when (orientation) {
            BarrierOrientation.HORIZONTAL -> GridPosition(row + 1, col)
            BarrierOrientation.VERTICAL -> GridPosition(row, col + 1)
        }

    val isInsideBoard: Boolean
        get() = first.isInsideBoard && second.isInsideBoard

    companion object {
        /** The canonical barrier for the edge between [a] and [b], or null if they are not adjacent. */
        fun between(a: GridPosition, b: GridPosition): Barrier? {
            val dr = b.row - a.row
            val dc = b.col - a.col
            return when {
                dc == 0 && dr == 1 -> Barrier(a.row, a.col, BarrierOrientation.HORIZONTAL)
                dc == 0 && dr == -1 -> Barrier(b.row, b.col, BarrierOrientation.HORIZONTAL)
                dr == 0 && dc == 1 -> Barrier(a.row, a.col, BarrierOrientation.VERTICAL)
                dr == 0 && dc == -1 -> Barrier(b.row, b.col, BarrierOrientation.VERTICAL)
                else -> null
            }
        }
    }
}

enum class ActionMode {
    MOVE,
    BARRIER
}

sealed interface LastAction {
    val player: Player

    data class Moved(
        override val player: Player,
        val from: GridPosition,
        val to: GridPosition
    ) : LastAction

    data class BarrierPlaced(
        override val player: Player,
        val barrier: Barrier
    ) : LastAction
}

data class BlockadeState(
    val player1Position: GridPosition = GridPosition(BOARD_SIZE - 1, BOARD_SIZE / 2),
    val player2Position: GridPosition = GridPosition(0, BOARD_SIZE / 2),
    val barriers: Set<Barrier> = emptySet(),
    val player1BarriersRemaining: Int = STARTING_BARRIERS,
    val player2BarriersRemaining: Int = STARTING_BARRIERS,
    val currentPlayer: Player = Player.PLAYER_ONE,
    val actionMode: ActionMode = ActionMode.MOVE,
    val barrierOrientation: BarrierOrientation = BarrierOrientation.HORIZONTAL,
    val winner: Player? = null,
    val turns: Int = 0,
    val movesMade: Int = 0,
    val lastAction: LastAction? = null,
    val isAnimating: Boolean = false
) {
    val isActive: Boolean
        get() = winner == null

    val barriersUsed: Int
        get() = barriers.size
}

enum class BarrierPlacementResult {
    VALID,
    GAME_OVER,
    OUT_OF_BOUNDS,
    DUPLICATE,
    NO_BARRIERS_LEFT,
    BLOCKS_PATH
}
