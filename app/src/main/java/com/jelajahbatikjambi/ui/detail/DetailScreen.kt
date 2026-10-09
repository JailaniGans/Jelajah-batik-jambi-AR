package com.jelajahbatikjambi.ui.detail

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.ui.common.AssetImage
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Full motif detail (§14). Resolves [batikId] through [MotifRepository], so
 * built-in motifs show the user's saved edits over `batik.json` and custom
 * motifs resolve through the same merged list — ids never overlap (see
 * [com.jelajahbatikjambi.data.repository.CustomMotifRepository]), so this is
 * unambiguous.
 *
 * Reads the repository's *Flow* rather than a one-shot lookup: coming back
 * from anywhere (e.g. an edit made while this screen sits in the back
 * stack) re-emits with the fresh values, so the detail shown always matches
 * what's stored.
 *
 * View-only: editing a motif happens exclusively behind Koleksi → Kelola
 * Motif (§ user request — "edit motif dan soal hanya bisa di akses lewat
 * kelola koleksi"), so there is no pencil in the top bar here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    batikId: Int,
    onBack: () -> Unit,
    onJelajahiDenganAr: () -> Unit
) {
    val context = LocalContext.current
    val motifRepository = remember {
        MotifRepository.getInstance(context.applicationContext as Application)
    }

    // Only flips off on the first emission — a motif that simply doesn't
    // exist emits null once and the "not found" branch below takes over.
    var isLoading by remember(batikId) { mutableStateOf(true) }
    val batik by produceState<BatikData?>(initialValue = null, batikId) {
        var firstEmission = true
        motifRepository.motifById(batikId).collect { motif ->
            value = motif
            if (firstEmission) {
                firstEmission = false
                isLoading = false
            }
        }
    }
    val currentBatik = batik

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentBatik?.name ?: "Detail Motif") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (currentBatik == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Motif tidak ditemukan")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (currentBatik.imagePath != null) {
                AssetImage(
                    path = currentBatik.imagePath,
                    contentDescription = currentBatik.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }

            Column(
                modifier = Modifier.padding(Dimensions.spacingLg),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
            ) {
                Text(text = currentBatik.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = currentBatik.category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                DetailSection(title = "Deskripsi", body = currentBatik.shortDescription)
                DetailSection(title = "Makna", body = currentBatik.meaning)
                DetailSection(title = "Sejarah", body = currentBatik.history)

                Button(
                    onClick = onJelajahiDenganAr.withClickSound(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.buttonHeight)
                ) {
                    Text("Jelajahi dengan AR")
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}
