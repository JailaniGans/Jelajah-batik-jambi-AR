package com.jelajahbatikjambi

import android.app.Application
import com.jelajahbatikjambi.ar.OpenCvSupport
import com.jelajahbatikjambi.render.NativeSupport

/**
 * Loads the optional native stacks at process start.
 *
 * Both calls are non-throwing by construction ([OpenCvSupport.initialize]
 * returns a Boolean, [NativeSupport.initialize] catches [Throwable]) — an
 * escaping failure here would kill the process before [MainActivity] existed,
 * turning "AR is unavailable on this device" into "the app won't start".
 */
class JelajahBatikJambiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCvSupport.initialize()
        NativeSupport.initialize()
    }
}