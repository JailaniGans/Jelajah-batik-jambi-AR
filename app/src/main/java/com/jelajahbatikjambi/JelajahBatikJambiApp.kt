package com.jelajahbatikjambi

import android.app.Application
import com.jelajahbatikjambi.ar.OpenCvSupport
import com.jelajahbatikjambi.render.FilamentRenderer
import com.jelajahbatikjambi.render.ModelLoader

class JelajahBatikJambiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCvSupport.initialize()
        FilamentRenderer.ensureNativeLibraryLoaded()
        ModelLoader.ensureNativeLibraryLoaded()
    }
}
