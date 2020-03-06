package com.bybutter.opencvdemo.util

import android.graphics.Bitmap
import com.bybutter.opencvdemo.application
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Created by suikajy on 2020.3.6
 */

val tempImgDir: File
    get() {
        val dir = application.getExternalFilesDir("tmpImg")!!
        if (!dir.exists()) dir.mkdir()
        Timber.i(dir.absolutePath)
        return dir
    }

val outputImgDir: File
    get() {
        val dir = application.getExternalFilesDir("outputImg")!!
        if (!dir.exists()) dir.mkdir()
        Timber.i(dir.absolutePath)
        return dir
    }

fun Bitmap.saveAsTempFile(fileName: String) {
    FileOutputStream(File(tempImgDir, fileName)).use {
        this.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.flush()
    }
}

fun getTempImageFile(fileName: String) = File(tempImgDir, fileName)

fun getTempImagePath(fileName: String) = File(tempImgDir, fileName).absolutePath

fun getOutputImageFilePath(fileName: String) = File(outputImgDir,fileName).absolutePath