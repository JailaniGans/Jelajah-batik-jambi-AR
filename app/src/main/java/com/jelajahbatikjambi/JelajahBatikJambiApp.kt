package com.jelajahbatikjambi

import android.app.Application
import android.util.Log
import com.jelajahbatikjambi.ar.OpenCvSupport
import com.jelajahbatikjambi.data.repository.MotifRepository
import com.jelajahbatikjambi.render.NativeSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "JBBApp"
private const val PREFS_NAME = "jelajah_prefs"
private const val KEY_GLB_REPAIRED = "custom_glb_repaired_v1"

/**
 * Process bootstrap: loads the optional native stacks and runs the one-time
 * GLB repair for the black-cube bug (§ user request — "objek 3d nya berwarna
 * hitam, perbaiki glb di aplikasi yang sudah terpasang").
 *
 * The repair regenerates every stored custom-motif GLB that still carries the
 * old mipmapped texture sampler (`minFilter 9987` — renders black through
 * gltfio's embedded-texture path) from the motif's original photo, so an
 * app that was installed *before* the generator fix heals its existing black
 * cubes on first launch after the update. It runs once per install (flag in
 * [PREFS_NAME]) on a background scope, is idempotent, and swallows failures —
 * exactly like the native stack loads: an escaping error here would kill the
 * process before [MainActivity] existed.
 */
class JelajahBatikJambiApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        OpenCvSupport.initialize()
        NativeSupport.initialize()
        repairLegacyCustomGlbsOnce()
    }

    private fun repairLegacyCustomGlbsOnce() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_GLB_REPAIRED, false)) return
        applicationScope.launch {
            try {
                val repaired = MotifRepository.getInstance(this@JelajahBatikJambiApp)
                    .repairLegacyCustomGlbs()
                Log.i(TAG, "GLB repair selesai: $repaired berkas digenerate ulang")
                prefs.edit().putBoolean(KEY_GLB_REPAIRED, true).apply()
            } catch (t: Throwable) {
                // Leave the flag unset so the next launch retries; repairs are idempotent.
                Log.w(TAG, "GLB repair gagal, akan dicoba lagi", t)
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}