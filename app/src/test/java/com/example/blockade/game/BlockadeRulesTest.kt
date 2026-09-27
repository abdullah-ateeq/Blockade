package com.example.blockade.game

import com.example.blockade.game.BarrierOrientation.HORIZONTAL
import com.example.blockade.game.BarrierOrientation.VERTICAL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockadeRulesTest {

    private val initial = BlockadeState()

    @Test
    fun initialSetup() {
        assertEquals(GridPosition(6, 3), initial.player1Position)
        assertEquals(GridPosition(0, 3), initial.player2Position)
        assertTrue(initial.barriers.isEmpty())
        assertEquals(6, initial.player1BarriersRemaining)
        assertEquals(6, initial.player2BarriersRemaining)
        assertEquals(Player.PLAYER_ONE, initial.currentPlayer)
        assertEquals(ActionMode.MOVE, initial.actionMode)
        assertNull(initial.winner)
    }

    @Test
    fun legalOrthogonalMovement() {
        val moves = initial.getLegalMoves(Player.PLAYER_ONE).toSet()
        assertEquals(setOf(GridPosition(5, 3), GridPosition(6, 2), GridPosition(6, 4)), moves)
        val next = initial.withMove(GridPosition(5, 3))
        assertEquals(GridPosition(5, 3), next.player1Position)
        assertEquals(Player.PLAYER_TWO, next.currentPlayer)
        assertEquals(1, next.turns)
    }

    @Test
    fun diagonalAndLongMovesRejected() {
        assertFalse(initial.canMove(Player.PLAYER_ONE, GridPosition(5, 2)))
        assertFalse(initial.canMove(Player.PLAYER_ONE, GridPosition(4, 3)))
        assertFalse(initial.canMove(Player.PLAYER_ONE, GridPosition(7, 3)))
    }

    @Test
    fun cannotMoveOntoOpponent() {
        val state = initial.copy(player1Position = GridPosition(1, 3))
        assertFalse(state.canMove(Player.PLAYER_ONE, GridPosition(0, 3)))
        assertFalse(state.canMove(Player.PLAYER_TWO, GridPosition(1, 3)))
    }

    @Test
    fun barrierBlocksMovementInBothDirections() {
        val barrier = Barrier(5, 3, HORIZONTAL) // between (5,3) and (6,3)
        val state = initial.copy(barriers = setOf(barrier))
        assertTrue(state.isEdgeBlocked(GridPosition(6, 3), GridPosition(5, 3)))
        assertTrue(state.isEdgeBlocked(GridPosition(5, 3), GridPosition(6, 3)))
        assertFalse(state.canMove(Player.PLAYER_ONE, GridPosition(5, 3)))

        val vertical = initial.copy(barriers = setOf(Barrier(6, 3, VERTICAL))) // (6,3)|(6,4)
        assertFalse(vertical.canMove(Player.PLAYER_ONE, GridPosition(6, 4)))
        assertTrue(vertical.canMove(Player.PLAYER_ONE, GridPosition(6, 2)))
    }

    @Test
    fun duplicateAndOutOfBoundsBarriersRejected() {
        val barrier = Barrier(2, 2, VERTICAL)
        val state = initial.withBarrier(barrier)
        assertEquals(BarrierPlacementResult.DUPLICATE, state.validateBarrier(barrier))
        assertEquals(BarrierPlacementResult.OUT_OF_BOUNDS, initial.validateBarrier(Barrier(6, 0, HORIZONTAL)))
        assertEquals(BarrierPlacementResult.OUT_OF_BOUNDS, initial.validateBarrier(Barrier(0, 6, VERTICAL)))
    }

    @Test
    fun barrierPlacementDecrementsCountAndSwitchesPlayer() {
        val next = initial.withBarrier(Barrier(1, 3, HORIZONTAL))
        assertEquals(5, next.player1BarriersRemaining)
        assertEquals(6, next.player2BarriersRemaining)
        assertEquals(Player.PLAYER_TWO, next.currentPlayer)
        assertEquals(1, next.barriersUsed)
    }

    @Test
    fun zeroBarriersRemainingRejected() {
        val state = initial.copy(player1BarriersRemaining = 0)
        assertEquals(BarrierPlacementResult.NO_BARRIERS_LEFT, state.validateBarrier(Barrier(2, 2, VERTICAL)))
    }

    @Test
    fun barrierWithRemainingPathsIsValid() {
        val state = initial.copy(
            barriers = setOf(Barrier(6, 2, VERTICAL), Barrier(6, 3, VERTICAL))
        )
        // Walled on both sides but forward remains open.
        assertEquals(BarrierPlacementResult.VALID, state.validateBarrier(Barrier(3, 3, HORIZONTAL)))
    }

    @Test
    fun barrierThatSealsPlayerOneRejected() {
        val state = initial.copy(
            barriers = setOf(Barrier(6, 2, VERTICAL), Barrier(6, 3, VERTICAL))
        )
        val seal = Barrier(5, 3, HORIZONTAL)
        assertEquals(BarrierPlacementResult.BLOCKS_PATH, state.validateBarrier(seal))
        assertFalse(state.canPlaceBarrier(seal))
        // Validation never mutates the source state.
        assertFalse(seal in state.barriers)
    }

    @Test
    fun barrierThatSealsPlayerTwoRejected() {
        val state = initial.copy(
            currentPlayer = Player.PLAYER_TWO,
            barriers = setOf(Barrier(0, 2, VERTICAL), Barrier(0, 3, VERTICAL))
        )
        assertEquals(BarrierPlacementResult.BLOCKS_PATH, state.validateBarrier(Barrier(0, 3, HORIZONTAL)))
    }

    @Test
    fun fullRowWallRejected() {
        val wall = (0 until 6).map { Barrier(3, it, HORIZONTAL) }.toSet()
        val state = initial.copy(barriers = wall, player1BarriersRemaining = 6)
        assertEquals(BarrierPlacementResult.BLOCKS_PATH, state.validateBarrier(Barrier(3, 6, HORIZONTAL)))
    }

    @Test
    fun bfsFindsShortestPathAndIgnoresOpponentToken() {
        assertEquals(6, initial.shortestPathLength(Player.PLAYER_ONE))
        assertEquals(6, initial.shortestPathLength(Player.PLAYER_TWO))
        // Barrier directly ahead forces a one-step detour.
        val blocked = initial.copy(barriers = setOf(Barrier(5, 3, HORIZONTAL)))
        assertEquals(7, blocked.shortestPathLength(Player.PLAYER_ONE))
        assertTrue(blocked.hasPathToGoal(Player.PLAYER_ONE))
        // Opponent standing in the column does not count as a wall.
        val facing = initial.copy(player2Position = GridPosition(5, 3))
        assertEquals(6, facing.shortestPathLength(Player.PLAYER_ONE))
    }

    @Test
    fun bfsReportsNoPathWhenSealed() {
        val sealed = initial.copy(
            barriers = setOf(Barrier(6, 2, VERTICAL), Barrier(6, 3, VERTICAL), Barrier(5, 3, HORIZONTAL))
        )
        assertFalse(sealed.hasPathToGoal(Player.PLAYER_ONE))
        assertNull(sealed.shortestPathLength(Player.PLAYER_ONE))
    }

    @Test
    fun playerOneWinsOnTopRow() {
        val state = initial.copy(player1Position = GridPosition(1, 0))
        val next = state.withMove(GridPosition(0, 0))
        assertEquals(Player.PLAYER_ONE, next.winner)
        assertFalse(next.isActive)
        assertFalse(next.canMove(Player.PLAYER_ONE, GridPosition(0, 1)))
        assertEquals(BarrierPlacementResult.GAME_OVER, next.validateBarrier(Barrier(2, 2, VERTICAL)))
    }

    @Test
    fun playerTwoWinsOnBottomRow() {
        val state = initial.copy(currentPlayer = Player.PLAYER_TWO, player2Position = GridPosition(5, 6))
        val next = state.withMove(GridPosition(6, 6))
        assertEquals(Player.PLAYER_TWO, next.winner)
    }
}
