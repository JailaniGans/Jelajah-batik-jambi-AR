package com.jelajahbatikjambi.render

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression coverage for the crash that made the whole app unlaunchable on
 * devices whose Filament libraries wouldn't load: `Filament.init()` and
 * `Gltfio.init()` were called straight from `Application.onCreate()` with
 * nothing between them and the crash. `UnsatisfiedLinkError` is an [Error], so
 * the failure surfaced before any Activity existed — no screen, no fallback,
 * just a dead process.
 *
 * The contract asserted here is that the failure is *contained and recorded*,
 * never propagated, and never retried.
 */
class NativeSupportTest {

    @Before
    fun setUp() = NativeSupport.resetForTesting()

    @After
    fun tearDown() = NativeSupport.resetForTesting()

    @Test
    fun `a successful load marks the overlay available`() {
        NativeSupport.loaders = { }

        assertTrue(NativeSupport.initialize())
        assertTrue(NativeSupport.isAvailable)
        assertNull(NativeSupport.failureReason)
    }

    @Test
    fun `an UnsatisfiedLinkError is contained rather than escaping the guard`() {
        NativeSupport.loaders = { throw UnsatisfiedLinkError("libfilament-jni.so") }

        // The assertion is that this line doesn't throw at all — the guard is
        // what keeps a device-level library problem from becoming a dead process.
        val available = NativeSupport.initialize()

        assertFalse(available)
        assertFalse(NativeSupport.isAvailable)
    }

    @Test
    fun `an Error rather than an Exception is the case that matters`() {
        // Documents why runCatching is used here: a plain catch (e: Exception)
        // would let this straight through, and UnsatisfiedLinkError — the
        // failure mode this object exists for — is an Error, not an Exception.
        NativeSupport.loaders = { throw StackOverflowError("deep JNI recursion") }

        assertFalse(NativeSupport.initialize())
        assertTrue(NativeSupport.failureReason!!.contains("StackOverflowError"))
    }

    @Test
    fun `failureReason names the error type so the AR screen can show it`() {
        NativeSupport.loaders = { throw UnsatisfiedLinkError("libgltfio-jni.so") }

        NativeSupport.initialize()

        val reason = NativeSupport.failureReason
        assertTrue(reason!!.contains("UnsatisfiedLinkError"))
        assertTrue(reason.contains("libgltfio-jni.so"))
    }

    @Test
    fun `a failed load is not retried on subsequent calls`() {
        var attempts = 0
        NativeSupport.loaders = {
            attempts++
            throw UnsatisfiedLinkError("libfilament-jni.so")
        }

        assertFalse(NativeSupport.initialize())
        assertFalse(NativeSupport.initialize())
        assertFalse(NativeSupport.initialize())

        // Re-attempting a load already known to fail would just re-throw and
        // re-log on every launch for no chance of a different outcome.
        assertEquals(1, attempts)
    }

    @Test
    fun `a successful load is not repeated on subsequent calls`() {
        var attempts = 0
        NativeSupport.loaders = { attempts++ }

        assertTrue(NativeSupport.initialize())
        assertTrue(NativeSupport.initialize())

        assertEquals(1, attempts)
    }

    @Test
    fun `a failure after a success cannot be reported as unavailable`() {
        var attempts = 0
        NativeSupport.loaders = {
            attempts++
            if (attempts > 1) throw UnsatisfiedLinkError("late failure")
        }

        assertTrue(NativeSupport.initialize())
        assertTrue(NativeSupport.initialize())

        assertTrue(NativeSupport.isAvailable)
        assertNull(NativeSupport.failureReason)
    }

    @Test
    fun `a partial load where gltfio fails leaves the overlay unavailable`() {
        NativeSupport.loaders = { throw UnsatisfiedLinkError("libgltfio-jni.so missing") }

        assertFalse(NativeSupport.initialize())

        // Both libraries are required: a half-loaded stack can't build a scene,
        // so it must not be advertised as available.
        assertFalse(NativeSupport.isAvailable)
    }
}