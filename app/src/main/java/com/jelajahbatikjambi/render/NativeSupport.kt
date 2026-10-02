package com.jelajahbatikjambi.render

import android.util.Log

private const val TAG = "NATIVE"

/**
 * Loads the Filament native libraries (`libfilament-jni.so`,
 * `libgltfio-jni.so` and the `libc++_shared.so` they link against) once, and
 * — crucially — never lets a load failure escape into the caller.
 *
 * [JelajahBatikJambiApp] calls [initialize] from `Application.onCreate()`,
 * where an escaping [UnsatisfiedLinkError] tears the process down before any
 * Activity exists. That turned "this particular device can't run the 3D
 * overlay" into "the entire app won't launch", on every screen, for reasons
 * having nothing to do with AR. So the 3D stack is treated as strictly
 * optional and gated on [isAvailable]: on a device where these libraries
 * refuse to load, marker detection, the info panel, the quiz and the rest of
 * the app keep working without the overlay (§33).
 *
 * Note the [runCatching] rather than a `try/catch (e: Exception)`:
 * `UnsatisfiedLinkError` is an [Error], not an [Exception], so catching
 * [Exception] would miss the exact failure this exists to contain.
 *
 * The result is also recorded rather than merely swallowed, because
 * [failureReason] is what [com.jelajahbatikjambi.ui.ar.ArScreen] surfaces to
 * the user — a camera preview that silently never grows an object on it is
 * indistinguishable from a broken build.
 */
object NativeSupport {

    private fun defaultLoaders() {
        FilamentRenderer.ensureNativeLibraryLoaded()
        ModelLoader.ensureNativeLibraryLoaded()
    }

    /**
     * Seam for unit tests: the real `.so` load can't run off-device, and
     * [NativeSupportTest] needs to drive both outcomes deterministically.
     */
    internal var loaders: () -> Unit = ::defaultLoaders

    @Volatile
    var isAvailable: Boolean = false
        private set

    @Volatile
    var failureReason: String? = null
        private set

    /**
     * Attempts the load once and remembers the outcome. Safe to call from any
     * thread and any number of times: the first result is memoised, so a
     * device with unusable libraries doesn't retry the failing load on every
     * process start (and doesn't re-log it every time).
     *
     * @return true if the 3D overlay can be used on this device.
     */
    fun initialize(): Boolean {
        if (isAvailable) return true
        if (failureReason != null) return false

        val failure = runCatching { loaders() }.exceptionOrNull()

        if (failure != null) {
            failureReason = "${failure::class.java.simpleName}: ${failure.message}"
            Log.e(TAG, "Filament native libraries unavailable, 3D overlay disabled", failure)
            return false
        }

        isAvailable = true
        Log.i(TAG, "Filament native libraries loaded, 3D overlay enabled")
        return true
    }

    /** Restores the real loaders and clears the memoised result between tests. */
    internal fun resetForTesting() {
        loaders = ::defaultLoaders
        isAvailable = false
        failureReason = null
    }
}