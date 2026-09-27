package com.example.blockade.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blockade.game.Player
import com.example.blockade.ui.components.ScreenScaffold
import com.example.blockade.ui.components.SectionLabel
import com.example.blockade.ui.components.TokenIcon

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "About", onBack = onBack) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TokenIcon(Player.PLAYER_ONE, Modifier.size(34.dp))
            TokenIcon(Player.PLAYER_TWO, Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Blockade", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        Text("Version 1.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text(
            "A polished local two-player strategy race where players advance toward the opposite " +
                "side while placing barriers to redirect their rival.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(32.dp))
        SectionLabel("Features")
        Spacer(Modifier.height(8.dp))
        Column {
            listOf(
                "Local 2 Player",
                "7×7 Tactical Board",
                "Barrier Placement",
                "Pathfinding Strategy",
                "Offline Play"
            ).forEachIndexed { i, feature ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("◆", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(14.dp))
                    Text(feature, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}
