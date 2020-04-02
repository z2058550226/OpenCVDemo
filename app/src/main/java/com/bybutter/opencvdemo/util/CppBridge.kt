package com.bybutter.opencvdemo.util

import org.opencv.android.OpenCVLoader
import timber.log.Timber

object CppBridge {
    init {
        if (!OpenCVLoader.initDebug()) {
            Timber.d("Unable to load OpenCV")
        } else {
            System.loadLibrary("native-lib")
        }
    }

    external fun test(matAddr: Long)

    external fun eraserCut(srcAddr:Long, bgdMaskAddr:Long, dstAddr:Long)

    external fun stringFromJNI(): String
}