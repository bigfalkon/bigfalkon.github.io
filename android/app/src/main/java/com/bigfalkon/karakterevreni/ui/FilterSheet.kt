package com.bigfalkon.karakterevreni.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    state: UiState,
    onMode: (GalleryMode) -> Unit,
    onSort: (SortMode) -> Unit,
    onRace: (String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtreler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = onClear) { Text("Sıfırla") }
            }

            SheetLabel("Görünüm")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GalleryMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { onMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            SheetLabel("Sıralama")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortMode.entries.forEach { sort ->
                    FilterChip(
                        selected = state.sort == sort,
                        onClick = { onSort(sort) },
                        label = { Text(sort.label) }
                    )
                }
            }

            if (state.races.isNotEmpty()) {
                SheetLabel("Irk")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.race == null,
                        onClick = { onRace(null) },
                        label = { Text("Tümü") }
                    )
                    state.races.forEach { race ->
                        FilterChip(
                            selected = state.race == race,
                            onClick = { onRace(race) },
                            label = { Text(race) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}
