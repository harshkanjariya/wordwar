package com.harshkanjariya.wordwar

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.pluto.Pluto
import com.pluto.plugins.network.PlutoNetworkPlugin

class WordWarApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        // Only initialize Pluto in debug builds
        if (BuildConfig.DEBUG) {
            Pluto.Installer(this)
                .addPlugin(PlutoNetworkPlugin())
                .install()
        }
    }
}
