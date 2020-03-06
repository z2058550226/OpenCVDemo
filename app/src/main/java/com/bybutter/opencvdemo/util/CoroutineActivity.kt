package com.bybutter.opencvdemo.util

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Created by suikajy on 2020.3.6
 */
abstract class CoroutineActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job + CoroutineExceptionHandler { coroutineContext, throwable ->
            Timber.e(throwable, "Error in coroutine")
        }

    protected var job = Job()

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}