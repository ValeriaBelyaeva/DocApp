package com.example.docapp

import android.app.Application
import com.example.docapp.core.ServiceLocator

class DocApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
