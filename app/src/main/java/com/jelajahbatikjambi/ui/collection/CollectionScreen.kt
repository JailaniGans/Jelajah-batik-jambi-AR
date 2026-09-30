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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onViewDetail: (Int) -> Unit,
    onStartQuiz: () -> Unit,
    onAddMotif: () -> Unit
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
                if (uiState.discovered.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Dimensions.spacingMd))
                    Button(
                        onClick = onStartQuiz.withClickSound(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.buttonHeight)
                    ) {
                        Text("Mulai Kuis")
                    }
                }
            }

            if (uiState.discovered.isEmpty()) {
                EmptyCollectionMessage(modifier = Modifier.fillMaxSize())
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
            text = "$discoveredCount dari $totalCount motif ditemukan",
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
private fun EmptyCollectionMessage(modifier: Modifier = Modifier) {
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
            text = "Jelajahi motif Batik Jambi menggunakan AR Scanner untuk mengisi koleksi ini.",
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
