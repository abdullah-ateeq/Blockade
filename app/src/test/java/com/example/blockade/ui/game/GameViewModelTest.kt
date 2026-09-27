package com.example.blockade.ui.game

import com.example.blockade.game.ActionMode
import com.example.blockade.game.Barrier
import com.example.blockade.game.BarrierOrientation
import com.example.blockade.game.BarrierPlacementResult
import com.example.blockade.game.GridPosition
import com.example.blockade.game.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val recorded = mutableListOf<Pair<Player, Int>>()
    private lateinit var vm: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = GameViewModel(recordResult = { p, t -> recorded += p to t }, clock = { 0L })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val game get() = vm.uiState.value.game

    @Test
    fun moveLocksInputUntilAnimationCompletes() = runTest(dispatcher) {
        vm.moveToken(GridPosition(5, 3))
        assertTrue(game.isAnimating)
        vm.moveToken(GridPosition(5, 3)) // ignored while animating
        vm.setActionMode(ActionMode.BARRIER) // ignored while animating
        advanceUntilIdle()
        assertFalse(game.isAnimating)
        assertEquals(GridPosition(5, 3), game.player1Position)
        assertEquals(Player.PLAYER_TWO, game.currentPlayer)
        assertEquals(1, game.turns)
    }

    @Test
    fun barrierRequiresBarrierMode() {
        val barrier = Barrier(2, 2, BarrierOrientation.VERTICAL)
        assertEquals(BarrierPlacementResult.GAME_OVER, vm.placeBarrier(barrier))
        vm.setActionMode(ActionMode.BARRIER)
        assertEquals(BarrierPlacementResult.VALID, vm.placeBarrier(barrier))
        assertEquals(5, game.player1BarriersRemaining)
        assertEquals(ActionMode.MOVE, game.actionMode)
    }

    @Test
    fun rejectedBarrierShowsMessageAndKeepsTurn() {
        vm.setActionMode(ActionMode.BARRIER)
        vm.placeBarrier(Barrier(2, 2, BarrierOrientation.VERTICAL))
        vm.setActionMode(ActionMode.BARRIER)
        val result = vm.placeBarrier(Barrier(2, 2, BarrierOrientation.VERTICAL))
        assertEquals(BarrierPlacementResult.DUPLICATE, result)
        assertEquals(Player.PLAYER_TWO, game.currentPlayer)
        assertEquals(6, game.player2BarriersRemaining)
        assertNotNull(vm.uiState.value.rejected)
    }

    @Test
    fun undoRestoresFullSnapshot() = runTest(dispatcher) {
        vm.setActionMode(ActionMode.BARRIER)
        vm.placeBarrier(Barrier(1, 3, BarrierOrientation.HORIZONTAL))
        vm.moveToken(GridPosition(1, 3))
        advanceUntilIdle()
        assertEquals(2, game.turns)

        vm.undo()
        assertEquals(GridPosition(0, 3), game.player2Position)
        assertEquals(Player.PLAYER_TWO, game.currentPlayer)
        assertEquals(1, game.barriers.size)

        vm.undo()
        assertTrue(game.barriers.isEmpty())
        assertEquals(6, game.player1BarriersRemaining)
        assertEquals(Player.PLAYER_ONE, game.currentPlayer)
        assertFalse(vm.uiState.value.canUndo)
    }

    @Test
    fun winRecordsStatisticsOnce() = runTest(dispatcher) {
        // P1 walks straight up column 0; P2 shuffles sideways on the top row.
        val p1Path = listOf(6 to 2, 6 to 1, 6 to 0, 5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0, 0 to 0)
        val p2Path = listOf(0 to 4, 0 to 5, 0 to 4, 0 to 5, 0 to 4, 0 to 5, 0 to 4, 0 to 5)
        for (i in p1Path.indices) {
            vm.moveToken(GridPosition(p1Path[i].first, p1Path[i].second))
            advanceUntilIdle()
            if (game.winner != null) break
            vm.moveToken(GridPosition(p2Path[i].first, p2Path[i].second))
            advanceUntilIdle()
        }
        assertEquals(Player.PLAYER_ONE, game.winner)
        assertEquals(listOf(Player.PLAYER_ONE to 17), recorded)
        assertEquals(Player.PLAYER_ONE, vm.summary?.winner)
        vm.undo() // disabled after a win
        assertEquals(Player.PLAYER_ONE, game.winner)
    }
}
