package com.example.blockade.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.blockade.game.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BoardTheme { CLASSIC, MIDNIGHT }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showLegalMoves: Boolean = true,
    val showLastAction: Boolean = true
)

data class Statistics(
    val gamesPlayed: Int = 0,
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val fewestTurnsToWin: Int? = null
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blockade")

class BlockadeRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val BOARD = stringPreferencesKey("board_theme")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val LEGAL = booleanPreferencesKey("show_legal_moves")
        val LAST = booleanPreferencesKey("show_last_action")
        val GAMES = intPreferencesKey("games_played")
        val P1_WINS = intPreferencesKey("player1_wins")
        val P2_WINS = intPreferencesKey("player2_wins")
        val FEWEST = intPreferencesKey("fewest_turns")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            boardTheme = p[Keys.BOARD]?.let { runCatching { BoardTheme.valueOf(it) }.getOrNull() }
                ?: BoardTheme.CLASSIC,
            soundEnabled = p[Keys.SOUND] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            showLegalMoves = p[Keys.LEGAL] ?: true,
            showLastAction = p[Keys.LAST] ?: true
        )
    }

    val statistics: Flow<Statistics> = store.data.map { p ->
        Statistics(
            gamesPlayed = p[Keys.GAMES] ?: 0,
            player1Wins = p[Keys.P1_WINS] ?: 0,
            player2Wins = p[Keys.P2_WINS] ?: 0,
            fewestTurnsToWin = p[Keys.FEWEST]
        )
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { store.edit { block(it) } }
    }

    fun setThemeMode(mode: ThemeMode) = write { it[Keys.THEME] = mode.name }
    fun setBoardTheme(theme: BoardTheme) = write { it[Keys.BOARD] = theme.name }
    fun setSound(enabled: Boolean) = write { it[Keys.SOUND] = enabled }
    fun setHaptics(enabled: Boolean) = write { it[Keys.HAPTICS] = enabled }
    fun setShowLegalMoves(enabled: Boolean) = write { it[Keys.LEGAL] = enabled }
    fun setShowLastAction(enabled: Boolean) = write { it[Keys.LAST] = enabled }

    fun recordResult(winner: Player, turns: Int) = write {
        it[Keys.GAMES] = (it[Keys.GAMES] ?: 0) + 1
        val winsKey = if (winner == Player.PLAYER_ONE) Keys.P1_WINS else Keys.P2_WINS
        it[winsKey] = (it[winsKey] ?: 0) + 1
        val fewest = it[Keys.FEWEST]
        if (fewest == null || turns < fewest) it[Keys.FEWEST] = turns
    }

    fun resetStatistics() = write {
        it.remove(Keys.GAMES)
        it.remove(Keys.P1_WINS)
        it.remove(Keys.P2_WINS)
        it.remove(Keys.FEWEST)
    }
}
