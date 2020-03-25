package com.bybutter.opencvdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bybutter.opencvdemo.activity.TutorialActivity
import com.bybutter.opencvdemo.grabcut.CanvasTestActivity
import com.bybutter.opencvdemo.grabcut.GrabCutActivity
import com.bybutter.opencvdemo.grabcut.GrabCutViewActivity
import kotlin.reflect.KClass

/**
 * Created by suikajy on 2020.3.1
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun navigate(kClz: KClass<out Activity>) = startActivity(Intent(this, kClz.java))

    fun ch1(view: View) = Unit
    fun tutorial(view: View) = navigate(TutorialActivity::class)
    fun myGrabCut(view: View) = navigate(GrabCutActivity::class)
    fun grabCutView(view: View) = navigate(GrabCutViewActivity::class)
    fun canvasTest(view: View) = navigate(CanvasTestActivity::class)
}