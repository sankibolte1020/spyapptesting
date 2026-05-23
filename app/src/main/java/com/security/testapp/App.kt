package com.security.testapp

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DatabaseHelper.init(this)
    }
}
