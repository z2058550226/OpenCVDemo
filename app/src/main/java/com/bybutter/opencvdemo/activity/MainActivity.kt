package com.bybutter.opencvdemo.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bybutter.opencvdemo.R
import com.bybutter.opencvdemo.grabcut.GrabCutActivity
import com.bybutter.opencvdemo.grabcut.GrabCutDemoActivity

/**
 * Created by suikajy on 2020.3.1
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun ch1(view: View) {

    }

    fun tutorial(view: View) {
        startActivity(Intent(this, TutorialActivity::class.java))
    }

    fun grabCut(view: View) {
        startActivity(Intent(this, GrabCutDemoActivity::class.java))
    }

    fun myGrabCut(view: View) {
        startActivity(Intent(this, GrabCutActivity::class.java))
    }
}