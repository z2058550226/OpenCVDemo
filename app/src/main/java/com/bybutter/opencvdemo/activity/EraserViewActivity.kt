package com.bybutter.opencvdemo.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.bybutter.opencvdemo.R
import com.bybutter.opencvdemo.util.saveAsTempFile
import kotlinx.android.synthetic.main.activity_eraser_view.*
import org.opencv.android.OpenCVLoader
import timber.log.Timber

class EraserViewActivity : AppCompatActivity() {
    companion object {
        private const val REQ_OPEN_IMAGE = 0x123
        private const val IMG_FILE_NAME = "canvasTmp.png"
    }

    private lateinit var mBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eraser_view)
        if (!OpenCVLoader.initDebug()) {
            throw IllegalStateException("OpenCv initialize error")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_canvas, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_img -> {
                val getPicIntent = Intent(Intent.ACTION_GET_CONTENT)
                getPicIntent.type = "image/*"
                val pickPicIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                val chooserIntent = Intent.createChooser(getPicIntent, "Select Image")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickPicIntent))
                startActivityForResult(
                    chooserIntent,
                    REQ_OPEN_IMAGE
                )
                true
            }
            R.id.action_reset_path -> {
                ev.reset()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OPEN_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Timber.i("onActivityResult")
            val imgUri = data.data ?: throw IllegalStateException("#1")
            val imgFd =
                contentResolver.openFileDescriptor(imgUri, "r") ?: throw IllegalStateException("#2")
            mBitmap = BitmapFactory.decodeFileDescriptor(imgFd.fileDescriptor)

            mBitmap.saveAsTempFile(IMG_FILE_NAME)

            ev.loadBitmap(IMG_FILE_NAME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ev.release()
    }
}