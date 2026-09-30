package com.jelajahbatikjambi.ui.detail

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.data.repository.BatikRepository
import com.jelajahbatikjambi.data.repository.CustomMotifRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.ui.common.AssetImage
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full motif detail (§14). Looks up [batikId] against the built-in bundled
 * data first (a synchronous in-memory read), falling back to Room-backed
 * custom motifs (§ user request: "tambahkan motif dengan upload .jpg") only
 * when that misses — custom motif ids never overlap the built-in range (see
 * [CustomMotifRepository]), so this is unambiguous. The fallback requires a
 * DB read, hence tracked as explicit loading/result state via [LaunchedEffect]
 * rather than a plain [remember].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    batikId: Int,
    onBack: () -> Unit,
    onJelajahiDenganAr: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember(batikId) { mutableStateOf(true) }
    var batik by remember(batikId) { mutableStateOf<BatikData?>(null) }

    LaunchedEffect(batikId) {
        isLoading = true
        batik = withContext(Dispatchers.IO) {
            BatikRepository(context.assets).getById(batikId)
                ?: CustomMotifRepository(AppDatabase.getInstance(context).customMotifDao())
                    .getAllOnceAsBatikData()
                    .firstOrNull { it.id == batikId }
        }
        isLoading = false
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
