package com.jelajahbatikjambi.ui.managemotifs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.ui.common.AssetImage
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Lists every motif (discovered by AR or not) so each one can be edited or
 * deleted — the Collection grid only shows *discovered* motifs, so without
 * this screen a freshly-added-but-never-scanned motif would have no way to be
 * managed at all (§ user request: "belum ada tombol untuk kelola motif untuk
 * hapus dan edit"). Editing opens the existing edit screen; deletion is
 * confirmed here and mirrors the edit screen's rule that the last motif can't
 * be removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMotifsScreen(
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onAddMotif: () -> Unit
) {
    val viewModel: ManageMotifsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var motifToDelete by remember { mutableStateOf<BatikData?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Motif") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.rows.isEmpty() -> EmptyState(
                    onAddMotif = onAddMotif,
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimensions.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
                ) {
                    items(uiState.rows, key = { it.batik.id }) { row ->
                        MotifRow(
                            row = row,
                            onEdit = { onEdit(row.batik.id) }.withClickSound(),
                            onDelete = { motifToDelete = row.batik }
                        )
                    }
                }
            }
        }

        motifToDelete?.let { batik ->
            AlertDialog(
                onDismissRequest = { motifToDelete = null },
                title = { Text("Hapus motif ini?") },
                text = {
                    Text(
                        "\"${batik.name}\" beserta foto, model 3D, soal kuis yang menautkannya, " +
                            "dan catatan penemuannya akan dihapus permanen. Tindakan ini tidak bisa dibatalkan."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMotif(batik.id)
                            motifToDelete = null
                        }
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { motifToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyState(onAddMotif: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Belum ada motif",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tambahkan motif batik pertamamu lewat tombol + di Koleksi, lalu kelola (edit / hapus) motifnya di sini.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimensions.spacingLg))
        TextButton(onClick = onAddMotif.withClickSound()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(Dimensions.spacingSm))
            Text("Tambah Motif")
        }
    }
}

@Composable
private fun MotifRow(row: ManageMotifRow, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(Dimensions.cornerRadiusSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                row.batik.imagePath?.let { path ->
                    AssetImage(
                        path = path,
                        contentDescription = row.batik.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(Dimensions.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.batik.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = row.batik.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (row.discovered) "Ditemukan via AR" else "Belum ditemukan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Dimensions.spacingSm))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit motif",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete, enabled = row.canDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Hapus motif",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}