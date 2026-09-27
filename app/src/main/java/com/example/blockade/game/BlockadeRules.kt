package com.example.blockade.game

/**
 * Pure game rules. Every function here is side-effect free: "temporarily adding" a barrier
 * is done by validating against a copy of the state, so nothing ever needs to be rolled back.
 */

private val DIRECTIONS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun getAdjacentPositions(position: GridPosition): List<GridPosition> =
    DIRECTIONS
        .map { (dr, dc) -> GridPosition(position.row + dr, position.col + dc) }
        .filter { it.isInsideBoard }

fun BlockadeState.positionOf(player: Player): GridPosition =
    if (player == Player.PLAYER_ONE) player1Position else player2Position

fun BlockadeState.barriersRemaining(player: Player): Int =
    if (player == Player.PLAYER_ONE) player1BarriersRemaining else player2BarriersRemaining

fun BlockadeState.hasReachedGoal(player: Player): Boolean =
    positionOf(player).row == player.goalRow

/**
 * Single source of truth for whether movement between two cells is obstructed by a barrier.
 * Cells that are not orthogonally adjacent never share an open edge.
 */
fun BlockadeState.isEdgeBlocked(from: GridPosition, to: GridPosition): Boolean {
    val edge = Barrier.between(from, to) ?: return true
    return edge in barriers
}

fun BlockadeState.canMove(player: Player, destination: GridPosition): Boolean {
    if (!isActive) return false
    val from = positionOf(player)
    return destination.isInsideBoard &&
        destination in getAdjacentPositions(from) &&
        destination != positionOf(player.opponent) &&
        !isEdgeBlocked(from, destination)
}

fun BlockadeState.getLegalMoves(player: Player): List<GridPosition> =
    getAdjacentPositions(positionOf(player)).filter { canMove(player, it) }

/**
 * Breadth-first search for the shortest route to [player]'s goal row.
 * The opponent's token is treated as traversable: it can always move out of the way later,
 * so only barriers count as structural obstructions. Returns null when no route exists.
 */
fun BlockadeState.shortestPathLength(player: Player): Int? =
    shortestPath(player)?.let { it.size - 1 }

fun BlockadeState.shortestPath(player: Player): List<GridPosition>? {
    val start = positionOf(player)
    val previous = HashMap<GridPosition, GridPosition?>()
    previous[start] = null
    val queue = ArrayDeque<GridPosition>()
    queue.addLast(start)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (current.row == player.goalRow) {
            val path = mutableListOf<GridPosition>()
            var step: GridPosition? = current
            while (step != null) {
                path.add(step)
                step = previous[step]
            }
            return path.asReversed()
        }
        for (next in getAdjacentPositions(current)) {
            if (next !in previous && !isEdgeBlocked(current, next)) {
                previous[next] = current
                queue.addLast(next)
            }
        }
    }
    return null
}

fun BlockadeState.hasPathToGoal(player: Player): Boolean = shortestPath(player) != null

fun BlockadeState.validateBarrier(barrier: Barrier): BarrierPlacementResult {
    if (!isActive) return BarrierPlacementResult.GAME_OVER
    if (!barrier.isInsideBoard) return BarrierPlacementResult.OUT_OF_BOUNDS
    if (barrier in barriers) return BarrierPlacementResult.DUPLICATE
    if (barriersRemaining(currentPlayer) <= 0) return BarrierPlacementResult.NO_BARRIERS_LEFT
    val trial = copy(barriers = barriers + barrier)
    if (!trial.hasPathToGoal(Player.PLAYER_ONE) || !trial.hasPathToGoal(Player.PLAYER_TWO)) {
        return BarrierPlacementResult.BLOCKS_PATH
    }
    return BarrierPlacementResult.VALID
}

fun BlockadeState.canPlaceBarrier(barrier: Barrier): Boolean =
    validateBarrier(barrier) == BarrierPlacementResult.VALID

/** Applies a move that has already been validated with [canMove]. */
fun BlockadeState.withMove(destination: GridPosition): BlockadeState {
    val mover = currentPlayer
    val from = positionOf(mover)
    val moved = if (mover == Player.PLAYER_ONE) {
        copy(player1Position = destination)
    } else {
        copy(player2Position = destination)
    }
    val won = moved.hasReachedGoal(mover)
    return moved.copy(
        turns = turns + 1,
        movesMade = movesMade + 1,
        lastAction = LastAction.Moved(mover, from, destination),
        winner = if (won) mover else null,
        currentPlayer = if (won) mover else mover.opponent,
        actionMode = ActionMode.MOVE,
        isAnimating = false
    )
}

/** Applies a barrier that has already been validated with [validateBarrier]. */
fun BlockadeState.withBarrier(barrier: Barrier): BlockadeState {
    val placer = currentPlayer
    val placed = copy(barriers = barriers + barrier)
    val counted = if (placer == Player.PLAYER_ONE) {
        placed.copy(player1BarriersRemaining = player1BarriersRemaining - 1)
    } else {
        placed.copy(player2BarriersRemaining = player2BarriersRemaining - 1)
    }
    return counted.copy(
        turns = turns + 1,
        lastAction = LastAction.BarrierPlaced(placer, barrier),
        currentPlayer = placer.opponent,
        actionMode = ActionMode.MOVE,
        isAnimating = false
    )
}

/** Barriers of [orientation] that are geometrically inside the board and not already placed. */
fun BlockadeState.openBarrierSlots(orientation: BarrierOrientation): List<Barrier> {
    val rows = if (orientation == BarrierOrientation.HORIZONTAL) BOARD_SIZE - 1 else BOARD_SIZE
    val cols = if (orientation == BarrierOrientation.VERTICAL) BOARD_SIZE - 1 else BOARD_SIZE
    val slots = ArrayList<Barrier>(rows * cols)
    for (r in 0 until rows) for (c in 0 until cols) {
        val b = Barrier(r, c, orientation)
        if (b !in barriers) slots.add(b)
    }
    return slots
}
