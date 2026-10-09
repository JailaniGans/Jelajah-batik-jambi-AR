package com.jelajahbatikjambi.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.ui.common.AssetImage
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Kelola Koleksi is the single entry point for managing content: motif
 * editing happens behind Kelola Motif (list icon) and quiz-question
 * authoring (create/edit/delete) behind the Kelola Soal Kuis button below —
 * neither is reachable from the play screens (Detail / Kuis) anymore
 * (§ user request: "edit motif dan soal hanya bisa di akses lewat kelola
 * koleksi").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onViewDetail: (Int) -> Unit,
    onStartQuiz: () -> Unit,
    onAddMotif: () -> Unit,
    onManageMotifs: () -> Unit,
    onManageQuiz: () -> Unit
) {
    val viewModel: CollectionViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koleksi Batik") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onManageMotifs.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Kelola Motif")
                    }
                    IconButton(onClick = onAddMotif.withClickSound()) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah Motif")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(Dimensions.spacingLg)) {
                DiscoveryProgress(
                    discoveredCount = uiState.discovered.size,
                    totalCount = uiState.totalCount,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingMd))
                // Always available: the session is made of the user's own
                // questions, so it no longer depends on what's discovered.
                Button(
                    onClick = onStartQuiz.withClickSound(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.buttonHeight)
                ) {
                    Text("Mulai Kuis")
                }
                Spacer(modifier = Modifier.height(Dimensions.spacingSm))
                OutlinedButton(
                    onClick = onManageQuiz.withClickSound(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.buttonHeight)
                ) {
                    Text("Kelola Soal Kuis")
                }
            }

            if (uiState.discovered.isEmpty()) {
                if (uiState.totalCount == 0) {
                    // Fresh start: the app ships with no bundled motifs, so
                    // everything comes from the user's own Tambah Motif flow.
                    EmptyCollectionMessage(
                        onAddMotif = onAddMotif,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    NoDiscoveryYetMessage(modifier = Modifier.fillMaxSize())
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Dimensions.spacingLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.discovered, key = { it.id }) { batik ->
                        CollectionCard(batik = batik, onClick = { onViewDetail(batik.id) }.withClickSound())
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryProgress(discoveredCount: Int, totalCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = if (totalCount == 0) {
                "Belum ada motif di koleksi"
            } else {
                "$discoveredCount dari $totalCount motif ditemukan"
            },
            style = MaterialTheme.typography.titleMedium
        )
        if (totalCount > 0) {
            LinearProgressIndicator(
                progress = { discoveredCount.toFloat() / totalCount.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.spacingXs)
            )
        }
    }
}

@Composable
private fun EmptyCollectionMessage(onAddMotif: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Koleksi masih kosong",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Aplikasi mulai tanpa motif bawaan. Tambahkan motif batik pertamamu — foto motifnya didaftarkan untuk dipindai di AR, model 3D dibuat otomatis, dan soal kuis bisa langsung ditautkan ke motif tersebut.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimensions.spacingLg))
        Button(onClick = onAddMotif.withClickSound()) {
            Text("Tambahkan Motif Pertama")
        }
    }
}

/** Motif sudah ada, tetapi belum ada satu pun yang dipindai lewat AR. */
@Composable
private fun NoDiscoveryYetMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Belum ada motif yang ditemukan",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Arahkan kamera AR ke motif yang sudah kamu tambahkan untuk mengisi koleksi ini.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CollectionCard(batik: BatikData, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick)
    ) {
        Box {
            if (batik.imagePath != null) {
                AssetImage(
                    path = batik.imagePath,
                    contentDescription = batik.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(Dimensions.spacingSm)
            ) {
                Text(
                    text = batik.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}
