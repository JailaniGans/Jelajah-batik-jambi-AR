package com.jelajahbatikjambi.ui.ar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Flash/torch toggle (§7, §31). Callers must only show this when the device
 * actually reports a flash unit — see
 * [com.jelajahbatikjambi.camera.CameraController.hasFlashUnit] — so devices
 * without one never see a button that does nothing.
 */
@Composable
fun ArControls(isFlashOn: Boolean, onToggleFlash: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onToggleFlash,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.35f),
            contentColor = Color.White
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
            contentDescription = if (isFlashOn) "Matikan flash" else "Nyalakan flash"
        )
    }
}
