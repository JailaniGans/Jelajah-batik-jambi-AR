package com.jelajahbatikjambi.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jelajahbatikjambi.data.model.BatikData

/**
 * Picks which motif a quiz question is tied to — every question must name
 * one (§ user request: "fitur buat soal ada fitur untuk menentukan soal yang
 * di buat itu ke motif yang telah di pilih user"). The options are the same
 * merged built-in + custom list the rest of the app renders, and there is
 * deliberately no "no motif" option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotifPicker(
    motifs: List<BatikData>,
    selectedBatikId: Int?,
    onBatikIdSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = motifs.firstOrNull { it.id == selectedBatikId }?.name

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Motif terkait") },
            placeholder = { Text("Pilih motif") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            motifs.forEach { motif ->
                DropdownMenuItem(
                    text = { Text("${motif.name} · ${motif.category}") },
                    onClick = {
                        onBatikIdSelected(motif.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
