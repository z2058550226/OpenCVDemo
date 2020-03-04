package com.bybutter.opencvdemo.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by suikajy on 2020.2.28
 */
class PermissionActivity : AppCompatActivity() {
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var havePermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
                havePermission = false
            }
        }
        if (havePermission) {
            startMainActivity()
        }
        Toast.makeText(this,"s",Toast.LENGTH_SHORT).show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startMainActivity()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}