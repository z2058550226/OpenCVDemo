package com.bybutter.opencvdemo

import android.app.Application
import com.suika.astree.AndroidStudioTree
import timber.log.Timber

/**
 * Created by suikajy on 2020.2.28
 */
lateinit var application: Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        application = this
        Timber.plant(AndroidStudioTree())
    }
}