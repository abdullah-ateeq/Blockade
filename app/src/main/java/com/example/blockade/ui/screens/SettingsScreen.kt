package com.example.blockade.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.blockade.data.BlockadeRepository
import com.example.blockade.data.BoardTheme
import com.example.blockade.data.Settings
import com.example.blockade.data.Statistics
import com.example.blockade.data.ThemeMode
import com.example.blockade.ui.components.ScreenScaffold
import com.example.blockade.ui.components.SecondaryButton
import com.example.blockade.ui.components.SectionLabel
import com.example.blockade.ui.components.SegmentedSelector

@Composable
fun SettingsScreen(
    settings: Settings,
    statistics: Statistics,
    repository: BlockadeRepository,
    onBack: () -> Unit
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(title = "Settings", onBack = onBack) {
        SectionLabel("Appearance")
        Spacer(Modifier.height(14.dp))
        ChoiceRow("Theme") {
            SegmentedSelector(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = repository::setThemeMode,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Board") {
            SegmentedSelector(
                options = BoardTheme.entries,
                selected = settings.boardTheme,
                label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = repository::setBoardTheme,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Gameplay")
        Spacer(Modifier.height(4.dp))
        ToggleRow("Show legal moves", settings.showLegalMoves, repository::setShowLegalMoves)
        Divider()
        ToggleRow("Show last action", settings.showLastAction, repository::setShowLastAction)

        Spacer(Modifier.height(28.dp))
        SectionLabel("Feedback")
        Spacer(Modifier.height(4.dp))
        ToggleRow("Sound effects", settings.soundEnabled, repository::setSound)
        Divider()
        ToggleRow("Haptic feedback", settings.hapticsEnabled, repository::setHaptics)

        Spacer(Modifier.height(28.dp))
        SectionLabel("Statistics")
        Spacer(Modifier.height(10.dp))
        Text(
            buildString {
                append("${statistics.gamesPlayed} games played · ")
                append("Player 1: ${statistics.player1Wins} · Player 2: ${statistics.player2Wins}")
                statistics.fewestTurnsToWin?.let { append("\nFewest turns to win: $it") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        SecondaryButton(
            "RESET STATISTICS",
            onClick = { confirmReset = true },
            enabled = statistics.gamesPlayed > 0,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Reset statistics?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Games played and win totals will be cleared. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; repository.resetStatistics() }) {
                    Text("RESET", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
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
private fun ChoiceRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
