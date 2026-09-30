package com.jelajahbatikjambi.ui.common

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * App-wide UI sound feedback (§ user request: "tambah suara saat tombol di
 * click atau terjadi suatu event"). Synthesizes short tones on the fly via
 * [ToneGenerator] rather than bundling audio files — consistent with how
 * [com.jelajahbatikjambi.render.TexturedCubeGlbGenerator] builds assets
 * on-device instead of shipping pre-baked ones. [ToneGenerator] needs no
 * [android.content.Context], so this can be called from anywhere — a
 * Composable's onClick, or a plain ViewModel — with no plumbing. A single
 * instance is cheap to keep for the whole process lifetime, so there's no
 * explicit release step.
 */
object SoundEffects {

    private const val VOLUME_PERCENT = 60

    // Construction can fail on devices with no usable audio output (rare,
    // but not worth crashing the app over a UI nicety) — degrades to silent
    // no-ops rather than propagating the failure.
    private val toneGenerator: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME_PERCENT) }.getOrNull()
    }

    /** A short, neutral tick for ordinary button presses and navigation. */
    fun playClick() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    /** A brighter tone for a positive event: a motif discovered, a correct quiz answer. */
    fun playSuccess() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    /** A duller tone for a negative event: a wrong quiz answer. */
    fun playError() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 150)
    }
}

/** Wraps a click lambda so every invocation also plays [SoundEffects.playClick]. */
fun (() -> Unit).withClickSound(): () -> Unit = {
    SoundEffects.playClick()
    this()
}
