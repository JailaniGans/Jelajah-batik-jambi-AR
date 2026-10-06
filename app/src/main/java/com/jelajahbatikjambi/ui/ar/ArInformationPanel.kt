package com.jelajahbatikjambi.ui.ar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jelajahbatikjambi.data.model.BatikData
import com.jelajahbatikjambi.ui.common.AssetImage
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/**
 * Summary card shown once a marker is confirmed (§9-13). Only a short
 * summary lives here — [batik.shortDescription] is capped at 3 lines; the
 * full [BatikData.meaning] / [BatikData.history] belong on the Detail screen.
 * Must never appear before the *first* confirmation: callers pass
 * `batik = null` until something has been confirmed, and pass null again
 * only if the panel was dismissed — afterwards the matched motif stays
 * sticky (§ user request), so the card no longer vanishes with each tracking
 * dropout even though the underlying [com.jelajahbatikjambi.ar.ArState]
 * does.
 *
 * Leads with a thumbnail of the matched motif photo — a quick visual
 * confirmation of *what* was recognized, not just its name — and a top-right
 * close affordance rather than a full-width "Tutup" button, matching the
 * compact bottom-sheet style most camera-driven apps use for this kind of
 * transient result card.
 *
 * Also offers a direct "Mulai Kuis" shortcut right after a successful scan
 * (§ user request), alongside the existing entry point from the Collection
 * screen. Passes the scanned [BatikData] through so the quiz that starts is
 * scoped to just this motif (§ user request: "pertanyaan sesuai dengan motif
 * apa yang saya scan") rather than the general discovered-motifs mix
 * Collection's entry point starts.
 */
@Composable
fun ArInformationPanel(
    batik: BatikData?,
    onLihatDetail: (BatikData) -> Unit,
    onMulaiKuis: (BatikData) -> Unit,
    onTutup: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = batik != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (batik == null) return@AnimatedVisibility

        Surface(
            shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
            shadowElevation = Dimensions.spacingSm,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Dimensions.spacingMd)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (batik.imagePath != null) {
                        AssetImage(
                            path = batik.imagePath,
                            contentDescription = batik.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(Dimensions.cornerRadiusSmall))
                        )
                        Spacer(modifier = Modifier.size(Dimensions.spacingMd))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = batik.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = batik.category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onTutup.withClickSound()) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacingXs))

                Text(
                    text = batik.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Dimensions.spacingSm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm)
                ) {
                    OutlinedButton(
                        onClick = { onMulaiKuis(batik) }.withClickSound(),
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimensions.buttonHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(Dimensions.spacingSm))
                        Text("Mulai Kuis")
                    }
                    Button(
                        onClick = { onLihatDetail(batik) }.withClickSound(),
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimensions.buttonHeight)
                    ) {
                        Text("Lihat Detail")
                        Spacer(modifier = Modifier.size(Dimensions.spacingSm))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
