package com.jelajahbatikjambi.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions
import com.jelajahbatikjambi.ui.theme.JelajahBatikJambiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tentang") },
                navigationIcon = {
                    IconButton(onClick = onBack.withClickSound()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimensions.spacingLg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd)
        ) {
            Text(
                text = "Tentang Jelajah Batik Jambi",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Aplikasi ini dibuat sebagai media pengenalan dan edukasi budaya Jambi " +
                    "menggunakan teknologi Augmented Reality.",
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            AboutInfoRow(label = "Versi Aplikasi", value = "1.3.0")
            AboutInfoRow(label = "Pengembang", value = "Jelajah Batik Jambi Team")
            AboutInfoRow(label = "Lisensi", value = "Hak Cipta Dilindungi")
            AboutInfoRow(
                label = "Informasi Aset",
                value = "Model 3D dan data motif digunakan untuk tujuan edukasi."
            )
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    JelajahBatikJambiTheme {
        AboutScreen(onBack = {})
    }
}
