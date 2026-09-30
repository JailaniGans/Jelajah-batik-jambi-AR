package com.jelajahbatikjambi.ui.ar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jelajahbatikjambi.ar.ArState
import com.jelajahbatikjambi.ui.theme.BatikGold
import com.jelajahbatikjambi.ui.theme.BatikGreen
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Short status pill shown over the camera preview (§29). A colored dot gives
 * an at-a-glance read of scan state (neutral while searching, gold while
 * locking on, green once confirmed, red on error) without relying on the
 * text alone, and pulses gently while the state is still "in progress"
 * (Initializing/Searching/MarkerDetected) so the UI reads as alive rather
 * than stuck. Text swaps with a fade instead of popping instantly.
 */
@Composable
fun ArStatusIndicator(state: ArState, modifier: Modifier = Modifier) {
    val text = when (state) {
        is ArState.Initializing -> "Menyiapkan kamera..."
        is ArState.Searching -> "Mencari motif..."
        is ArState.MarkerDetected -> "Mengunci motif..."
        is ArState.Tracking -> "Motif ditemukan"
        is ArState.MarkerLost -> "Arahkan kamera ke motif"
        is ArState.Error -> state.message
    }
    val dotColor = when (state) {
        is ArState.Tracking -> BatikGreen
        is ArState.MarkerDetected -> BatikGold
        is ArState.Error -> MaterialTheme.colorScheme.error
        else -> Color.White
    }
    val isPulsing = state is ArState.Initializing || state is ArState.Searching || state is ArState.MarkerDetected

    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusPulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
            )
            .padding(horizontal = Dimensions.spacingMd, vertical = Dimensions.spacingSm)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isPulsing) pulseAlpha else 1f)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.size(Dimensions.spacingSm))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "statusText"
        ) { label ->
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}
