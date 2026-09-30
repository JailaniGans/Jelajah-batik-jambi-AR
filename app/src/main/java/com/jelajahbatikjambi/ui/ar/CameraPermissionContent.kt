package com.jelajahbatikjambi.ui.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions
import com.jelajahbatikjambi.ui.theme.JelajahBatikJambiTheme

/**
 * Shown when camera permission is not granted, per the permission flow in spec.
 * [isPermanentlyDenied] switches the call to action from "Berikan Izin" (re-request)
 * to "Buka Pengaturan" (app settings), matching the two denial states Android exposes.
 */
@Composable
fun CameraPermissionContent(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimensions.spacingSm))

        Text(
            text = "Aplikasi membutuhkan kamera untuk mengenali motif Batik Jambi.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimensions.spacingLg))

        if (isPermanentlyDenied) {
            Button(onClick = onOpenSettings.withClickSound()) {
                Text("Buka Pengaturan")
            }
        } else {
            Button(onClick = onRequestPermission.withClickSound()) {
                Text("Berikan Izin")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPermissionContentPreview() {
    JelajahBatikJambiTheme {
        CameraPermissionContent(
            isPermanentlyDenied = false,
            onRequestPermission = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPermissionContentPermanentlyDeniedPreview() {
    JelajahBatikJambiTheme {
        CameraPermissionContent(
            isPermanentlyDenied = true,
            onRequestPermission = {},
            onOpenSettings = {}
        )
    }
}
