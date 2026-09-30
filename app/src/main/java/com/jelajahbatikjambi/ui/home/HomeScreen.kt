package com.jelajahbatikjambi.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions
import com.jelajahbatikjambi.ui.theme.JelajahBatikJambiTheme

@Composable
fun HomeScreen(
    onMulaiJelajah: () -> Unit,
    onKoleksiBatik: () -> Unit,
    onTentang: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BatikCornerOrnament(modifier = Modifier.align(Alignment.TopStart))
        BatikCornerOrnament(modifier = Modifier.align(Alignment.BottomEnd))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = Dimensions.spacingLg)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BatikLogoMark()

            Spacer(modifier = Modifier.size(Dimensions.spacingLg))

            Text(
                text = "JELAJAH BATIK JAMBI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(Dimensions.spacingSm))

            Text(
                text = "Kenali budaya Jambi melalui pengalaman augmented reality.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(Dimensions.spacingXl))

            Button(
                onClick = onMulaiJelajah.withClickSound(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight)
            ) {
                Text(text = "Mulai Jelajah", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.size(Dimensions.spacingMd))

            OutlinedButton(
                onClick = onKoleksiBatik.withClickSound(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight)
            ) {
                Text(text = "Koleksi Batik", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.size(Dimensions.spacingSm))

            TextButton(onClick = onTentang.withClickSound()) {
                Text(text = "Tentang", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BatikLogoMark() {
    val color = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.size(Dimensions.iconSizeLarge)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val outerRadius = w / 2f
        val innerRadius = outerRadius * 0.55f

        val outerDiamond = Path().apply {
            moveTo(center.x, center.y - outerRadius)
            lineTo(center.x + outerRadius, center.y)
            lineTo(center.x, center.y + outerRadius)
            lineTo(center.x - outerRadius, center.y)
            close()
        }
        val innerDiamond = Path().apply {
            moveTo(center.x, center.y - innerRadius)
            lineTo(center.x + innerRadius, center.y)
            lineTo(center.x, center.y + innerRadius)
            lineTo(center.x - innerRadius, center.y)
            close()
        }

        drawPath(outerDiamond, color = color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        drawPath(innerDiamond, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun BatikCornerOrnament(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    Canvas(modifier = modifier.size(120.dp)) {
        val step = size.minDimension / 4f
        for (i in 1..3) {
            drawCircle(
                color = color,
                radius = step * i,
                center = Offset.Zero,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    JelajahBatikJambiTheme {
        HomeScreen(onMulaiJelajah = {}, onKoleksiBatik = {}, onTentang = {})
    }
}
